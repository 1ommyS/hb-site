package com.example.hbsite.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "quiz")
data class QuizProperties(
    val defaultQuestionDurationSeconds: Long = 30,
    val warningSecondsBeforeEnd: Long = 10,
    val autoModePauseSeconds: Long = 7,
    val manualMode: Boolean = true,
    val defaultQuizSlug: String = "hb-default-quiz-vanya",
)
