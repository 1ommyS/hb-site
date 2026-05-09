package com.example.hbsite.service

import com.example.hbsite.api.QuestionDistributionDto
import com.example.hbsite.api.StatsResponse
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
    suspend fun buildRoomStats(room: Room): StatsResponse {
        val qs = questions.findAllByQuizIdOrderByOrderNumberAsc(room.quizId).toList()
        val optsByQuestion = options.findAllByQuestionIds(qs.mapNotNull { it.id }).toList().groupBy { it.questionId }
        val allAnswers = answers.findAllByRoomId(room.id!!).toList()
        val answersByQuestion = allAnswers.groupBy { it.questionId }

        val perQuestion =
            qs.mapIndexed { index, question ->
                val questionOptions = optsByQuestion[question.id!!] ?: emptyList()
                QuestionDistributionDto(
                    questionId = question.id,
                    questionNumber = index + 1,
                    text = question.text,
                    correctOptions = questionOptions.filter { it.isCorrect }.map { it.optionKey },
                    distribution = buildDistribution(questionOptions, answersByQuestion[question.id].orEmpty()),
                )
            }

        return StatsResponse(
            roomId = room.id,
            mostPopularWrong = mostPopularWrongAnswer(allAnswers),
            hardestQuestionId = hardestQuestion(answersByQuestion),
            unanimouslyCorrectQuestionId = answersByQuestion.entries.firstOrNull { (_, v) ->
                v.isNotEmpty() && v.all { it.isCorrect }
            }?.key,
            confusingQuestionId = answersByQuestion.entries.firstOrNull { (_, v) ->
                v.isNotEmpty() && v.none { it.isCorrect }
            }?.key,
            perQuestion = perQuestion,
        )
    }

    suspend fun buildQuestionResult(
        room: Room,
        question: Question,
        opts: List<OptionEntity>,
    ): QuestionResultPayload {
        val ans = answers.findAllByRoomIdAndQuestionId(room.id!!, question.id!!).toList()
        val ps = players.findAllByRoomIdOrderByJoinedAtAsc(room.id).toList()
        val playerById = ps.associateBy { it.id!! }

        val distribution = buildDistribution(opts, ans)

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

    private fun buildDistribution(
        opts: List<OptionEntity>,
        ans: List<com.example.hbsite.domain.Answer>,
    ): Map<String, Int> {
        val distribution = LinkedHashMap<String, Int>()
        opts.forEach { distribution[it.optionKey] = 0 }
        ans.forEach { answer ->
            answer.selectedOptions
                .split(",")
                .filter { it.isNotBlank() }
                .forEach { key -> distribution[key] = (distribution[key] ?: 0) + 1 }
        }
        return distribution
    }

    private fun mostPopularWrongAnswer(ans: List<com.example.hbsite.domain.Answer>): String? =
        ans
            .asSequence()
            .filter { !it.isCorrect }
            .flatMap { it.selectedOptions.split(",").asSequence() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

    private fun hardestQuestion(
        answersByQuestion: Map<java.util.UUID, List<com.example.hbsite.domain.Answer>>,
    ): java.util.UUID? =
        answersByQuestion
            .mapValues { (_, answers) -> answers.count { it.isCorrect }.toDouble() / answers.size.coerceAtLeast(1) }
            .minByOrNull { it.value }
            ?.key
}
