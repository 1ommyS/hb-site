package com.example.hbsite.ws

import com.example.hbsite.repo.PlayerRepository
import com.example.hbsite.repo.QuestionRepository
import com.example.hbsite.repo.RoomRepository
import com.example.hbsite.service.GameService
import com.example.hbsite.service.RoomService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Component
class GameWebSocketHandler(
    private val roomService: RoomService,
    private val gameService: GameService,
    private val bus: RoomEventBus,
    private val rooms: RoomRepository,
    private val players: PlayerRepository,
    private val questions: QuestionRepository,
    private val mapper: JsonMapper,
) : WebSocketHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val out: Sinks.Many<WsEnvelope> = Sinks.many().unicast().onBackpressureBuffer()
        val ctx = ConnectionContext(out)

        val outbound =
            session.send(
                out.asFlux().map { env ->
                    session.textMessage(mapper.writeValueAsString(env))
                },
            )

        val inbound =
            session
                .receive()
                .map(WebSocketMessage::getPayloadAsText)
                .concatMap { raw ->
                    mono(Dispatchers.Default) {
                        runCatching { handle(ctx, raw) }
                            .onFailure { e ->
                                log.warn("WS error: {}", e.message)
                                ctx.emit(EventTypes.ERROR, ErrorPayload(e.message ?: "Ошибка"))
                            }
                    }
                }.doFinally {
                    ctx.subscription?.dispose()
                    out.tryEmitComplete()
                }.then()

        return Mono.zip(inbound, outbound).then()
    }

    private suspend fun handle(
        ctx: ConnectionContext,
        raw: String,
    ) {
        val msg = mapper.readTree(raw)
        val type = msg.path("type").asString(null) ?: throw IllegalArgumentException("Missing type")
        val payload = msg.path("payload")

        when (type) {
            EventTypes.JOIN_ROOM -> {
                onJoin(ctx, payload)
            }

            EventTypes.START_QUIZ -> {
                requireJoined(ctx)
                val room = rooms.findById(ctx.roomId!!) ?: return
                gameService.startQuiz(room.code, ctx.organizerToken)
            }

            EventTypes.NEXT_QUESTION -> {
                requireJoined(ctx)
                val room = rooms.findById(ctx.roomId!!) ?: return
                gameService.nextQuestion(room.code, ctx.organizerToken)
            }

            EventTypes.FINISH_QUIZ -> {
                requireJoined(ctx)
                val room = rooms.findById(ctx.roomId!!) ?: return
                gameService.finishQuiz(room.code, ctx.organizerToken)
            }

            EventTypes.SUBMIT_ANSWER -> {
                onSubmitAnswer(ctx, payload)
            }

            EventTypes.PING -> {
                ctx.emit(EventTypes.PONG, null)
            }

            else -> {
                ctx.emit(EventTypes.ERROR, ErrorPayload("Неизвестный тип события: $type"))
            }
        }
    }

    private suspend fun onJoin(
        ctx: ConnectionContext,
        payload: JsonNode,
    ) {
        val code = payload.path("roomCode").asString(null) ?: error("roomCode required")
        val organizerToken = payload.path("organizerToken").asString(null)
        val sessionId = payload.path("sessionId").asString(null)
        val name = payload.path("name").asString(null)

        val room = roomService.findRoomByCode(code)

        if (organizerToken != null) {
            if (organizerToken != room.organizerToken) {
                ctx.emit(EventTypes.ERROR, ErrorPayload("Неверный токен организатора"))
                return
            }
            ctx.roomId = room.id
            ctx.organizerToken = organizerToken
            ctx.sessionId = sessionId
            subscribeToRoom(ctx, room.id!!)
            sendStateSync(ctx, room.id)
            return
        }

        if (sessionId.isNullOrBlank()) {
            ctx.emit(EventTypes.ERROR, ErrorPayload("sessionId required"))
            return
        }
        if (name.isNullOrBlank()) {
            ctx.emit(EventTypes.ERROR, ErrorPayload("name required"))
            return
        }

        val joined = roomService.joinRoom(code, name, sessionId)
        ctx.roomId = joined.room.id
        ctx.sessionId = sessionId
        ctx.playerId = joined.player.id

        subscribeToRoom(ctx, joined.room.id!!)
        sendStateSync(ctx, joined.room.id)

        if (!joined.isReconnect) {
            val all = roomService.listPlayers(joined.room.id).map { PlayerDto(it.id!!, it.name, it.score) }
            bus.emit(
                joined.room.id,
                WsEnvelope(
                    EventTypes.PLAYER_JOINED,
                    PlayerJoinedPayload(
                        roomId = joined.room.id,
                        player = PlayerDto(joined.player.id!!, joined.player.name, joined.player.score),
                        players = all,
                    ),
                ),
            )
        }
    }

    private suspend fun onSubmitAnswer(
        ctx: ConnectionContext,
        payload: JsonNode,
    ) {
        requireJoined(ctx)
        val sessionId = ctx.sessionId ?: error("sessionId required")
        val questionId =
            payload.path("questionId").asString(null)?.let(UUID::fromString)
                ?: error("questionId required")
        val selected: List<String> =
            buildList {
                val node = payload.path("selectedOptions")
                if (node.isArray) {
                    for (item in node) add(item.asString())
                }
            }
        val room = rooms.findById(ctx.roomId!!) ?: error("room missing")
        val outcome = gameService.submitAnswer(room.code, sessionId, questionId, selected)
        if (outcome.accepted) {
            ctx.emit(
                EventTypes.ANSWER_ACCEPTED,
                AnswerAcceptedPayload(questionId, outcome.acceptedAt!!),
            )
        } else {
            ctx.emit(
                EventTypes.ANSWER_REJECTED,
                AnswerRejectedPayload(questionId, outcome.rejectionReason ?: "Отклонено"),
            )
        }
    }

    private fun subscribeToRoom(
        ctx: ConnectionContext,
        roomId: UUID,
    ) {
        ctx.subscription?.dispose()
        ctx.subscription =
            bus
                .flux(roomId)
                .subscribe { env -> ctx.out.tryEmitNext(env) }
    }

    private suspend fun sendStateSync(
        ctx: ConnectionContext,
        roomId: UUID,
    ) {
        val room = rooms.findById(roomId) ?: return
        val ps = players.findAllByRoomIdOrderByJoinedAtAsc(roomId).toList()
        val total = questions.countByQuizId(room.quizId).toInt()
        ctx.emit(
            EventTypes.ROOM_STATE_SYNC,
            RoomStateSyncPayload(
                roomId = roomId,
                code = room.code,
                status = room.status,
                currentQuestionIndex = room.currentQuestionIndex,
                totalQuestions = total,
                players = ps.map { PlayerDto(it.id!!, it.name, it.score) },
                questionStartedAt = room.questionStartedAt,
                questionEndsAt = room.questionEndsAt,
                annoyingModeEnabled = room.annoyingModeEnabled,
            ),
        )
    }

    private fun requireJoined(ctx: ConnectionContext) {
        if (ctx.roomId == null) error("Сначала отправьте JOIN_ROOM")
    }
}

private class ConnectionContext(
    val out: Sinks.Many<WsEnvelope>,
    var roomId: UUID? = null,
    var sessionId: String? = null,
    var playerId: UUID? = null,
    var organizerToken: String? = null,
    var subscription: Disposable? = null,
) {
    fun emit(
        type: String,
        payload: Any?,
    ) {
        out.tryEmitNext(WsEnvelope(type, payload))
    }
}
