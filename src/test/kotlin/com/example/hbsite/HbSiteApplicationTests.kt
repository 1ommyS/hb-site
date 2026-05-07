package com.example.hbsite

import com.example.hbsite.domain.OptionEntity
import com.example.hbsite.domain.Question
import com.example.hbsite.service.NameSanitizer
import com.example.hbsite.service.ScoringService
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HbSiteApplicationTests {
    private val sanitizer = NameSanitizer()
    private val scoring = ScoringService()

    @Test
    fun `name sanitizer strips html and trims`() {
        assertEquals("Иван", sanitizer.sanitize("  <b>Иван</b>  "))
        assertEquals("Леха Profi", sanitizer.sanitize("Леха   Profi"))
    }

    @Test
    fun `single choice scoring is correct only for full match`() {
        val qid = UUID.randomUUID()
        val q = Question(id = qid, quizId = UUID.randomUUID(), text = "?", type = "single", points = 2, orderNumber = 1)
        val opts =
            listOf(
                OptionEntity(questionId = qid, optionKey = "A", text = "a", isCorrect = false),
                OptionEntity(questionId = qid, optionKey = "B", text = "b", isCorrect = true),
            )
        val correct = scoring.score(q, opts, listOf("B"))
        val wrong = scoring.score(q, opts, listOf("A"))
        val empty = scoring.score(q, opts, emptyList())
        assertTrue(correct.isCorrect)
        assertEquals(2, correct.pointsEarned)
        assertFalse(wrong.isCorrect)
        assertEquals(0, wrong.pointsEarned)
        assertFalse(empty.isCorrect)
    }

    @Test
    fun `multiple choice requires exact set match`() {
        val qid = UUID.randomUUID()
        val q = Question(id = qid, quizId = UUID.randomUUID(), text = "?", type = "multiple", points = 3, orderNumber = 1)
        val opts =
            listOf(
                OptionEntity(questionId = qid, optionKey = "A", text = "a", isCorrect = true),
                OptionEntity(questionId = qid, optionKey = "B", text = "b", isCorrect = false),
                OptionEntity(questionId = qid, optionKey = "C", text = "c", isCorrect = true),
            )
        assertTrue(scoring.score(q, opts, listOf("A", "C")).isCorrect)
        assertFalse(scoring.score(q, opts, listOf("A")).isCorrect)
        assertFalse(scoring.score(q, opts, listOf("A", "B", "C")).isCorrect)
    }
}
