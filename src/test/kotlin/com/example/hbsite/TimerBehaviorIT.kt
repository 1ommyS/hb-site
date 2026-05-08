package com.example.hbsite

import com.example.hbsite.support.IntegrationTestBase
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

/**
 * Поведение таймера. В application-test.yaml duration=2s, autoModePauseSeconds=1s.
 */
class TimerBehaviorIT : IntegrationTestBase() {
    @Test
    fun `question auto finishes when timer expires`() =
        runTest {
            val room = fixtures.createRoom()
            val org = fixtures.connectOrganizer(room.code, room.organizerToken)
            val sessionId = UUID.randomUUID().toString()
            fixtures.joinViaRest(room.code, "Молчун", sessionId)
            val silent = fixtures.connectPlayer(room.code, "Молчун", sessionId)
            try {
                org.send(WsEnvelope(EventTypes.START_QUIZ, emptyMap<String, Any>()))
                silent.awaitEvent(EventTypes.QUESTION_STARTED)
                // Никто не отвечает; через ~2с приходит QUESTION_FINISHED по таймеру.
                org.awaitEvent(EventTypes.QUESTION_FINISHED, Duration.ofSeconds(5))
                org.awaitEvent(EventTypes.QUESTION_RESULT, Duration.ofSeconds(2))
            } finally {
                org.close()
                silent.close()
            }
        }
}
