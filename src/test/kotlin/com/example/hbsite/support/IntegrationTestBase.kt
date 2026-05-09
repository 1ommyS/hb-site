package com.example.hbsite.support

import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
abstract class IntegrationTestBase {
    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var connectionFactory: ConnectionFactory

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var mapper: JsonMapper

    protected val baseUrl: String get() = "http://localhost:$port"
    protected val wsUrl: String get() = "ws://localhost:$port/ws"

    protected val fixtures: Fixtures by lazy { Fixtures(webTestClient, mapper, wsUrl) }

    @AfterEach
    fun cleanRuntimeData() {
        // Сидированный квиз (V2) НЕ трогаем — он нужен для всех тестов.
        val client = DatabaseClient.create(connectionFactory)
        Mono
            .from(client.sql("TRUNCATE answers, players, rooms RESTART IDENTITY CASCADE").then())
            .block()
    }

    companion object {
        @JvmStatic
        protected val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("hbsite_test")
                .withUsername("hbsite")
                .withPassword("hbsite")
                .withReuse(true)
                .also { it.start() }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            val host = postgres.host
            val port = postgres.firstMappedPort
            val db = postgres.databaseName
            registry.add("spring.r2dbc.url") { "r2dbc:postgresql://$host:$port/$db" }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.liquibase.url") { postgres.jdbcUrl }
            registry.add("spring.liquibase.user") { postgres.username }
            registry.add("spring.liquibase.password") { postgres.password }
        }
    }
}
