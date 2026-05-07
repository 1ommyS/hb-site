package com.example.hbsite.api

import com.example.hbsite.ws.PlayerDto
import java.time.Instant
import java.util.UUID

data class CreateRoomResponse(
    val roomId: UUID,
    val code: String,
    val organizerToken: String,
    val quizTitle: String,
    val totalQuestions: Int,
    val joinUrl: String,
    val websocketUrl: String,
)

data class RoomInfoResponse(
    val roomId: UUID,
    val code: String,
    val status: String,
    val totalQuestions: Int,
    val currentQuestionIndex: Int,
    val players: List<PlayerDto>,
    val createdAt: Instant?,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val annoyingModeEnabled: Boolean,
    val manualMode: Boolean,
)

data class JoinRoomRequest(
    val name: String,
    val sessionId: String,
)

data class JoinRoomResponse(
    val roomId: UUID,
    val player: PlayerDto,
    val isReconnect: Boolean,
)

data class ErrorResponse(
    val message: String,
    val code: String? = null,
)

data class QuestionDistributionDto(
    val questionId: UUID,
    val questionNumber: Int,
    val text: String,
    val correctOptions: List<String>,
    val distribution: Map<String, Int>,
)

data class StatsResponse(
    val roomId: UUID,
    val mostPopularWrong: String?,
    val hardestQuestionId: UUID?,
    val unanimouslyCorrectQuestionId: UUID?,
    val confusingQuestionId: UUID?,
    val perQuestion: List<QuestionDistributionDto>,
)
