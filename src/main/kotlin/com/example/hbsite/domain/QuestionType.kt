package com.example.hbsite.domain

enum class QuestionType(
    val raw: String,
) {
    SINGLE("single"),
    MULTIPLE("multiple"),
    ;

    companion object {
        fun fromRaw(raw: String): QuestionType =
            entries.firstOrNull { it.raw == raw }
                ?: throw IllegalArgumentException("Unknown question type: $raw")
    }
}
