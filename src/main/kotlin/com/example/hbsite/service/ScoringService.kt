package com.example.hbsite.service

import com.example.hbsite.domain.OptionEntity
import com.example.hbsite.domain.Question
import org.springframework.stereotype.Service

data class ScoringResult(
    val isCorrect: Boolean,
    val pointsEarned: Int,
)

@Service
class ScoringService {
    fun score(
        question: Question,
        options: List<OptionEntity>,
        selectedKeys: Collection<String>,
    ): ScoringResult {
        if (selectedKeys.isEmpty()) return ScoringResult(false, 0)
        val correct = options.filter { it.isCorrect }.map { it.optionKey }.toSet()
        val selected = selectedKeys.toSet()
        val isCorrect = correct == selected
        return ScoringResult(isCorrect, if (isCorrect) question.points else 0)
    }
}
