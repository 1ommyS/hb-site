package com.example.hbsite

import com.example.hbsite.support.IntegrationTestBase
import com.example.hbsite.support.payloadMap
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Базовый smoke happy-path: создать комнату, организатор подключается по WS,
 * запускает квиз, получает QUIZ_STARTED + QUESTION_STARTED с непустыми вариантами.
 *
 * Если этот тест валится — это ровно тот продовый баг "при старте квиза
 * ничего не меняется, не появляются вопросы".
 */
class SmokeIT : IntegrationTestBase() {
    @Test
    fun `organizer starts quiz and receives QUESTION_STARTED with options`() =
        runTest {
            val room = fixtures.createRoom()
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))

                org.awaitEvent(EventTypes.QUIZ_STARTED)
                val question = org.awaitEvent(EventTypes.QUESTION_STARTED)
                val payload = question.payloadMap()

                @Suppress("UNCHECKED_CAST")
                val options = payload["options"] as List<Map<String, Any>>
                assertTrue(options.isNotEmpty(), "options must be non-empty")
                assertEquals(1, payload["questionNumber"])
            } finally {
                org.close()
            }
        }
}
