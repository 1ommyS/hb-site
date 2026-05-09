package com.example.hbsite.ws

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class RoomEventBus {
    private val sinks = ConcurrentHashMap<UUID, Sinks.Many<WsEnvelope>>()

    private fun sinkFor(roomId: UUID): Sinks.Many<WsEnvelope> =
        sinks.computeIfAbsent(roomId) {
            Sinks.many().multicast().directBestEffort()
        }

    fun emit(roomId: UUID, event: WsEnvelope) {
        sinkFor(roomId).tryEmitNext(event)
    }

    fun flux(roomId: UUID): Flux<WsEnvelope> = sinkFor(roomId).asFlux()

    fun close(roomId: UUID) {
        sinks.remove(roomId)?.tryEmitComplete()
    }
}
