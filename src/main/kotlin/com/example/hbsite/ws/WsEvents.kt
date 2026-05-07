package com.example.hbsite.ws

import java.time.Instant
import java.util.UUID

data class WsEnvelope(
    val type: String,
    val payload: Any? = null,
)

object EventTypes {
    // server -> client
    const val ROOM_CREATED = "ROOM_CREATED"
    const val PLAYER_JOINED = "PLAYER_JOINED"
    const val PLAYER_LEFT = "PLAYER_LEFT"
    const val QUIZ_STARTED = "QUIZ_STARTED"
    const val QUESTION_STARTED = "QUESTION_STARTED"
    const val ANSWER_ACCEPTED = "ANSWER_ACCEPTED"
    const val ANSWER_REJECTED = "ANSWER_REJECTED"
    const val QUESTION_FINISHED = "QUESTION_FINISHED"
    const val QUESTION_RESULT = "QUESTION_RESULT"
    const val QUIZ_FINISHED = "QUIZ_FINISHED"
    const val ROOM_STATE_SYNC = "ROOM_STATE_SYNC"
    const val ERROR = "ERROR"
    const val PONG = "PONG"

    // client -> server
    const val JOIN_ROOM = "JOIN_ROOM"
    const val START_QUIZ = "START_QUIZ"
    const val SUBMIT_ANSWER = "SUBMIT_ANSWER"
    const val NEXT_QUESTION = "NEXT_QUESTION"
    const val FINISH_QUIZ = "FINISH_QUIZ"
    const val PING = "PING"
}

data class PlayerDto(
    val id: UUID,
    val name: String,
    val score: Int = 0,
)

data class OptionDto(
    val id: String,
    val text: String,
)

data class RoomCreatedPayload(
    val roomId: UUID,
    val code: String,
    val organizerToken: String,
    val quizTitle: String,
    val totalQuestions: Int,
)

data class PlayerJoinedPayload(
    val roomId: UUID,
    val player: PlayerDto,
    val players: List<PlayerDto>,
)

data class PlayerLeftPayload(
    val roomId: UUID,
    val playerId: UUID,
    val players: List<PlayerDto>,
)

data class QuizStartedPayload(
    val roomId: UUID,
    val totalQuestions: Int,
)

data class QuestionStartedPayload(
    val roomId: UUID,
    val questionId: UUID,
    val questionNumber: Int,
    val totalQuestions: Int,
    val text: String,
    val options: List<OptionDto>,
    val type: String,
    val startedAt: Instant,
    val endsAt: Instant,
)

data class AnswerAcceptedPayload(
    val questionId: UUID,
    val receivedAt: Instant,
)

data class AnswerRejectedPayload(
    val questionId: UUID,
    val reason: String,
)

data class QuestionFinishedPayload(
    val roomId: UUID,
    val questionId: UUID,
)

data class PlayerAnswerStat(
    val playerId: UUID,
    val name: String,
    val selectedOptions: List<String>,
    val isCorrect: Boolean,
    val pointsEarned: Int,
    val totalScore: Int,
)

data class QuestionResultPayload(
    val roomId: UUID,
    val questionId: UUID,
    val correctOptions: List<String>,
    val comment: String?,
    val distribution: Map<String, Int>,
    val playerAnswers: List<PlayerAnswerStat>,
    val ranking: List<PlayerDto>,
)

data class FinalRankingRow(
    val rank: Int,
    val playerId: UUID,
    val name: String,
    val score: Int,
    val correctAnswers: Int,
    val averageAnswerTimeMs: Long,
)

data class QuizFinishedPayload(
    val roomId: UUID,
    val ranking: List<FinalRankingRow>,
)

data class RoomStateSyncPayload(
    val roomId: UUID,
    val code: String,
    val status: String,
    val currentQuestionIndex: Int,
    val totalQuestions: Int,
    val players: List<PlayerDto>,
    val questionStartedAt: Instant?,
    val questionEndsAt: Instant?,
    val annoyingModeEnabled: Boolean,
)

data class ErrorPayload(
    val message: String,
    val code: String? = null,
)
