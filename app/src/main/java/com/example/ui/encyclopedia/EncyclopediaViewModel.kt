package com.example.ui.encyclopedia

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GrammarExample
import com.example.data.local.GrammarLessonEntity
import com.example.data.local.GrammarQuizQuestion
import com.example.data.local.LexisDao
import com.example.data.local.LexisJsonCodec
import com.example.data.local.VerbEntity
import com.example.data.remote.ContentProgress
import com.example.data.remote.UserProgressRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GrammarLessonUi(
    val entity: GrammarLessonEntity,
    val examples: List<GrammarExample>,
    val quiz: List<GrammarQuizQuestion>,
    val completed: Boolean,
    val bestScorePct: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class EncyclopediaViewModel(
    private val dao: LexisDao,
    private val progressRepository: UserProgressRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _grammarLevel = MutableStateFlow("ALL")
    val grammarLevel: StateFlow<String> = _grammarLevel.asStateFlow()

    private val _verbQuery = MutableStateFlow("")
    val verbQuery: StateFlow<String> = _verbQuery.asStateFlow()

    private val _verbLevel = MutableStateFlow("ALL")
    val verbLevel: StateFlow<String> = _verbLevel.asStateFlow()

    private val _onlyIrregular = MutableStateFlow(false)
    val onlyIrregular: StateFlow<Boolean> = _onlyIrregular.asStateFlow()

    private val _selectedLesson = MutableStateFlow<GrammarLessonUi?>(null)
    val selectedLesson: StateFlow<GrammarLessonUi?> = _selectedLesson.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val contentProgressFlow: StateFlow<Map<String, ContentProgress>> =
        progressRepository.observeContentProgress(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Error observing content progress", firestoreError ?: error)
                emit(emptyMap())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val grammarLessons: StateFlow<List<GrammarLessonUi>> = combine(
        _grammarLevel.flatMapLatest { dao.observeGrammarLessons(it) },
        contentProgressFlow
    ) { lessons, progMap ->
        lessons.map { l ->
            val p = progMap[l.id]
            GrammarLessonUi(
                entity = l,
                examples = LexisJsonCodec.parseGrammarExamples(l.examplesJson),
                quiz = LexisJsonCodec.parseGrammarQuiz(l.quizJson),
                completed = p?.completed == true,
                bestScorePct = (p?.bestScorePct ?: 0L).toInt()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val verbs: StateFlow<List<VerbEntity>> = combine(
        _verbQuery,
        _verbLevel,
        _onlyIrregular
    ) { q, lvl, irr ->
        Triple(q.trim().lowercase(), lvl, irr)
    }.flatMapLatest { (q, lvl, irr) ->
        dao.observeVerbs(q, lvl, irr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun setGrammarLevel(level: String) {
        _grammarLevel.value = level
    }

    fun openLesson(lesson: GrammarLessonUi) {
        _selectedLesson.value = lesson
    }

    fun closeLesson() {
        _selectedLesson.value = null
    }

    fun setVerbQuery(q: String) {
        _verbQuery.value = q
    }

    fun setVerbLevel(level: String) {
        _verbLevel.value = level
    }

    fun toggleOnlyIrregular() {
        _onlyIrregular.value = !_onlyIrregular.value
    }

    fun submitGrammarQuizResult(lesson: GrammarLessonUi, score: Int, total: Int) {
        viewModelScope.launch {
            val pct = if (total > 0) ((score * 100) / total) else 100
            progressRepository.markContentProgress(
                itemId = lesson.entity.id,
                contentType = "GRAMMAR",
                cefrLevel = lesson.entity.cefrLevel,
                completed = true,
                scorePct = pct,
                minutesSpent = 3
            ).onFailure { err ->
                _actionError.value = err.localizedMessage ?: "تعذر حفظ نتيجة اختبار القاعدة"
            }
            progressRepository.recordQuizAttempt(
                category = "GRAMMAR",
                mode = "GRAMMAR_RULE",
                difficulty = when (lesson.entity.cefrLevel) {
                    "A1", "A2" -> "BEGINNER"
                    "B1", "B2" -> "INTERMEDIATE"
                    else -> "ADVANCED"
                },
                cefrLevel = lesson.entity.cefrLevel,
                score = score,
                totalQuestions = total.coerceAtLeast(1),
                durationSeconds = total * 12,
                topicTitle = lesson.entity.titleEn
            )
        }
    }

    fun markVerbPracticed(verb: VerbEntity) {
        viewModelScope.launch {
            progressRepository.markContentProgress(
                itemId = "verb_${verb.id}",
                contentType = "VERB",
                cefrLevel = verb.cefrLevel,
                completed = true,
                scorePct = 100,
                minutesSpent = 1
            )
        }
    }

    companion object {
        private const val TAG = "EncyclopediaVM"
    }
}
