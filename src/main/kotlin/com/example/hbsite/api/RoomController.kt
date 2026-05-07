package com.example.hbsite.api

import com.example.hbsite.repo.AnswerRepository
import com.example.hbsite.repo.OptionRepository
import com.example.hbsite.repo.QuestionRepository
import com.example.hbsite.service.RoomService
import com.example.hbsite.service.StatsService
import com.example.hbsite.ws.EventTypes
import com.example.hbsite.ws.PlayerDto
import com.example.hbsite.ws.PlayerJoinedPayload
import com.example.hbsite.ws.RoomEventBus
import com.example.hbsite.ws.WsEnvelope
import kotlinx.coroutines.flow.toList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/api/rooms")
class RoomController(
    private val roomService: RoomService,
    private val statsService: StatsService,
    private val questions: QuestionRepository,
    private val options: OptionRepository,
    private val answers: AnswerRepository,
    private val bus: RoomEventBus,
) {
    @PostMapping
    suspend fun create(exchange: ServerWebExchange): ResponseEntity<CreateRoomResponse> {
        val created = roomService.createRoom()
        val baseUrl = baseUrl(exchange)
        val wsScheme = if (baseUrl.startsWith("https")) "wss" else "ws"
        val host = exchange.request.uri.authority
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreateRoomResponse(
                roomId = created.room.id!!,
                code = created.room.code,
                organizerToken = created.room.organizerToken,
                quizTitle = created.quiz.title,
                totalQuestions = created.totalQuestions,
                joinUrl = "$baseUrl/?code=${created.room.code}",
                websocketUrl = "$wsScheme://$host/ws",
            ),
        )
    }

    @GetMapping("/{code}")
    suspend fun get(@PathVariable code: String): RoomInfoResponse {
        val room = roomService.findRoomByCode(code)
        val players = roomService.listPlayers(room.id!!).map { PlayerDto(it.id!!, it.name, it.score) }
        val total = questions.countByQuizId(room.quizId).toInt()
        return RoomInfoResponse(
            roomId = room.id,
            code = room.code,
            status = room.status,
            totalQuestions = total,
            currentQuestionIndex = room.currentQuestionIndex,
            players = players,
            createdAt = room.createdAt,
            startedAt = room.startedAt,
            finishedAt = room.finishedAt,
            annoyingModeEnabled = room.annoyingModeEnabled,
            manualMode = room.manualMode,
        )
    }

    @PostMapping("/{code}/players")
    suspend fun join(
        @PathVariable code: String,
        @RequestBody body: JoinRoomRequest,
    ): JoinRoomResponse {
        val joined = roomService.joinRoom(code, body.name, body.sessionId)
        val player = PlayerDto(joined.player.id!!, joined.player.name, joined.player.score)
        if (!joined.isReconnect) {
            val all = roomService.listPlayers(joined.room.id!!).map { PlayerDto(it.id!!, it.name, it.score) }
            bus.emit(
                joined.room.id,
                WsEnvelope(
                    EventTypes.PLAYER_JOINED,
                    PlayerJoinedPayload(roomId = joined.room.id, player = player, players = all),
                ),
            )
        }
        return JoinRoomResponse(
            roomId = joined.room.id!!,
            player = player,
            isReconnect = joined.isReconnect,
        )
    }

    @GetMapping("/{code}/results")
    suspend fun results(@PathVariable code: String) =
        statsService.buildFinalRanking(roomService.findRoomByCode(code))

    @GetMapping("/{code}/stats")
    suspend fun stats(@PathVariable code: String): StatsResponse {
        val room = roomService.findRoomByCode(code)
        val qs = questions.findAllByQuizIdOrderByOrderNumberAsc(room.quizId).toList()
        val opts = options.findAllByQuestionIds(qs.mapNotNull { it.id }).toList().groupBy { it.questionId }
        val ans = answers.findAllByRoomId(room.id!!).toList()
        val ansByQ = ans.groupBy { it.questionId }

        val perQuestion =
            qs.mapIndexed { i, q ->
                val qOpts = opts[q.id!!] ?: emptyList()
                val dist = LinkedHashMap<String, Int>()
                qOpts.forEach { dist[it.optionKey] = 0 }
                ansByQ[q.id]
                    ?.flatMap { it.selectedOptions.split(",") }
                    ?.filter { it.isNotBlank() }
                    ?.forEach { dist[it] = (dist[it] ?: 0) + 1 }
                QuestionDistributionDto(
                    questionId = q.id,
                    questionNumber = i + 1,
                    text = q.text,
                    correctOptions = qOpts.filter { it.isCorrect }.map { it.optionKey },
                    distribution = dist,
                )
            }

        val wrongTally =
            ans
                .asSequence()
                .filter { !it.isCorrect }
                .flatMap { it.selectedOptions.split(",").asSequence() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
        val mostPopularWrong = wrongTally.maxByOrNull { it.value }?.key

        val hardest =
            ansByQ
                .mapValues { (_, v) -> v.count { it.isCorrect }.toDouble() / v.size.coerceAtLeast(1) }
                .minByOrNull { it.value }
                ?.key

        val unanimousCorrect =
            ansByQ.entries.firstOrNull { (_, v) -> v.isNotEmpty() && v.all { it.isCorrect } }?.key
        val confusing =
            ansByQ.entries.firstOrNull { (_, v) -> v.isNotEmpty() && v.none { it.isCorrect } }?.key

        return StatsResponse(
            roomId = room.id,
            mostPopularWrong = mostPopularWrong,
            hardestQuestionId = hardest,
            unanimouslyCorrectQuestionId = unanimousCorrect,
            confusingQuestionId = confusing,
            perQuestion = perQuestion,
        )
    }

    private fun baseUrl(exchange: ServerWebExchange): String {
        val req = exchange.request
        val scheme = req.uri.scheme ?: "http"
        val authority = req.uri.authority ?: req.headers.host?.toString() ?: "localhost:8080"
        return "$scheme://$authority"
    }
}
