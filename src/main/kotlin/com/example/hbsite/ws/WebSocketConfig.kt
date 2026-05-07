package com.example.hbsite.ws

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig {
    @Bean
    fun wsHandlerMapping(handler: GameWebSocketHandler): HandlerMapping {
        val mapping = SimpleUrlHandlerMapping()
        mapping.urlMap = mapOf("/ws" to handler)
        mapping.order = Ordered.HIGHEST_PRECEDENCE
        return mapping
    }

    @Bean
    fun handlerAdapter(): WebSocketHandlerAdapter = WebSocketHandlerAdapter()
}
