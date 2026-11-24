package com.example.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.local.ThemeMode
import com.example.data.remote.DailyActivity
import com.example.data.remote.QuizAttempt
import com.example.data.remote.UserLearningProfile
import com.example.data.remote.UserProgressRepository
import com.example.data.remote.WordProgress
import com.example.di.LexisUiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StatsDashboardUi(
    val profile: UserLearningProfile,
    val recentQuizzes: List<QuizAttempt>,
    val dailyActivities: List<DailyActivity>,
    val cefrMasteredCounts: Map<String, Int>,
    val cefrTotalLearnedCounts: Map<String, Int>,
    val favoriteWordsCount: Int,
    val averageQuizPct: Int,
    val settings: UserSettings
)

class StatsViewModel(
    private val progressRepository: UserProgressRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val currentUserId: String,
    private val initialDisplayName: String?
) : ViewModel() {

    init {
        viewModelScope.launch {
            progressRepository.ensureProfileExists(initialDisplayName)
        }
    }

    val uiState: StateFlow<LexisUiState<StatsDashboardUi>> = combine(
        progressRepository.observeProfile(currentUserId),
        progressRepository.observeQuizAttempts(currentUserId),
        progressRepository.observeDailyActivity(currentUserId),
        progressRepository.observeWordProgressMap(currentUserId),
        prefsRepository.settingsFlow
    ) { profile, quizzes, daily, wordMap, settings ->
        val safeProfile = profile ?: UserLearningProfile(
            userId = currentUserId,
            displayName = initialDisplayName ?: "Learner",
            preferredLevel = "A1"
        )
        val levels = listOf("A1", "A2", "B1", "B2", "C1")
        val masteredByLevel = levels.associateWith { lvl ->
            wordMap.values.count { it.cefrLevel == lvl && it.masteryState == "MASTERED" }
        }
        val activeByLevel = levels.associateWith { lvl ->
            wordMap.values.count { it.cefrLevel == lvl && it.masteryState != "NEW" }
        }
        val favCount = wordMap.values.count { it.favorite }
        val avgQuiz = if (safeProfile.quizzesCompleted > 0L) {
            (safeProfile.totalQuizScorePct / safeProfile.quizzesCompleted).toInt().coerceIn(0, 100)
        } else 0

        LexisUiState.Success(
            StatsDashboardUi(
                profile = safeProfile,
                recentQuizzes = quizzes,
                dailyActivities = daily.sortedBy { it.dateKey },
                cefrMasteredCounts = masteredByLevel,
                cefrTotalLearnedCounts = activeByLevel,
                favoriteWordsCount = favCount,
                averageQuizPct = avgQuiz,
                settings = settings
            )
        ) as LexisUiState<StatsDashboardUi>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing stats dashboard", rootError)
        emit(LexisUiState.Error(rootError.localizedMessage ?: "تعذر تحميل الإحصائيات"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LexisUiState.Loading
    )

    fun setPreferredLevel(level: String) {
        viewModelScope.launch {
            progressRepository.updatePreferredLevel(level)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
        }
    }

    fun setArabicUi(arabic: Boolean) {
        viewModelScope.launch {
            prefsRepository.setArabicUi(arabic)
        }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsRepository.setDailyGoalMinutes(minutes)
        }
    }

    companion object {
        private const val TAG = "StatsVM"
    }
}
