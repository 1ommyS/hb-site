package com.example.hbsite.service

import com.example.hbsite.config.QuizProperties
import com.example.hbsite.domain.Answer
import com.example.hbsite.domain.OptionEntity
import com.example.hbsite.domain.Question
import com.example.hbsite.domain.Room
import com.example.hbsite.domain.RoomStatus
import com.example.hbsite.repo.AnswerRepository
import com.example.hbsite.repo.OptionRepository
import com.example.hbsite.repo.PlayerRepository
import com.example.hbsite.repo.QuestionRepository
import com.example.hbsite.repo.RoomRepository
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.OptionDto
import com.example.hbsite.ws.QuestionFinishedPayload
import com.example.hbsite.ws.QuestionStartedPayload
import com.example.hbsite.ws.QuizStartedPayload
import com.example.hbsite.ws.RoomEventBus
import com.example.hbsite.ws.WsEnvelope
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class AnswerOutcome(
    val accepted: Boolean,
    val rejectionReason: String? = null,
    val acceptedAt: Instant? = null,
)

/**
 * Состояние игры в памяти + конечный автомат переходов.
 *
 * Все мутации статуса комнаты сериализуются через `RuntimeState.mutex`,
 * корутины-таймеры держим в той же структуре, чтобы атомарно отменять при
 * любом досрочном переходе. После `QUIZ_FINISHED` — грейс [QuizProperties.finishedRoomGraceSeconds],
 * затем eviction state-а и закрытие шины.
 */
@Service
class GameService(
    private val rooms: RoomRepository,
    private val players: PlayerRepository,
    private val questions: QuestionRepository,
    private val options: OptionRepository,
    private val answers: AnswerRepository,
    private val scoring: ScoringService,
    private val statsService: StatsService,
    private val bus: RoomEventBus,
    private val quizProps: QuizProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private data class RuntimeState(
        val mutex: Mutex = Mutex(),
        var loadedQuestions: List<Pair<Question, List<OptionEntity>>>? = null,
        var timerJob: Job? = null,
        var autoAdvanceJob: Job? = null,
        var evictionJob: Job? = null,
        val answeredPlayerIds: MutableSet<UUID> = mutableSetOf(),
    )

    private val state = ConcurrentHashMap<UUID, RuntimeState>()

    private fun runtime(roomId: UUID): RuntimeState = state.computeIfAbsent(roomId) { RuntimeState() }

    suspend fun startQuiz(code: String, organizerToken: String?) {
        val room = rooms.findByCode(code) ?: throw RoomNotFoundException(code)
        if (organizerToken == null || organizerToken != room.organizerToken) throw NotOrganizerException()
        val rt = runtime(room.id!!)
        rt.mutex.withLock {
            val cur = rooms.findById(room.id) ?: throw RoomNotFoundException(code)
            if (cur.status != RoomStatus.WAITING.name) throw IllegalGameStateException("Квиз уже запущен")
            ensureQuestionsLoaded(cur)
            val saved =
                rooms.save(
                    cur.copy(
                        status = RoomStatus.IN_PROGRESS.name,
                        startedAt = Instant.now(),
                    ),
                )
            log.info("Quiz started roomCode={} totalQuestions={}", code, rt.loadedQuestions!!.size)
            bus.emit(
                saved.id!!,
                WsEnvelope(
                    EventTypes.QUIZ_STARTED,
                    QuizStartedPayload(saved.id, rt.loadedQuestions!!.size),
                ),
            )
            advanceToNextQuestionLocked(saved)
        }
    }

    suspend fun nextQuestion(code: String, organizerToken: String?) {
        val room = rooms.findByCode(code) ?: throw RoomNotFoundException(code)
        if (organizerToken == null || organizerToken != room.organizerToken) throw NotOrganizerException()
        val rt = runtime(room.id!!)
        rt.mutex.withLock {
            val cur = rooms.findById(room.id) ?: return@withLock
            if (cur.status != RoomStatus.QUESTION_RESULT.name) {
                throw IllegalGameStateException("Сейчас не время для следующего вопроса")
            }
            rt.autoAdvanceJob?.cancel()
            advanceToNextQuestionLocked(cur)
        }
    }

    suspend fun finishQuiz(code: String, organizerToken: String?) {
        val room = rooms.findByCode(code) ?: throw RoomNotFoundException(code)
        if (organizerToken == null || organizerToken != room.organizerToken) throw NotOrganizerException()
        val rt = runtime(room.id!!)
        rt.mutex.withLock {
            val cur = rooms.findById(room.id) ?: return@withLock
            if (cur.status == RoomStatus.FINISHED.name) return@withLock
            rt.timerJob?.cancel()
            rt.autoAdvanceJob?.cancel()
            finishQuizLocked(cur)
        }
    }

    suspend fun submitAnswer(
        roomCode: String,
        sessionId: String,
        questionId: UUID,
        selectedKeys: List<String>,
    ): AnswerOutcome {
        val room = rooms.findByCode(roomCode) ?: throw RoomNotFoundException(roomCode)
        val player =
            players.findByRoomIdAndSessionId(room.id!!, sessionId)
                ?: throw PlayerNotFoundException()
        val rt = runtime(room.id)
        rt.mutex.withLock {
            val cur = rooms.findById(room.id) ?: return AnswerOutcome(false, "Комната не найдена")
            if (cur.status != RoomStatus.QUESTION_ACTIVE.name) {
                return AnswerOutcome(false, "Вопрос неактивен")
            }
            val loaded = rt.loadedQuestions ?: return AnswerOutcome(false, "Вопросы не загружены")
            val (question, opts) = loaded[cur.currentQuestionIndex]
            if (question.id != questionId) return AnswerOutcome(false, "Неверный questionId")

            val now = Instant.now()
            cur.questionEndsAt?.let { if (now.isAfter(it)) return AnswerOutcome(false, "Время вышло") }

            val existing = answers.findByRoomIdAndPlayerIdAndQuestionId(room.id, player.id!!, question.id!!)
            if (existing != null) return AnswerOutcome(false, "Ответ уже принят")

            val sanitizedKeys = selectedKeys.distinct().filter { key -> opts.any { it.optionKey == key } }
            val result = scoring.score(question, opts, sanitizedKeys)
            val answerTime =
                cur.questionStartedAt?.let { Duration.between(it, now).toMillis() } ?: 0L

            answers.save(
                Answer(
                    roomId = room.id,
                    playerId = player.id,
                    questionId = question.id,
                    selectedOptions = sanitizedKeys.sorted().joinToString(","),
                    isCorrect = result.isCorrect,
                    pointsEarned = result.pointsEarned,
                    answerTimeMs = answerTime,
                ),
            )
            if (result.pointsEarned > 0) {
                players.save(player.copy(score = player.score + result.pointsEarned))
            }
            rt.answeredPlayerIds.add(player.id)

            val playerCount = players.countByRoomId(room.id).toInt()
            if (playerCount > 0 && rt.answeredPlayerIds.size >= playerCount) {
                rt.timerJob?.cancel()
                finishQuestionLocked(cur)
            }
            return AnswerOutcome(true, acceptedAt = now)
        }
    }

    private suspend fun ensureQuestionsLoaded(room: Room) {
        val rt = runtime(room.id!!)
        if (rt.loadedQuestions != null) return
        val qs = questions.findAllByQuizIdOrderByOrderNumberAsc(room.quizId).toList()
        if (qs.isEmpty()) throw IllegalGameStateException("В квизе нет вопросов")
        val opts = options.findAllByQuestionIds(qs.mapNotNull { it.id }).toList()
        val byQuestion = opts.groupBy { it.questionId }
        rt.loadedQuestions = qs.map { it to (byQuestion[it.id!!] ?: emptyList()) }
    }

    private suspend fun advanceToNextQuestionLocked(room: Room) {
        val rt = runtime(room.id!!)
        val all = rt.loadedQuestions ?: error("Questions not loaded")
        val nextIndex = room.currentQuestionIndex + 1
        if (nextIndex >= all.size) {
            finishQuizLocked(room)
            return
        }
        val (question, opts) = all[nextIndex]
        val now = Instant.now()
        val ends = now.plusSeconds(quizProps.defaultQuestionDurationSeconds)
        val updated =
            rooms.save(
                room.copy(
                    status = RoomStatus.QUESTION_ACTIVE.name,
                    currentQuestionIndex = nextIndex,
                    questionStartedAt = now,
                    questionEndsAt = ends,
                ),
            )
        rt.answeredPlayerIds.clear()
        bus.emit(
            updated.id!!,
            WsEnvelope(
                EventTypes.QUESTION_STARTED,
                QuestionStartedPayload(
                    roomId = updated.id,
                    questionId = question.id!!,
                    questionNumber = nextIndex + 1,
                    totalQuestions = all.size,
                    text = question.text,
                    options = opts.map { OptionDto(it.optionKey, it.text) },
                    type = question.type,
                    startedAt = now,
                    endsAt = ends,
                ),
            ),
        )
        rt.timerJob?.cancel()
        rt.timerJob =
            scope.launch {
                try {
                    delay(quizProps.defaultQuestionDurationSeconds * 1000)
                    runtime(updated.id).mutex.withLock {
                        val cur = rooms.findById(updated.id) ?: return@withLock
                        if (cur.status == RoomStatus.QUESTION_ACTIVE.name &&
                            cur.currentQuestionIndex == nextIndex
                        ) {
                            finishQuestionLocked(cur)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    log.error("Timer job failed for room {}", updated.id, e)
                }
            }
    }

    private suspend fun finishQuestionLocked(room: Room) {
        val rt = runtime(room.id!!)
        val all = rt.loadedQuestions ?: return
        val (question, opts) = all[room.currentQuestionIndex]
        val updated =
            rooms.save(
                room.copy(status = RoomStatus.QUESTION_RESULT.name),
            )
        bus.emit(
            updated.id!!,
            WsEnvelope(
                EventTypes.QUESTION_FINISHED,
                QuestionFinishedPayload(updated.id, question.id!!),
            ),
        )
        val resultPayload = statsService.buildQuestionResult(updated, question, opts)
        bus.emit(updated.id, WsEnvelope(EventTypes.QUESTION_RESULT, resultPayload))
        if (!updated.manualMode) {
            rt.autoAdvanceJob?.cancel()
            rt.autoAdvanceJob =
                scope.launch {
                    try {
                        delay(quizProps.autoModePauseSeconds * 1000)
                        runtime(updated.id).mutex.withLock {
                            val cur = rooms.findById(updated.id) ?: return@withLock
                            if (cur.status == RoomStatus.QUESTION_RESULT.name &&
                                cur.currentQuestionIndex == updated.currentQuestionIndex
                            ) {
                                advanceToNextQuestionLocked(cur)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        log.error("Auto advance job failed for room {}", updated.id, e)
                    }
                }
        }
    }

    private suspend fun finishQuizLocked(room: Room) {
        val finished =
            rooms.save(
                room.copy(
                    status = RoomStatus.FINISHED.name,
                    finishedAt = Instant.now(),
                ),
            )
        val payload = statsService.buildFinalRanking(finished)
        bus.emit(finished.id!!, WsEnvelope(EventTypes.QUIZ_FINISHED, payload))
        scheduleEviction(finished.id)
        log.info("Quiz finished roomCode={}", finished.code)
    }

    /**
     * После `FINISHED` ждём грейс-период (фронт скачивает `/results`),
     * потом удаляем runtime-state и закрываем шину для комнаты.
     */
    private fun scheduleEviction(roomId: UUID) {
        val rt = runtime(roomId)
        rt.evictionJob?.cancel()
        rt.evictionJob =
            scope.launch {
                try {
                    delay(quizProps.finishedRoomGraceSeconds * 1000)
                    evictRoom(roomId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    log.error("Eviction job failed for room {}", roomId, e)
                }
            }
    }

    /** Видно тестам: убедиться, что всё прибралось после грейса. */
    fun evictRoom(roomId: UUID) {
        val removed = state.remove(roomId)
        removed?.timerJob?.cancel()
        removed?.autoAdvanceJob?.cancel()
        removed?.evictionJob?.cancel()
        bus.close(roomId)
        log.debug("Evicted runtime state for room {}", roomId)
    }

    /** Кол-во активных runtime-state — используется в тестах для проверки утечек. */
    fun activeRuntimeCount(): Int = state.size

    @PreDestroy
    fun shutdown() {
        scope.cancel()
        state.keys.toList().forEach { evictRoom(it) }
    }
}
