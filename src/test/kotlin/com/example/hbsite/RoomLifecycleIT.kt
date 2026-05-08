package com.example.hbsite

import com.example.hbsite.api.RoomInfoResponse
import com.example.hbsite.support.IntegrationTestBase
import com.example.hbsite.support.payloadMap
import com.example.hbsite.ws.EventTypes
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Создание/получение комнаты и REST-вход игроков. Ключевое:
 *  - `POST /players` теперь публикует `PLAYER_JOINED` (через RoomService.joinRoom)
 *  - повторный вызов с тем же sessionId — `isReconnect=true`, без повторной эмиссии
 *  - дубликаты имён получают суффикс ` #N`
 */
class RoomLifecycleIT : IntegrationTestBase() {
    @Test
    fun `create room returns code and organizer token`() {
        val room = fixtures.createRoom()
        assertTrue(room.code.matches(Regex("^[A-Z0-9]{4,8}$")), "code: ${room.code}")
        assertNotNull(UUID.fromString(room.organizerToken))
        assertTrue(room.totalQuestions > 0)
        assertTrue(room.joinUrl.contains("?code=${room.code}"))
        assertTrue(room.websocketUrl.startsWith("ws://"))
    }

    @Test
    fun `get returns initial WAITING state with no players`() {
        val room = fixtures.createRoom()
        val info =
            webTestClient
                .get()
                .uri("/api/rooms/{code}", room.code)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody(RoomInfoResponse::class.java)
                .returnResult()
                .responseBody!!
        assertEquals("WAITING", info.status)
        assertEquals(-1, info.currentQuestionIndex)
        assertTrue(info.players.isEmpty())
    }

    @Test
    fun `join via REST emits PLAYER_JOINED to subscribed organizer`() =
        runTest {
            val room = fixtures.createRoom()
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            try {
                fixtures.joinViaRest(room.code, name = "Алёна")
                val event = org.awaitEvent(EventTypes.PLAYER_JOINED)
                val payload = event.payloadMap()

                @Suppress("UNCHECKED_CAST")
                val player = payload["player"] as Map<String, Any>
                assertEquals("Алёна", player["name"])

                @Suppress("UNCHECKED_CAST")
                val all = payload["players"] as List<Map<String, Any>>
                assertEquals(1, all.size)
            } finally {
                org.close()
            }
        }

    @Test
    fun `reconnect with same sessionId is idempotent and emits nothing`() =
        runTest {
            val room = fixtures.createRoom()
            val sessionId = UUID.randomUUID().toString()
            val first = fixtures.joinViaRest(room.code, "Игорь", sessionId)
            assertFalse(first.isReconnect)

            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            try {
                val second = fixtures.joinViaRest(room.code, "Игорь", sessionId)
                assertTrue(second.isReconnect)
                assertEquals(first.player.id, second.player.id)
                // Шина ничего не должна прислать на reconnect — даём время.
                runCatching { org.awaitEvent(EventTypes.PLAYER_JOINED, java.time.Duration.ofMillis(800)) }
                    .onSuccess { error("PLAYER_JOINED не должен эмититься на reconnect") }
            } finally {
                org.close()
            }
        }

    @Test
    fun `duplicate name gets numeric suffix`() {
        val room = fixtures.createRoom()
        fixtures.joinViaRest(room.code, "Иван")
        val second = fixtures.joinViaRest(room.code, "Иван")
        assertEquals("Иван #2", second.player.name)
    }
}
