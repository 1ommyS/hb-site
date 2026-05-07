package com.example.hbsite.repo

import com.example.hbsite.domain.Answer
import com.example.hbsite.domain.OptionEntity
import com.example.hbsite.domain.Player
import com.example.hbsite.domain.Question
import com.example.hbsite.domain.Quiz
import com.example.hbsite.domain.Room
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface QuizRepository : CoroutineCrudRepository<Quiz, UUID> {
    @Query("SELECT * FROM quizzes WHERE id = md5(:slug)::uuid")
    suspend fun findBySlug(slug: String): Quiz?
}

@Repository
interface QuestionRepository : CoroutineCrudRepository<Question, UUID> {
    fun findAllByQuizIdOrderByOrderNumberAsc(quizId: UUID): Flow<Question>

    suspend fun countByQuizId(quizId: UUID): Long
}

@Repository
interface OptionRepository : CoroutineCrudRepository<OptionEntity, UUID> {
    fun findAllByQuestionIdOrderByOptionKeyAsc(questionId: UUID): Flow<OptionEntity>

    @Query(
        """
        SELECT * FROM options
        WHERE question_id IN (:questionIds)
        ORDER BY option_key ASC
        """,
    )
    fun findAllByQuestionIds(questionIds: Collection<UUID>): Flow<OptionEntity>
}

@Repository
interface RoomRepository : CoroutineCrudRepository<Room, UUID> {
    suspend fun findByCode(code: String): Room?
}

@Repository
interface PlayerRepository : CoroutineCrudRepository<Player, UUID> {
    fun findAllByRoomIdOrderByJoinedAtAsc(roomId: UUID): Flow<Player>

    suspend fun findByRoomIdAndSessionId(
        roomId: UUID,
        sessionId: String,
    ): Player?

    suspend fun countByRoomId(roomId: UUID): Long

    @Query("SELECT count(*) FROM players WHERE room_id = :roomId AND name = :name")
    suspend fun countByRoomIdAndName(
        roomId: UUID,
        name: String,
    ): Long
}

@Repository
interface AnswerRepository : CoroutineCrudRepository<Answer, UUID> {
    suspend fun findByRoomIdAndPlayerIdAndQuestionId(
        roomId: UUID,
        playerId: UUID,
        questionId: UUID,
    ): Answer?

    fun findAllByRoomIdAndQuestionId(
        roomId: UUID,
        questionId: UUID,
    ): Flow<Answer>

    fun findAllByRoomId(roomId: UUID): Flow<Answer>
}
