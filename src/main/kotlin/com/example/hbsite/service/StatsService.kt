package com.example.hbsite.service

import com.example.hbsite.domain.OptionEntity
import com.example.hbsite.domain.Player
import com.example.hbsite.domain.Question
import com.example.hbsite.domain.Room
import com.example.hbsite.repo.AnswerRepository
import com.example.hbsite.repo.OptionRepository
import com.example.hbsite.repo.PlayerRepository
import com.example.hbsite.repo.QuestionRepository
import com.example.hbsite.ws.FinalRankingRow
import com.example.hbsite.ws.PlayerAnswerStat
import com.example.hbsite.ws.PlayerDto
import com.example.hbsite.ws.QuestionResultPayload
import com.example.hbsite.ws.QuizFinishedPayload
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class StatsService(
    private val players: PlayerRepository,
    private val questions: QuestionRepository,
    private val options: OptionRepository,
    private val answers: AnswerRepository,
) {
    suspend fun buildQuestionResult(
        room: Room,
        question: Question,
        opts: List<OptionEntity>,
    ): QuestionResultPayload {
        val ans = answers.findAllByRoomIdAndQuestionId(room.id!!, question.id!!).toList()
        val ps = players.findAllByRoomIdOrderByJoinedAtAsc(room.id).toList()
        val playerById = ps.associateBy { it.id!! }

        val distribution = LinkedHashMap<String, Int>()
        opts.forEach { distribution[it.optionKey] = 0 }
        ans.forEach { a ->
            a.selectedOptions
                .split(",")
                .filter { it.isNotBlank() }
                .forEach { key ->
                    distribution[key] = (distribution[key] ?: 0) + 1
                }
        }

        val playerAnswers =
            ans.map { a ->
                val pl = playerById[a.playerId]
                PlayerAnswerStat(
                    playerId = a.playerId,
                    name = pl?.name ?: "?",
                    selectedOptions = a.selectedOptions.split(",").filter { it.isNotBlank() },
                    isCorrect = a.isCorrect,
                    pointsEarned = a.pointsEarned,
                    totalScore = pl?.score ?: 0,
                )
            }

        val ranking =
            ps.sortedByDescending { it.score }
                .map { PlayerDto(it.id!!, it.name, it.score) }

        return QuestionResultPayload(
            roomId = room.id,
            questionId = question.id,
            correctOptions = opts.filter { it.isCorrect }.map { it.optionKey },
            comment = question.afterAnswerComment,
            distribution = distribution,
            playerAnswers = playerAnswers,
            ranking = ranking,
        )
    }

    suspend fun buildFinalRanking(room: Room): QuizFinishedPayload {
        val ps = players.findAllByRoomIdOrderByJoinedAtAsc(room.id!!).toList()
        val ans = answers.findAllByRoomId(room.id).toList()
        val byPlayer = ans.groupBy { it.playerId }

        data class Row(
            val player: Player,
            val correct: Int,
            val avgMs: Long,
            val lastCorrectMs: Long,
        )

        val rows =
            ps
                .map { p ->
                    val pa = byPlayer[p.id] ?: emptyList()
                    val correct = pa.count { it.isCorrect }
                    val avg = if (pa.isEmpty()) Long.MAX_VALUE else pa.sumOf { it.answerTimeMs } / pa.size
                    val lastCorrect =
                        pa
                            .filter { it.isCorrect }
                            .maxOfOrNull { it.answeredAt?.toEpochMilli() ?: 0L } ?: Long.MAX_VALUE
                    Row(p, correct, avg, lastCorrect)
                }.sortedWith(
                    compareByDescending<Row> { it.player.score }
                        .thenByDescending { it.correct }
                        .thenBy { it.avgMs }
                        .thenBy { it.lastCorrectMs },
                )

        return QuizFinishedPayload(
            roomId = room.id,
            ranking =
                rows.mapIndexed { i, r ->
                    FinalRankingRow(
                        rank = i + 1,
                        playerId = r.player.id!!,
                        name = r.player.name,
                        score = r.player.score,
                        correctAnswers = r.correct,
                        averageAnswerTimeMs = if (r.avgMs == Long.MAX_VALUE) 0L else r.avgMs,
                    )
                },
        )
    }
}
