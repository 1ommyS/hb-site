package com.example.hbsite.service

class RoomNotFoundException(val code: String) : RuntimeException("Комната не найдена: $code")

class InvalidPlayerNameException(message: String) : RuntimeException(message)

class IllegalGameStateException(message: String) : RuntimeException(message)

class NotOrganizerException : RuntimeException("Действие доступно только организатору")

class PlayerNotFoundException : RuntimeException("Игрок не найден в комнате")
