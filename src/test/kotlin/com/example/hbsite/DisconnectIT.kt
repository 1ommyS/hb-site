package com.example.hbsite

import com.example.hbsite.support.IntegrationTestBase
import com.example.hbsite.support.payloadMap
import com.example.hbsite.ws.EventTypes
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Проверяем PLAYER_LEFT при разрыве WS-соединения игрока.
 */
class DisconnectIT : IntegrationTestBase() {
    @Test
    fun `player disconnect emits PLAYER_LEFT to organizer`() =
        runTest {
            val room = fixtures.createRoom()
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val sessionId = UUID.randomUUID().toString()
            fixtures.joinViaRest(room.code, "Лёша", sessionId)
            val player = fixtures.connectPlayer(room.code, "Лёша", sessionId)
            // Дождёмся PLAYER_JOINED, чтобы он не «случайно» был ниже в очереди.
            org.awaitEvent(EventTypes.PLAYER_JOINED)

            player.close()

            val left = org.awaitEvent(EventTypes.PLAYER_LEFT, java.time.Duration.ofSeconds(3))
            val payload = left.payloadMap()

            @Suppress("UNCHECKED_CAST")
            val all = payload["players"] as List<Map<String, Any>>
            // Игрока в БД мы не удаляем — он остаётся в списке (может вернуться).
            assertEquals(1, all.size)

            org.close()
        }
}
