package com.example.hbsite.support

import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.future.await
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Тонкий WS-клиент для интеграционных тестов: подключается к /ws,
 * собирает все входящие сообщения в очередь, отдаёт API для отправки и ожидания.
 */
class WsTestClient(
    private val mapper: JsonMapper,
    private val url: String,
) {
    private val outbound: Sinks.Many<String> = Sinks.many().unicast().onBackpressureBuffer()
    private val received = ConcurrentLinkedQueue<WsEnvelope>()
    private val ready = CompletableFuture<Unit>()
    private val closed = CompletableFuture<Unit>()
    private var sessionDisposable: Disposable? = null

    suspend fun connect(): WsTestClient {
        val client = ReactorNettyWebSocketClient()
        val handler =
            WebSocketHandler { session: WebSocketSession ->
                ready.complete(Unit)

                val out =
                    session.send(
                        outbound.asFlux().map { session.textMessage(it) },
                    )

                val incoming =
                    session
                        .receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .doOnNext { raw -> received.add(mapper.readValue(raw, WsEnvelope::class.java)) }
                        .then()

                Mono.zip(out, incoming).then()
            }

        sessionDisposable =
            client
                .execute(URI.create(url), handler)
                .doFinally { closed.complete(Unit) }
                .subscribe()

        ready.await()
        return this
    }

    fun send(envelope: WsEnvelope) {
        val json = mapper.writeValueAsString(envelope)
        val result = outbound.tryEmitNext(json)
        check(result.isSuccess) { "WS send failed: $result" }
    }

    /** Ждёт первое событие указанного типа, появившееся в очереди ПОСЛЕ начала ожидания. */
    suspend fun awaitEvent(
        type: String,
        timeout: Duration = Duration.ofSeconds(5),
    ): WsEnvelope = awaitMatching(timeout) { it.type == type }

    suspend fun awaitMatching(
        timeout: Duration = Duration.ofSeconds(5),
        predicate: (WsEnvelope) -> Boolean,
    ): WsEnvelope {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (System.nanoTime() < deadline) {
            val match = received.firstOrNull(predicate)
            if (match != null) {
                received.remove(match)
                return match
            }
            kotlinx.coroutines.delay(20)
        }
        error("Timed out waiting for WS event matching predicate. Got: ${received.toList().map { it.type }}")
    }

    fun received(): List<WsEnvelope> = received.toList()

    fun close() {
        outbound.tryEmitComplete()
        sessionDisposable?.dispose()
    }
}
