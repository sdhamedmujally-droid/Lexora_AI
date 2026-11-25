package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserProgressRepository(
  private val db: FirebaseFirestore?,
private val auth: FirebaseAuth? = try { FirebaseApp.getInstance(); Firebase.auth } catch (_: Exception) { null }
) {
constructor(context: Context) : this(try { if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)) else null } catch (_: Exception) { null }, try { if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) Firebase.auth else null } catch (_: Exception) { null })

private fun requireFirestore(): FirebaseFirestore = db ?: throw IllegalStateException("Firebase is not initialized.")

    private fun requireUserId(): String {
        return auth?.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun todayDateKey(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date())
    }

    private fun yesterdayDateKey(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(System.currentTimeMillis() - 86_400_000L))
    }

    fun observeProfile(userId: String): Flow<UserLearningProfile?> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val docRef = requireFirestore().collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserLearningProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeWordProgressMap(userId: String): Flow<Map<String, WordProgress>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val colRef = requireFirestore().collection("users").document(userId).collection("word_progress")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        WordProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).associateBy { it.id }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val query = requireFirestore().collection("users").document(userId)
            .collection("quiz_attempts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuizAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/quiz_attempts")
                    }
                    throw error
                }
        )
    }

    fun observeContentProgress(userId: String): Flow<Map<String, ContentProgress>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val colRef = requireFirestore().collection("users").document(userId).collection("content_progress")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ContentProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).associateBy { it.id }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeDailyActivity(userId: String): Flow<List<DailyActivity>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val query = requireFirestore().collection("users").document(userId)
            .collection("daily_activity")
            .orderBy("dateKey", Query.Direction.DESCENDING)
            .limit(14)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DailyActivity::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth?.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/daily_activity")
                    }
                    throw error
                }
        )
    }

    suspend fun ensureProfileExists(displayName: String?): Result<Unit> {
        val uid = auth?.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val resolvedUid = requireUserId()
            val docRef = requireFirestore().collection("users").document(resolvedUid)
            val snap = docRef.get().await()
            val today = todayDateKey()
            val yesterday = yesterdayDateKey()
            if (!snap.exists()) {
                val cleanName = (displayName ?: auth?.currentUser?.displayName ?: "Learner")
                    .trim().take(100).ifBlank { "Learner" }
                val profile = UserLearningProfile(
                    userId = resolvedUid,
                    displayName = cleanName,
                    preferredLevel = "A1",
                    streakDays = 1L,
                    longestStreak = 1L,
                    lastActiveDay = today
                )
                docRef.set(profile.toCreateMap()).await()
            } else {
                val existing = snap.toObject(UserLearningProfile::class.java)
                if (existing != null && existing.lastActiveDay != today) {
                    val newStreak = when (existing.lastActiveDay) {
                        yesterday -> existing.streakDays + 1L
                        else -> 1L
                    }
                    val newLongest = maxOf(existing.longestStreak, newStreak)
                    docRef.update(
                        mapOf(
                            "streakDays" to newStreak,
                            "longestStreak" to newLongest,
                            "lastActiveDay" to today,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredLevel(level: String): Result<Unit> {
        val uid = auth?.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}"
        return try {
            val resolvedUid = requireUserId()
            requireFirestore().collection("users").document(resolvedUid).update(
                mapOf(
                    "preferredLevel" to normalizeCefr(level),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleWordFavorite(
        wordId: String,
        headword: String,
        cefrLevel: String,
        current: WordProgress?
    ): Result<Boolean> {
        val docId = sanitizeFirestoreId(wordId)
        val uid = auth?.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/word_progress/$docId"
        return try {
            val resolvedUid = requireUserId()
            val docRef = requireFirestore().collection("users").document(resolvedUid)
                .collection("word_progress").document(docId)
            val snap = docRef.get().await()
            val nextFav = !(current?.favorite ?: snap.getBoolean("favorite") ?: false)
            if (!snap.exists()) {
                val created = WordProgress(
                    id = docId,
                    userId = resolvedUid,
                    headword = headword,
                    cefrLevel = normalizeCefr(cefrLevel),
                    favorite = nextFav,
                    masteryState = "NEW"
                )
                docRef.set(created.toCreateMap()).await()
            } else {
                docRef.update(
                    mapOf(
                        "favorite" to nextFav,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(nextFav)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordFlashcardReview(
        wordId: String,
        headword: String,
        cefrLevel: String,
        current: WordProgress?,
        quality: Sm2Quality
    ): Result<WordProgress> {
        val docId = sanitizeFirestoreId(wordId)
        val uid = auth?.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/word_progress/$docId"
        return try {
            val resolvedUid = requireUserId()
            val docRef = requireFirestore().collection("users").document(resolvedUid)
                .collection("word_progress").document(docId)
            val snap = docRef.get().await()
            val existing = if (snap.exists()) {
                snap.toObject(WordProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else current

            val updated = Sm2Scheduler.schedule(
                current = existing?.copy(id = docId) ?: current,
                userId = resolvedUid,
                headword = headword,
                cefrLevel = cefrLevel,
                quality = quality
            )

            if (!snap.exists()) {
                docRef.set(updated.toCreateMap()).await()
            } else {
                docRef.update(
                    mapOf(
                        "cefrLevel" to updated.cefrLevel,
                        "masteryState" to updated.masteryState,
                        "easeFactor" to updated.easeFactor,
                        "intervalDays" to updated.intervalDays,
                        "repetitions" to updated.repetitions,
                        "nextReviewAtMs" to updated.nextReviewAtMs,
                        "correctCount" to updated.correctCount,
                        "attemptCount" to updated.attemptCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val wasMastered = existing?.masteryState == "MASTERED"
            val isNowMastered = updated.masteryState == "MASTERED"
            val masteredDelta = when {
                !wasMastered && isNowMastered -> 1L
                wasMastered && !isNowMastered -> -1L
                else -> 0L
            }
            val wasLearning = existing != null && existing.masteryState in setOf("LEARNING", "REVIEWING")
            val isNowLearning = updated.masteryState in setOf("LEARNING", "REVIEWING")
            val learningDelta = when {
                !wasLearning && isNowLearning -> 1L
                wasLearning && !isNowLearning -> -1L
                else -> 0L
            }

            bumpActivityAndProfile(
                userId = resolvedUid,
                minutesDelta = 1L,
                wordsReviewedDelta = 1L,
                quizzesDelta = 0L,
                masteredDelta = masteredDelta,
                learningDelta = learningDelta
            )

            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun recordQuizAttempt(
        category: String,
        mode: String,
        difficulty: String,
        cefrLevel: String,
        score: Int,
        totalQuestions: Int,
        durationSeconds: Int,
        topicTitle: String
    ): Result<Unit> {
        val uid = auth?.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/quiz_attempts"
        return try {
            val resolvedUid = requireUserId()
            val safeTotal = totalQuestions.coerceIn(1, 100)
            val safeScore = score.coerceIn(0, safeTotal)
            val attempt = QuizAttempt(
                userId = resolvedUid,
                category = category,
                mode = mode,
                difficulty = difficulty,
                cefrLevel = cefrLevel,
                score = safeScore.toLong(),
                totalQuestions = safeTotal.toLong(),
                durationSeconds = durationSeconds.coerceAtLeast(0).toLong(),
                topicTitle = topicTitle
            )
            requireFirestore().collection("users").document(resolvedUid)
                .collection("quiz_attempts")
                .document()
                .set(attempt.toCreateMap())
                .await()

            val pct = ((safeScore * 100.0) / safeTotal).toLong().coerceIn(0L, 100L)
            val mins = (durationSeconds / 60).coerceAtLeast(1).toLong()
            val userDoc = requireFirestore().collection("users").document(resolvedUid)
            userDoc.update(
                mapOf(
                    "quizzesCompleted" to FieldValue.increment(1L),
                    "totalQuizScorePct" to FieldValue.increment(pct),
                    "totalStudyMinutes" to FieldValue.increment(mins),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            bumpDailyActivityOnly(
                userId = resolvedUid,
                minutesDelta = mins,
                wordsReviewedDelta = 0L,
                quizzesDelta = 1L
            )

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markContentProgress(
        itemId: String,
        contentType: String,
        cefrLevel: String,
        completed: Boolean,
        scorePct: Int,
        minutesSpent: Int
    ): Result<Unit> {
        val docId = sanitizeFirestoreId(itemId)
        val uid = auth?.currentUser?.uid
        val path = "users/${uid ?: "unauthenticated"}/content_progress/$docId"
        return try {
            val resolvedUid = requireUserId()
            val docRef = requireFirestore().collection("users").document(resolvedUid)
                .collection("content_progress").document(docId)
            val snap = docRef.get().await()
            val existing = if (snap.exists()) {
                snap.toObject(ContentProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null

            val newlyCompleted = completed && (existing == null || !existing.completed)
            val bestScore = maxOf(existing?.bestScorePct ?: 0L, scorePct.coerceIn(0, 100).toLong())
            val totalMins = (existing?.minutesSpent ?: 0L) + minutesSpent.coerceAtLeast(0).toLong()

            if (!snap.exists()) {
                val record = ContentProgress(
                    id = docId,
                    userId = resolvedUid,
                    contentType = contentType,
                    cefrLevel = cefrLevel,
                    completed = completed,
                    bestScorePct = bestScore,
                    minutesSpent = totalMins.coerceIn(0L, 10_000L)
                )
                docRef.set(record.toCreateMap()).await()
            } else {
                docRef.update(
                    mapOf(
                        "cefrLevel" to normalizeCefr(cefrLevel),
                        "completed" to (existing?.completed == true || completed),
                        "bestScorePct" to bestScore,
                        "minutesSpent" to totalMins.coerceIn(0L, 10_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (newlyCompleted) {
                val counterField = when (contentType) {
                    "STORY" -> "storiesCompleted"
                    "CONVERSATION" -> "conversationsCompleted"
                    "GRAMMAR" -> "grammarCompleted"
                    "VERB" -> "verbsPracticed"
                    else -> null
                }
                val updates = mutableMapOf<String, Any>(
                    "totalStudyMinutes" to FieldValue.increment(minutesSpent.coerceAtLeast(1).toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (counterField != null) {
                    updates[counterField] = FieldValue.increment(1L)
                }
                requireFirestore().collection("users").document(resolvedUid).update(updates).await()
            } else if (minutesSpent > 0) {
                requireFirestore().collection("users").document(resolvedUid).update(
                    mapOf(
                        "totalStudyMinutes" to FieldValue.increment(minutesSpent.toLong()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            bumpDailyActivityOnly(
                userId = resolvedUid,
                minutesDelta = minutesSpent.coerceAtLeast(1).toLong(),
                wordsReviewedDelta = 0L,
                quizzesDelta = 0L
            )

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun bumpActivityAndProfile(
        userId: String,
        minutesDelta: Long,
        wordsReviewedDelta: Long,
        quizzesDelta: Long,
        masteredDelta: Long,
        learningDelta: Long
    ) {
        val profileUpdates = mutableMapOf<String, Any>(
            "totalStudyMinutes" to FieldValue.increment(minutesDelta),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (masteredDelta != 0L) profileUpdates["wordsMasteredCount"] = FieldValue.increment(masteredDelta)
        if (learningDelta != 0L) profileUpdates["wordsLearningCount"] = FieldValue.increment(learningDelta)
        requireFirestore().collection("users").document(userId).update(profileUpdates).await()
        bumpDailyActivityOnly(userId, minutesDelta, wordsReviewedDelta, quizzesDelta)
    }

    private suspend fun bumpDailyActivityOnly(
        userId: String,
        minutesDelta: Long,
        wordsReviewedDelta: Long,
        quizzesDelta: Long
    ) {
        val dateKey = todayDateKey()
        val dayRef = requireFirestore().collection("users").document(userId)
            .collection("daily_activity").document(dateKey)
        val snap = dayRef.get().await()
        if (!snap.exists()) {
            val activity = DailyActivity(
                id = dateKey,
                userId = userId,
                dateKey = dateKey,
                minutesStudied = minutesDelta.coerceIn(0L, 1440L),
                wordsReviewed = wordsReviewedDelta.coerceIn(0L, 50_000L),
                quizzesTaken = quizzesDelta.coerceIn(0L, 10_000L)
            )
            dayRef.set(activity.toCreateMap()).await()
        } else {
            dayRef.update(
                mapOf(
                    "minutesStudied" to FieldValue.increment(minutesDelta),
                    "wordsReviewed" to FieldValue.increment(wordsReviewedDelta),
                    "quizzesTaken" to FieldValue.increment(quizzesDelta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }
}
