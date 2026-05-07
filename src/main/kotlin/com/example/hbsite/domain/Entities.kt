package com.example.hbsite.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("quizzes")
data class Quiz(
    @Id val id: UUID? = null,
    val title: String,
    val description: String? = null,
    @Column("created_at") val createdAt: Instant? = null,
)

@Table("questions")
data class Question(
    @Id val id: UUID? = null,
    @Column("quiz_id") val quizId: UUID,
    val text: String,
    val type: String,
    val difficulty: String = "medium",
    val points: Int = 1,
    @Column("after_answer_comment") val afterAnswerComment: String? = null,
    @Column("order_number") val orderNumber: Int,
)

@Table("options")
data class OptionEntity(
    @Id val id: UUID? = null,
    @Column("question_id") val questionId: UUID,
    @Column("option_key") val optionKey: String,
    val text: String,
    @Column("is_correct") val isCorrect: Boolean = false,
)

@Table("rooms")
data class Room(
    @Id val id: UUID? = null,
    val code: String,
    @Column("quiz_id") val quizId: UUID,
    @Column("organizer_token") val organizerToken: String,
    val status: String,
    @Column("current_question_index") val currentQuestionIndex: Int = -1,
    @Column("manual_mode") val manualMode: Boolean = true,
    @Column("annoying_mode_enabled") val annoyingModeEnabled: Boolean = true,
    @Column("question_started_at") val questionStartedAt: Instant? = null,
    @Column("question_ends_at") val questionEndsAt: Instant? = null,
    @Column("created_at") val createdAt: Instant? = null,
    @Column("started_at") val startedAt: Instant? = null,
    @Column("finished_at") val finishedAt: Instant? = null,
)

@Table("players")
data class Player(
    @Id val id: UUID? = null,
    @Column("room_id") val roomId: UUID,
    val name: String,
    @Column("session_id") val sessionId: String,
    val score: Int = 0,
    @Column("joined_at") val joinedAt: Instant? = null,
    @Column("last_seen_at") val lastSeenAt: Instant? = null,
)

@Table("answers")
data class Answer(
    @Id val id: UUID? = null,
    @Column("room_id") val roomId: UUID,
    @Column("player_id") val playerId: UUID,
    @Column("question_id") val questionId: UUID,
    @Column("selected_options") val selectedOptions: String,
    @Column("is_correct") val isCorrect: Boolean,
    @Column("points_earned") val pointsEarned: Int = 0,
    @Column("answered_at") val answeredAt: Instant? = null,
    @Column("answer_time_ms") val answerTimeMs: Long,
)
