package com.example.hbsite.service

import com.example.hbsite.config.QuizProperties
import com.example.hbsite.domain.Player
import com.example.hbsite.domain.Quiz
import com.example.hbsite.domain.Room
import com.example.hbsite.domain.RoomStatus
import com.example.hbsite.repo.PlayerRepository
import com.example.hbsite.repo.QuestionRepository
import com.example.hbsite.repo.QuizRepository
import com.example.hbsite.repo.RoomRepository
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.PlayerDto
import com.example.hbsite.ws.PlayerJoinedPayload
import com.example.hbsite.ws.PlayerLeftPayload
import com.example.hbsite.ws.RoomEventBus
import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

data class CreatedRoom(
    val room: Room,
    val quiz: Quiz,
    val totalQuestions: Int,
)

data class JoinedPlayer(
    val room: Room,
    val player: Player,
    val isReconnect: Boolean,
)

@Service
class RoomService(
    private val rooms: RoomRepository,
    private val players: PlayerRepository,
    private val quizzes: QuizRepository,
    private val questions: QuestionRepository,
    private val codeGenerator: CodeGenerator,
    private val nameSanitizer: NameSanitizer,
    private val quizProps: QuizProperties,
    private val bus: RoomEventBus,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun createRoom(): CreatedRoom {
        val quiz =
            quizzes.findBySlug(quizProps.defaultQuizSlug)
                ?: throw IllegalStateException("Default quiz not seeded")
        val totalQuestions = questions.countByQuizId(quiz.id!!).toInt()
        if (totalQuestions == 0) {
            throw IllegalStateException("Default quiz has no questions seeded")
        }

        var code = codeGenerator.generate()
        var attempts = 0
        while (rooms.findByCode(code) != null) {
            code = codeGenerator.generate()
            if (++attempts > 10) throw IllegalStateException("Cannot generate unique room code")
        }

        val token = UUID.randomUUID().toString()
        val saved =
            rooms.save(
                Room(
                    code = code,
                    quizId = quiz.id,
                    organizerToken = token,
                    status = RoomStatus.WAITING.name,
                    manualMode = quizProps.manualMode,
                ),
            )
        log.info("Room created code={} totalQuestions={}", saved.code, totalQuestions)
        return CreatedRoom(saved, quiz, totalQuestions)
    }

    suspend fun findRoomByCode(code: String): Room =
        rooms.findByCode(code) ?: throw RoomNotFoundException(code)

    /**
     * Идемпотентно: первый вызов создаёт игрока и публикует [EventTypes.PLAYER_JOINED];
     * повторный вызов с тем же `sessionId` просто возвращает существующего игрока с
     * `isReconnect=true` без эмиссии в шину.
     *
     * Эмиссия живёт здесь, а не в контроллере/WS-handler, чтобы любой путь входа
     * (REST, WS) приводил ровно к одному событию.
     */
    suspend fun joinRoom(
        code: String,
        name: String,
        sessionId: String,
    ): JoinedPlayer {
        val room = findRoomByCode(code)
        val cleaned = nameSanitizer.sanitize(name)
        if (cleaned.length !in 2..20) {
            throw InvalidPlayerNameException("Имя должно быть от 2 до 20 символов")
        }

        players.findByRoomIdAndSessionId(room.id!!, sessionId)?.let {
            return JoinedPlayer(room, it, isReconnect = true)
        }

        val same = players.countByRoomIdAndName(room.id, cleaned)
        val finalName = if (same > 0) "$cleaned #${same + 1}" else cleaned

        val saved =
            players.save(
                Player(
                    roomId = room.id,
                    name = finalName,
                    sessionId = sessionId,
                ),
            )
        emitPlayersUpdate(room.id, EventTypes.PLAYER_JOINED, saved)
        log.info("Player joined roomCode={} playerId={} name={}", code, saved.id, saved.name)
        return JoinedPlayer(room, saved, isReconnect = false)
    }

    /**
     * Вызывается из WS-обработчика при разрыве соединения игрока.
     * Игрока из БД не удаляем (ответы в `answers` ссылаются), просто шлём `PLAYER_LEFT`.
     */
    suspend fun handleDisconnect(roomId: UUID, playerId: UUID) {
        val player = players.findById(playerId) ?: return
        emitPlayersUpdate(roomId, EventTypes.PLAYER_LEFT, player)
        log.info("Player disconnected roomId={} playerId={}", roomId, player.id)
    }

    suspend fun listPlayers(roomId: UUID): List<Player> =
        players.findAllByRoomIdOrderByJoinedAtAsc(roomId).toList()

    suspend fun requireOrganizer(room: Room, token: String?) {
        if (token == null || token != room.organizerToken) throw NotOrganizerException()
    }

    private suspend fun emitPlayersUpdate(roomId: UUID, type: String, player: Player) {
        val all =
            listPlayers(roomId).map { PlayerDto(it.id!!, it.name, it.score) }
        val playerDto = PlayerDto(player.id!!, player.name, player.score)
        val envelope =
            when (type) {
                EventTypes.PLAYER_JOINED ->
                    WsEnvelope(
                        type,
                        PlayerJoinedPayload(roomId = roomId, player = playerDto, players = all),
                    )
                EventTypes.PLAYER_LEFT ->
                    WsEnvelope(
                        type,
                        PlayerLeftPayload(roomId = roomId, playerId = player.id, players = all),
                    )
                else -> error("Unsupported player event type: $type")
            }
        bus.emit(roomId, envelope)
    }
}
