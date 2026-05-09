package com.example.hbsite

import com.example.hbsite.support.IntegrationTestBase
import com.example.hbsite.support.payloadMap
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Главный e2e сценарий: организатор + игроки по WS, прохождение нескольких
 * вопросов с ручным NEXT_QUESTION, финал с QUIZ_FINISHED и финальным рангом.
 */
class QuizFlowIT : IntegrationTestBase() {
    @Test
    fun `full quiz flow with two players in manual mode`() =
        runTest {
            val room = fixtures.createRoom()
            assertEquals(40, room.totalQuestions)
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val sessionA = UUID.randomUUID().toString()
            val sessionB = UUID.randomUUID().toString()
            // Сначала создаём игроков через REST (как делает фронт), потом WS-сессии.
            fixtures.joinViaRest(room.code, "Аня", sessionA)
            fixtures.joinViaRest(room.code, "Бен", sessionB)
            val playerA = fixtures.connectPlayer(room.code, "Аня", sessionA)
            val playerB = fixtures.connectPlayer(room.code, "Бен", sessionB)

            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))

                listOf(org, playerA, playerB).forEach { it.awaitEvent(EventTypes.QUIZ_STARTED) }
                val q1 = playerA.awaitEvent(EventTypes.QUESTION_STARTED).payloadMap()

                @Suppress("UNCHECKED_CAST")
                val opts1 = q1["options"] as List<Map<String, Any>>
                val firstOption = opts1.first()["id"] as String
                val q1Id = q1["questionId"] as String

                // Оба отвечают — после второго ответа QUESTION_FINISHED + QUESTION_RESULT.
                playerA.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q1Id, "selectedOptions" to listOf(firstOption)),
                    ),
                )
                playerB.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q1Id, "selectedOptions" to listOf(firstOption)),
                    ),
                )
                playerA.awaitEvent(EventTypes.ANSWER_ACCEPTED)
                playerB.awaitEvent(EventTypes.ANSWER_ACCEPTED)
                org.awaitEvent(EventTypes.QUESTION_FINISHED)
                val result = org.awaitEvent(EventTypes.QUESTION_RESULT).payloadMap()
                assertEquals(q1Id, result["questionId"])

                // Организатор продвигает на следующий вопрос.
                org.send(WsEnvelope(EventTypes.NEXT_QUESTION, emptyMap<String, Any>()))
                val q2 = playerA.awaitEvent(EventTypes.QUESTION_STARTED).payloadMap()
                assertEquals(2, q2["questionNumber"])
                assertNotNull(q2["questionId"])

                // Не доигрываем все 40 вопросов — финишируем принудительно.
                org.send(WsEnvelope(EventTypes.FINISH_QUIZ, emptyMap<String, Any>()))
                val finished = org.awaitEvent(EventTypes.QUIZ_FINISHED).payloadMap()

                @Suppress("UNCHECKED_CAST")
                val ranking = finished["ranking"] as List<Map<String, Any>>
                assertEquals(2, ranking.size, "ranking must include both players")
                assertTrue(ranking.all { it["score"] != null })
            } finally {
                org.close()
                playerA.close()
                playerB.close()
            }
        }

    @Test
    fun `wrong answer finishes only current question not whole quiz`() =
        runTest {
            val room = fixtures.createRoom()
            assertEquals(40, room.totalQuestions)
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val sessionId = UUID.randomUUID().toString()
            fixtures.joinViaRest(room.code, "Игрок", sessionId)
            val player = fixtures.connectPlayer(room.code, "Игрок", sessionId)

            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))
                player.awaitEvent(EventTypes.QUIZ_STARTED)
                val q1 = player.awaitEvent(EventTypes.QUESTION_STARTED).payloadMap()
                val q1Id = q1["questionId"] as String

                player.send(
                    WsEnvelope(
                        EventTypes.SUBMIT_ANSWER,
                        mapOf("questionId" to q1Id, "selectedOptions" to listOf("D")),
                    ),
                )

                player.awaitEvent(EventTypes.ANSWER_ACCEPTED)
                val result = player.awaitEvent(EventTypes.QUESTION_RESULT).payloadMap()
                assertEquals(q1Id, result["questionId"])

                val unexpectedFinish =
                    withTimeoutOrNull(300) {
                        player.awaitEvent(EventTypes.QUIZ_FINISHED)
                    }
                assertNull(unexpectedFinish, "wrong answer must not finish a 40-question quiz")
            } finally {
                org.close()
                player.close()
            }
        }
}
