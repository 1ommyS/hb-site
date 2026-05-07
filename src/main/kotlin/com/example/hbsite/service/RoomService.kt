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
import kotlinx.coroutines.flow.toList
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
) {
    suspend fun createRoom(): CreatedRoom {
        val quiz =
            quizzes.findBySlug(quizProps.defaultQuizSlug)
                ?: throw IllegalStateException("Default quiz not seeded")
        val totalQuestions = questions.countByQuizId(quiz.id!!).toInt()

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
        return CreatedRoom(saved, quiz, totalQuestions)
    }

    suspend fun findRoomByCode(code: String): Room =
        rooms.findByCode(code) ?: throw RoomNotFoundException(code)

    suspend fun joinRoom(code: String, name: String, sessionId: String): JoinedPlayer {
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
        return JoinedPlayer(room, saved, isReconnect = false)
    }

    suspend fun listPlayers(roomId: UUID): List<Player> =
        players.findAllByRoomIdOrderByJoinedAtAsc(roomId).toList()

    suspend fun requireOrganizer(room: Room, token: String?) {
        if (token == null || token != room.organizerToken) throw NotOrganizerException()
    }
}
