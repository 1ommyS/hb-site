package com.example.hbsite.support

import com.example.hbsite.api.CreateRoomResponse
import com.example.hbsite.api.JoinRoomRequest
import com.example.hbsite.api.JoinRoomResponse
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.WsEnvelope
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

/**
 * Хелперы для интеграционных тестов: типовые операции через REST/WS,
 * чтобы каждый тест-метод оставался коротким и читаемым.
 */
class Fixtures(
    private val webTestClient: WebTestClient,
    private val mapper: JsonMapper,
    private val wsUrl: String,
) {
    fun createRoom(): CreateRoomResponse =
        webTestClient
            .post()
            .uri("/api/rooms")
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(CreateRoomResponse::class.java)
            .returnResult()
            .responseBody!!

    fun joinViaRest(
        code: String,
        name: String,
        sessionId: String = UUID.randomUUID().toString(),
    ): JoinRoomResponse =
        webTestClient
            .post()
            .uri("/api/rooms/{code}/players", code)
            .bodyValue(JoinRoomRequest(name = name, sessionId = sessionId))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(JoinRoomResponse::class.java)
            .returnResult()
            .responseBody!!

    suspend fun connectOrganizer(code: String, organizerToken: String): WsTestClient =
        WsTestClient(mapper, wsUrl)
            .connect()
            .also {
                it.send(
                    WsEnvelope(
                        EventTypes.JOIN_ROOM,
                        mapOf(
                            "roomCode" to code,
                            "organizerToken" to organizerToken,
                        ),
                    ),
                )
                it.awaitEvent(EventTypes.ROOM_STATE_SYNC)
            }

    suspend fun connectPlayer(
        code: String,
        name: String,
        sessionId: String = UUID.randomUUID().toString(),
    ): WsTestClient =
        WsTestClient(mapper, wsUrl)
            .connect()
            .also {
                it.send(
                    WsEnvelope(
                        EventTypes.JOIN_ROOM,
                        mapOf(
                            "roomCode" to code,
                            "sessionId" to sessionId,
                            "name" to name,
                        ),
                    ),
                )
                it.awaitEvent(EventTypes.ROOM_STATE_SYNC)
            }
}

@Suppress("UNCHECKED_CAST")
fun WsEnvelope.payloadMap(): Map<String, Any?> = payload as Map<String, Any?>
