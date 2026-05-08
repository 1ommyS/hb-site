package com.example.hbsite

import com.example.hbsite.api.StatsResponse
import com.example.hbsite.repo.OptionRepository
import com.example.hbsite.repo.QuestionRepository
import com.example.hbsite.repo.QuizRepository
import com.example.hbsite.support.IntegrationTestBase
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.QuizFinishedPayload
import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * /api/rooms/{code}/stats и /results после завершённого квиза.
 */
class StatsIT : IntegrationTestBase() {
    @Autowired
    private lateinit var quizzes: QuizRepository

    @Autowired
    private lateinit var questions: QuestionRepository

    @Autowired
    private lateinit var options: OptionRepository

    @Test
    fun `stats endpoint returns distribution after at least one answered question`() =
        runTest {
            val room = fixtures.createRoom()
            val sessionId = UUID.randomUUID().toString()
            fixtures.joinViaRest(room.code, "Аня", sessionId)
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val player = fixtures.connectPlayer(room.code, "Аня", sessionId)

            val quiz = runBlocking { quizzes.findBySlug("hb-default-quiz-vanya")!! }
            val q = runBlocking { questions.findAllByQuizIdOrderByOrderNumberAsc(quiz.id!!).toList().first() }
            val opts = runBlocking { options.findAllByQuestionIds(listOf(q.id!!)).toList() }
            val correctKeys = opts.filter { it.isCorrect }.map { it.optionKey }

            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))
                player.awaitEvent(EventTypes.QUESTION_STARTED)
                player.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q.id.toString(), "selectedOptions" to correctKeys),
                    ),
                )
                org.awaitEvent(EventTypes.QUESTION_RESULT)
                org.send(WsEnvelope(EventTypes.FINISH_QUIZ, emptyMap<String, Any>()))
                org.awaitEvent(EventTypes.QUIZ_FINISHED)
            } finally {
                org.close()
                player.close()
            }

            val stats =
                webTestClient
                    .get()
                    .uri("/api/rooms/{code}/stats", room.code)
                    .exchange()
                    .expectStatus()
                    .isOk
                    .expectBody(StatsResponse::class.java)
                    .returnResult()
                    .responseBody!!

            val first = stats.perQuestion.first { it.questionId == q.id }
            assertEquals(correctKeys.toSet(), first.correctOptions.toSet())
            // Сумма distribution = число ответивших на вопрос.
            assertEquals(correctKeys.size, first.distribution.values.sum())
        }

    @Test
    fun `final ranking sorts by score desc`() =
        runTest {
            val room = fixtures.createRoom()
            val sessionA = UUID.randomUUID().toString()
            val sessionB = UUID.randomUUID().toString()
            fixtures.joinViaRest(room.code, "Аня", sessionA)
            fixtures.joinViaRest(room.code, "Бен", sessionB)

            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val playerA = fixtures.connectPlayer(room.code, "Аня", sessionA)
            val playerB = fixtures.connectPlayer(room.code, "Бен", sessionB)

            val quiz = runBlocking { quizzes.findBySlug("hb-default-quiz-vanya")!! }
            val q = runBlocking { questions.findAllByQuizIdOrderByOrderNumberAsc(quiz.id!!).toList().first() }
            val opts = runBlocking { options.findAllByQuestionIds(listOf(q.id!!)).toList() }
            val correctKeys = opts.filter { it.isCorrect }.map { it.optionKey }
            val wrongKey = opts.first { !it.isCorrect }.optionKey

            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))
                playerA.awaitEvent(EventTypes.QUESTION_STARTED)
                playerA.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q.id.toString(), "selectedOptions" to correctKeys),
                    ),
                )
                playerB.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q.id.toString(), "selectedOptions" to listOf(wrongKey)),
                    ),
                )
                org.awaitEvent(EventTypes.QUESTION_RESULT)
                org.send(WsEnvelope(EventTypes.FINISH_QUIZ, emptyMap<String, Any>()))
                val finished = org.awaitEvent(EventTypes.QUIZ_FINISHED)

                @Suppress("UNCHECKED_CAST")
                val ranking = (finished.payload as Map<String, Any>)["ranking"] as List<Map<String, Any>>
                assertEquals(2, ranking.size)
                assertEquals("Аня", ranking[0]["name"], "Аня ответила правильно — должна быть первой")
                assertTrue((ranking[0]["score"] as Number).toInt() > (ranking[1]["score"] as Number).toInt())
            } finally {
                org.close()
                playerA.close()
                playerB.close()
            }
        }
}
