package com.example.hbsite.service

import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class CodeGenerator {
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(length: Int = 5): String =
        (1..length)
            .map { alphabet[Random.nextInt(alphabet.length)] }
            .joinToString("")
}

@Component
class NameSanitizer {
    private val tagRegex = Regex("<[^>]*>")
    private val whitespaceRegex = Regex("\\s+")

    fun sanitize(raw: String): String =
        raw.replace(tagRegex, "")
            .replace(whitespaceRegex, " ")
            .trim()
            .take(40)
}
