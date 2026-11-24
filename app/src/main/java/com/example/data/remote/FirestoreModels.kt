package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class UserLearningProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val preferredLevel: String = "A1",
    val streakDays: Long = 0L,
    val longestStreak: Long = 0L,
    val lastActiveDay: String = "",
    val totalStudyMinutes: Long = 0L,
    val wordsMasteredCount: Long = 0L,
    val wordsLearningCount: Long = 0L,
    val quizzesCompleted: Long = 0L,
    val totalQuizScorePct: Long = 0L,
    val storiesCompleted: Long = 0L,
    val conversationsCompleted: Long = 0L,
    val grammarCompleted: Long = 0L,
    val verbsPracticed: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(100).ifBlank { "Learner" },
        "preferredLevel" to normalizeCefr(preferredLevel),
        "streakDays" to streakDays.coerceIn(0L, 100_000L),
        "longestStreak" to longestStreak.coerceIn(0L, 100_000L),
        "lastActiveDay" to lastActiveDay.take(20),
        "totalStudyMinutes" to totalStudyMinutes.coerceAtLeast(0L),
        "wordsMasteredCount" to wordsMasteredCount.coerceIn(0L, 10_000L),
        "wordsLearningCount" to wordsLearningCount.coerceIn(0L, 10_000L),
        "quizzesCompleted" to quizzesCompleted.coerceAtLeast(0L),
        "totalQuizScorePct" to totalQuizScorePct.coerceAtLeast(0L),
        "storiesCompleted" to storiesCompleted.coerceIn(0L, 1_000L),
        "conversationsCompleted" to conversationsCompleted.coerceIn(0L, 1_000L),
        "grammarCompleted" to grammarCompleted.coerceIn(0L, 500L),
        "verbsPracticed" to verbsPracticed.coerceIn(0L, 5_000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class WordProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val headword: String = "",
    val cefrLevel: String = "A1",
    val favorite: Boolean = false,
    val masteryState: String = "NEW",
    val easeFactor: Double = 2.5,
    val intervalDays: Long = 0L,
    val repetitions: Long = 0L,
    val nextReviewAtMs: Long = 0L,
    val correctCount: Long = 0L,
    val attemptCount: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "headword" to headword.take(100).ifBlank { "word" },
        "cefrLevel" to normalizeCefr(cefrLevel),
        "favorite" to favorite,
        "masteryState" to normalizeMastery(masteryState),
        "easeFactor" to easeFactor.coerceIn(1.3, 5.0),
        "intervalDays" to intervalDays.coerceIn(0L, 3650L),
        "repetitions" to repetitions.coerceIn(0L, 10_000L),
        "nextReviewAtMs" to nextReviewAtMs.coerceAtLeast(0L),
        "correctCount" to correctCount.coerceAtLeast(0L),
        "attemptCount" to attemptCount.coerceAtLeast(0L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = "VOCABULARY",
    val mode: String = "MCQ",
    val difficulty: String = "BEGINNER",
    val cefrLevel: String = "ALL",
    val score: Long = 0L,
    val totalQuestions: Long = 1L,
    val durationSeconds: Long = 0L,
    val topicTitle: String = "",
    val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val safeTotal = totalQuestions.coerceIn(1L, 100L)
        val safeScore = score.coerceIn(0L, safeTotal)
        return mapOf(
            "userId" to userId,
            "category" to if (category in setOf("VOCABULARY", "GRAMMAR", "STORY", "CONVERSATION")) category else "VOCABULARY",
            "mode" to mode.take(50).ifBlank { "MCQ" },
            "difficulty" to if (difficulty in setOf("BEGINNER", "INTERMEDIATE", "ADVANCED")) difficulty else "BEGINNER",
            "cefrLevel" to if (cefrLevel in setOf("A1", "A2", "B1", "B2", "C1", "ALL")) cefrLevel else "ALL",
            "score" to safeScore,
            "totalQuestions" to safeTotal,
            "durationSeconds" to durationSeconds.coerceIn(0L, 86_400L),
            "topicTitle" to topicTitle.take(160).ifBlank { "English Quiz" },
            "createdAt" to FieldValue.serverTimestamp()
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ContentProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val contentType: String = "STORY",
    val cefrLevel: String = "A1",
    val completed: Boolean = false,
    val bestScorePct: Long = 0L,
    val minutesSpent: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "contentType" to if (contentType in setOf("GRAMMAR", "STORY", "CONVERSATION", "VERB")) contentType else "STORY",
        "cefrLevel" to normalizeCefr(cefrLevel),
        "completed" to completed,
        "bestScorePct" to bestScorePct.coerceIn(0L, 100L),
        "minutesSpent" to minutesSpent.coerceIn(0L, 10_000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class DailyActivity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val minutesStudied: Long = 0L,
    val wordsReviewed: Long = 0L,
    val quizzesTaken: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "dateKey" to dateKey.take(16),
        "minutesStudied" to minutesStudied.coerceIn(0L, 1440L),
        "wordsReviewed" to wordsReviewed.coerceIn(0L, 50_000L),
        "quizzesTaken" to quizzesTaken.coerceIn(0L, 10_000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class Sm2Quality(val q: Int) {
    AGAIN(1),
    HARD(3),
    GOOD(4),
    EASY(5)
}

object Sm2Scheduler {
    fun schedule(
        current: WordProgress?,
        userId: String,
        headword: String,
        cefrLevel: String,
        quality: Sm2Quality,
        nowMs: Long = System.currentTimeMillis()
    ): WordProgress {
        val prevEf = current?.easeFactor ?: 2.5
        val prevInterval = current?.intervalDays ?: 0L
        val prevReps = current?.repetitions ?: 0L
        val prevCorrect = current?.correctCount ?: 0L
        val prevAttempts = current?.attemptCount ?: 0L
        val fav = current?.favorite ?: false

        val q = quality.q
        val newEf = (prevEf + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))).coerceIn(1.3, 5.0)
        val newReps: Long
        val newInterval: Long
        if (q < 3) {
            newReps = 0L
            newInterval = 1L
        } else {
            newReps = prevReps + 1L
            newInterval = when (newReps) {
                1L -> 1L
                2L -> 6L
                else -> (prevInterval * newEf).toLong().coerceIn(1L, 3650L)
            }
        }
        val mastery = when {
            newReps >= 4L && newInterval >= 14L -> "MASTERED"
            newReps >= 2L -> "REVIEWING"
            else -> "LEARNING"
        }
        val nextReview = nowMs + newInterval * 86_400_000L
        return WordProgress(
            id = sanitizeFirestoreId(current?.id?.ifBlank { headword } ?: headword),
            userId = userId,
            headword = headword,
            cefrLevel = normalizeCefr(cefrLevel),
            favorite = fav,
            masteryState = mastery,
            easeFactor = newEf,
            intervalDays = newInterval,
            repetitions = newReps,
            nextReviewAtMs = nextReview,
            correctCount = prevCorrect + if (q >= 3) 1L else 0L,
            attemptCount = prevAttempts + 1L,
            createdAt = current?.createdAt,
            updatedAt = current?.updatedAt
        )
    }
}

fun normalizeCefr(level: String): String =
    if (level in setOf("A1", "A2", "B1", "B2", "C1")) level else "A1"

fun normalizeMastery(state: String): String =
    if (state in setOf("NEW", "LEARNING", "REVIEWING", "MASTERED")) state else "NEW"

fun sanitizeFirestoreId(raw: String): String =
    raw.lowercase()
        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        .trim('_')
        .take(120)
        .ifBlank { "item" }
