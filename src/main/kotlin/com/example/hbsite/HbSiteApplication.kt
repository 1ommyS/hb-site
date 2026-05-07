package com.example.hbsite

import com.example.hbsite.config.QuizProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(QuizProperties::class)
class HbSiteApplication

fun main(args: Array<String>) {
    runApplication<HbSiteApplication>(*args)
}
