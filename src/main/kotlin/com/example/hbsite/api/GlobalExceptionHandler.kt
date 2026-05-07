package com.example.hbsite.api

import com.example.hbsite.service.IllegalGameStateException
import com.example.hbsite.service.InvalidPlayerNameException
import com.example.hbsite.service.NotOrganizerException
import com.example.hbsite.service.PlayerNotFoundException
import com.example.hbsite.service.RoomNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(RoomNotFoundException::class)
    fun roomNotFound(e: RoomNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "Not found", "ROOM_NOT_FOUND"))

    @ExceptionHandler(PlayerNotFoundException::class)
    fun playerNotFound(e: PlayerNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(e.message ?: "Not found", "PLAYER_NOT_FOUND"))

    @ExceptionHandler(InvalidPlayerNameException::class)
    fun invalidName(e: InvalidPlayerNameException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Bad request", "INVALID_NAME"))

    @ExceptionHandler(IllegalGameStateException::class)
    fun illegalState(e: IllegalGameStateException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(e.message ?: "Conflict", "GAME_STATE"))

    @ExceptionHandler(NotOrganizerException::class)
    fun notOrganizer(e: NotOrganizerException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse(e.message ?: "Forbidden", "NOT_ORGANIZER"))

    @ExceptionHandler(IllegalArgumentException::class)
    fun illegalArg(e: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(ErrorResponse(e.message ?: "Bad request", "BAD_REQUEST"))
}
