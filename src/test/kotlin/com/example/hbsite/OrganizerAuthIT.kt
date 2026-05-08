package com.example.hbsite

import com.example.hbsite.support.IntegrationTestBase
import com.example.hbsite.support.WsTestClient
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * Чужой organizerToken должен вернуть ERROR на JOIN_ROOM и не давать
 * управлять квизом.
 */
class OrganizerAuthIT : IntegrationTestBase() {
    @Test
    fun `wrong organizer token rejected on join`() =
        runTest {
            val room = fixtures.createRoom()
            val ws = WsTestClient(mapper, wsUrl).connect()
            try {
                ws.send(
                    WsEnvelope(
                        EventTypes.JOIN_ROOM,
                        mapOf(
                            "roomCode" to room.code,
                            "organizerToken" to "totally-not-the-token",
                        ),
                    ),
                )
                val err = ws.awaitEvent(EventTypes.ERROR)
                assertNotNull(err.payload)
            } finally {
                ws.close()
            }
        }

    @Test
    fun `command before join is rejected with ERROR not disconnect`() =
        runTest {
            val ws = WsTestClient(mapper, wsUrl).connect()
            try {
                ws.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))
                val err = ws.awaitEvent(EventTypes.ERROR)
                assertNotNull(err.payload)
                // Соединение живо: PING отвечает.
                ws.send(WsEnvelope(EventTypes.PING, null))
                ws.awaitEvent(EventTypes.PONG)
            } finally {
                ws.close()
            }
        }
}
