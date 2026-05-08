package com.example.hbsite

import com.example.hbsite.repo.OptionRepository
import com.example.hbsite.repo.QuestionRepository
import com.example.hbsite.repo.QuizRepository
import com.example.hbsite.support.IntegrationTestBase
import com.example.hbsite.support.payloadMap
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Сценарии скоринга — полагаемся на сидированный квиз: достаём из БД
 * правильные опции для первого вопроса и шлём их / неправильные / повтор.
 */
class AnswerScoringIT : IntegrationTestBase() {
    @Autowired
    private lateinit var quizzes: QuizRepository

    @Autowired
    private lateinit var questions: QuestionRepository

    @Autowired
    private lateinit var options: OptionRepository

    private data class FirstQuestion(
        val id: UUID,
        val correctKeys: List<String>,
        val anyWrongKey: String,
    )

    private fun firstSeededQuestion(): FirstQuestion =
        runBlocking {
            val quiz = quizzes.findBySlug("hb-default-quiz-vanya")!!
            val q = questions.findAllByQuizIdOrderByOrderNumberAsc(quiz.id!!).toList().first()
            val opts = options.findAllByQuestionIds(listOf(q.id!!)).toList()
            FirstQuestion(
                id = q.id,
                correctKeys = opts.filter { it.isCorrect }.map { it.optionKey },
                anyWrongKey = opts.first { !it.isCorrect }.optionKey,
            )
        }

    @Test
    fun `correct answer awards points`() =
        runTest {
            val q = firstSeededQuestion()
            val room = fixtures.createRoom()
            val sessionId = UUID.randomUUID().toString()
            fixtures.joinViaRest(room.code, "Аня", sessionId)
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val player = fixtures.connectPlayer(room.code, "Аня", sessionId)
            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))
                player.awaitEvent(EventTypes.QUESTION_STARTED)
                player.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q.id.toString(), "selectedOptions" to q.correctKeys),
                    ),
                )
                val accepted = player.awaitEvent(EventTypes.ANSWER_ACCEPTED).payloadMap()
                assertEquals(q.id.toString(), accepted["questionId"])

                val result = org.awaitEvent(EventTypes.QUESTION_RESULT).payloadMap()

                @Suppress("UNCHECKED_CAST")
                val players = result["ranking"] as List<Map<String, Any>>
                assertEquals(1, players.size)
                val score = (players[0]["score"] as Number).toInt()
                assert(score > 0) { "score must be positive but was $score" }
            } finally {
                org.close()
                player.close()
            }
        }

    @Test
    fun `incorrect answer earns zero`() =
        runTest {
            val q = firstSeededQuestion()
            val room = fixtures.createRoom()
            val sessionId = UUID.randomUUID().toString()
            fixtures.joinViaRest(room.code, "Бен", sessionId)
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val player = fixtures.connectPlayer(room.code, "Бен", sessionId)
            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))
                player.awaitEvent(EventTypes.QUESTION_STARTED)
                player.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q.id.toString(), "selectedOptions" to listOf(q.anyWrongKey)),
                    ),
                )
                player.awaitEvent(EventTypes.ANSWER_ACCEPTED)

                val result = org.awaitEvent(EventTypes.QUESTION_RESULT).payloadMap()

                @Suppress("UNCHECKED_CAST")
                val players = result["ranking"] as List<Map<String, Any>>
                val score = (players[0]["score"] as Number).toInt()
                assertEquals(0, score)
            } finally {
                org.close()
                player.close()
            }
        }

    @Test
    fun `double submit for same question is rejected`() =
        runTest {
            val q = firstSeededQuestion()
            val room = fixtures.createRoom()
            val sessionId = UUID.randomUUID().toString()
            fixtures.joinViaRest(room.code, "Дима", sessionId)
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val player = fixtures.connectPlayer(room.code, "Дима", sessionId)
            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))
                player.awaitEvent(EventTypes.QUESTION_STARTED)
                player.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q.id.toString(), "selectedOptions" to q.correctKeys),
                    ),
                )
                player.awaitEvent(EventTypes.ANSWER_ACCEPTED)

                player.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q.id.toString(), "selectedOptions" to q.correctKeys),
                    ),
                )
                val rejected = player.awaitEvent(EventTypes.ANSWER_REJECTED).payloadMap()
                assertNotNull(rejected["reason"])
            } finally {
                org.close()
                player.close()
            }
        }
}
