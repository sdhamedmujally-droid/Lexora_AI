package com.example.ui.practice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LexisDao
import com.example.data.local.LexisJsonCodec
import com.example.data.local.WordEntryEntity
import com.example.data.local.WordSense
import com.example.data.remote.Sm2Quality
import com.example.data.remote.UserProgressRepository
import com.example.data.remote.WordProgress
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FlashcardMode(val labelAr: String, val labelEn: String) {
    VISUAL("الذاكرة البصرية", "Visual Memory"),
    AUDITORY("الذاكرة السمعية", "Auditory Memory")
}

enum class QuizMode(val labelAr: String, val descAr: String) {
    MCQ("اختيار من متعدد", "اختر المعنى العربي الدقيق للكلمة الإنجليزية"),
    LISTENING("إملاء واستماع", "استمع إلى نطق الكلمة واختر الكلمة الصحيحة"),
    CLOZE("إكمال الجملة السياقية", "أكمل الفراغ داخل جملة سياقية أصيلة"),
    MATCHING("مطابقة الكلمات ومعانيها", "طابق الكلمة الإنجليزية بمعناها ونوعها النحوي"),
    SCRAMBLE("ترتيب حروف الكلمة", "رتب الحروف لتكوين الكلمة المطابقة للمعنى")
}

enum class QuizDifficulty(val labelAr: String, val apiName: String, val optionCount: Int) {
    BEGINNER("مبتدئ", "BEGINNER", 3),
    INTERMEDIATE("متوسط", "INTERMEDIATE", 4),
    ADVANCED("متقدم", "ADVANCED", 4)
}

data class FlashcardItem(
    val word: WordEntryEntity,
    val senses: List<WordSense>,
    val progress: WordProgress?
)

data class QuizQuestionUi(
    val index: Int,
    val word: WordEntryEntity,
    val promptEn: String,
    val promptAr: String,
    val audioText: String?,
    val options: List<String>,
    val correctIndex: Int,
    val correctAnswerText: String,
    val explanationAr: String
)

data class QuizAnswerReview(
    val question: QuizQuestionUi,
    val chosenIndex: Int,
    val timedOut: Boolean,
    val isCorrect: Boolean
)

data class QuizSessionState(
    val active: Boolean = false,
    val finished: Boolean = false,
    val mode: QuizMode = QuizMode.MCQ,
    val difficulty: QuizDifficulty = QuizDifficulty.BEGINNER,
    val cefrFilter: String = "ALL",
    val secondsPerQuestion: Int = 20, // 0 = no timer
    val questions: List<QuizQuestionUi> = emptyList(),
    val currentIndex: Int = 0,
    val remainingSeconds: Int = 20,
    val paused: Boolean = false,
    val totalElapsedSeconds: Int = 0,
    val reviews: List<QuizAnswerReview> = emptyList(),
    val selectedOption: Int? = null,
    val scrambleLetters: List<Char> = emptyList(),
    val scrambleBuilt: String = ""
)

class PracticeViewModel(
    private val dao: LexisDao,
    private val progressRepository: UserProgressRepository,
    private val currentUserId: String
) : ViewModel() {

    private val progressMapFlow: StateFlow<Map<String, WordProgress>> =
        progressRepository.observeWordProgressMap(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Progress stream error", firestoreError ?: error)
                emit(emptyMap())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    // Flashcards State
    private val _flashcards = MutableStateFlow<List<FlashcardItem>>(emptyList())
    val flashcards: StateFlow<List<FlashcardItem>> = _flashcards.asStateFlow()

    private val _flashcardIndex = MutableStateFlow(0)
    val flashcardIndex: StateFlow<Int> = _flashcardIndex.asStateFlow()

    private val _flashcardFlipped = MutableStateFlow(false)
    val flashcardFlipped: StateFlow<Boolean> = _flashcardFlipped.asStateFlow()

    private val _flashcardMode = MutableStateFlow(FlashcardMode.VISUAL)
    val flashcardMode: StateFlow<FlashcardMode> = _flashcardMode.asStateFlow()

    private val _flashcardLevel = MutableStateFlow("ALL")
    val flashcardLevel: StateFlow<String> = _flashcardLevel.asStateFlow()

    private val _sessionReviewed = MutableStateFlow(0)
    val sessionReviewed: StateFlow<Int> = _sessionReviewed.asStateFlow()

    private val _practiceError = MutableStateFlow<String?>(null)
    val practiceError: StateFlow<String?> = _practiceError.asStateFlow()

    // Quiz State
    private val _quizState = MutableStateFlow(QuizSessionState())
    val quizState: StateFlow<QuizSessionState> = _quizState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadFlashcardDeck("ALL")
    }

    fun setFlashcardMode(mode: FlashcardMode) {
        _flashcardMode.value = mode
        _flashcardFlipped.value = false
    }

    fun loadFlashcardDeck(level: String) {
        _flashcardLevel.value = level
        viewModelScope.launch {
            val words = if (level == "ALL") dao.getRandomWords(25) else dao.getRandomWordsByLevel(level, 25)
            val map = progressMapFlow.value
            val now = System.currentTimeMillis()
            val sorted = words.sortedBy { w ->
                val p = map[w.id]
                when {
                    p == null -> 0L
                    p.nextReviewAtMs <= now -> 1L
                    else -> p.nextReviewAtMs
                }
            }
            _flashcards.value = sorted.map { w ->
                FlashcardItem(w, LexisJsonCodec.parseSenses(w.sensesJson), map[w.id])
            }
            _flashcardIndex.value = 0
            _flashcardFlipped.value = false
        }
    }

    fun flipCard() {
        _flashcardFlipped.value = !_flashcardFlipped.value
    }

    fun rateFlashcard(quality: Sm2Quality) {
        val deck = _flashcards.value
        val idx = _flashcardIndex.value
        val item = deck.getOrNull(idx) ?: return
        viewModelScope.launch {
            _practiceError.value = null
            val res = progressRepository.recordFlashcardReview(
                wordId = item.word.id,
                headword = item.word.headword,
                cefrLevel = item.word.lowestLevel,
                current = item.progress,
                quality = quality
            )
            res.onFailure { err ->
                _practiceError.value = err.localizedMessage ?: "تعذر حفظ تقييم البطاقة"
            }.onSuccess { updatedProg ->
                val updatedList = deck.toMutableList()
                updatedList[idx] = item.copy(progress = updatedProg)
                _flashcards.value = updatedList
                _sessionReviewed.value += 1
                _flashcardFlipped.value = false
                if (idx + 1 < updatedList.size) {
                    _flashcardIndex.value = idx + 1
                } else {
                    loadFlashcardDeck(_flashcardLevel.value)
                }
            }
        }
    }

    fun configureAndStartQuiz(
        mode: QuizMode,
        difficulty: QuizDifficulty,
        cefrFilter: String,
        secondsPerQuestion: Int,
        questionCount: Int = 10
    ) {
        timerJob?.cancel()
        viewModelScope.launch {
            val pool = when {
                cefrFilter != "ALL" -> dao.getRandomWordsByLevel(cefrFilter, 60)
                difficulty == QuizDifficulty.BEGINNER -> dao.getRandomWordsByLevel("A1", 40) + dao.getRandomWordsByLevel("A2", 30)
                difficulty == QuizDifficulty.INTERMEDIATE -> dao.getRandomWordsByLevel("B1", 40) + dao.getRandomWordsByLevel("B2", 30)
                else -> dao.getRandomWordsByLevel("B2", 35) + dao.getRandomWordsByLevel("C1", 35)
            }.distinctBy { it.id }.shuffled()

            val targets = pool.take(questionCount.coerceAtMost(pool.size))
            if (targets.size < 4) {
                _practiceError.value = "لا توجد كلمات كافية لبدء الاختبار في هذا الفلتر."
                return@launch
            }

            val questions = targets.mapIndexed { index, word ->
                val senses = LexisJsonCodec.parseSenses(word.sensesJson)
                val primary = senses.firstOrNull()
                val distractors = pool.filter { it.id != word.id }
                    .shuffled()
                    .take(difficulty.optionCount - 1)

                when (mode) {
                    QuizMode.MCQ -> {
                        val correctText = "${primary?.arabic ?: word.headword} (${primary?.posAr.orEmpty()})"
                        val wrongTexts = distractors.map { d ->
                            val ds = LexisJsonCodec.parseSenses(d.sensesJson).firstOrNull()
                            "${ds?.arabic ?: d.headword} (${ds?.posAr.orEmpty()})"
                        }
                        val allOpts = (wrongTexts + correctText).shuffled()
                        QuizQuestionUi(
                            index = index,
                            word = word,
                            promptEn = word.headword,
                            promptAr = "اختر المعنى العربي الدقيق لكلمة (${word.headword}):",
                            audioText = word.headword,
                            options = allOpts,
                            correctIndex = allOpts.indexOf(correctText),
                            correctAnswerText = correctText,
                            explanationAr = "${word.headword} (${word.lowestLevel}): ${primary?.arabic.orEmpty()} — ${primary?.exampleEn.orEmpty()}"
                        )
                    }

                    QuizMode.LISTENING -> {
                        val allOpts = (distractors.map { it.headword } + word.headword).shuffled()
                        QuizQuestionUi(
                            index = index,
                            word = word,
                            promptEn = "Listen carefully and pick the exact word",
                            promptAr = "استمع إلى النطق ثم اختر الكلمة المطابقة:",
                            audioText = word.headword,
                            options = allOpts,
                            correctIndex = allOpts.indexOf(word.headword),
                            correctAnswerText = word.headword,
                            explanationAr = "الكلمة المنطوقة هي ${word.headword} وتعني: ${primary?.arabic.orEmpty()}"
                        )
                    }

                    QuizMode.CLOZE -> {
                        val rawSentence = primary?.exampleEn.orEmpty()
                        val regex = Regex("\\b${Regex.escape(word.headword.split(",", " ").first())}\\w*\\b", RegexOption.IGNORE_CASE)
                        val masked = if (regex.containsMatchIn(rawSentence)) {
                            regex.replace(rawSentence, "______")
                        } else {
                            "Choose the word that means '${primary?.arabic.orEmpty()}': ______"
                        }
                        val allOpts = (distractors.map { it.headword } + word.headword).shuffled()
                        QuizQuestionUi(
                            index = index,
                            word = word,
                            promptEn = masked,
                            promptAr = "أكمل الفراغ بالكلمة المناسبة للسياق (${primary?.exampleAr.orEmpty()}):",
                            audioText = rawSentence,
                            options = allOpts,
                            correctIndex = allOpts.indexOf(word.headword),
                            correctAnswerText = word.headword,
                            explanationAr = "الجملة الكاملة: $rawSentence"
                        )
                    }

                    QuizMode.MATCHING -> {
                        val clue = "${primary?.arabic.orEmpty()} • ${primary?.posAr.orEmpty()} (${word.lowestLevel})"
                        val allOpts = (distractors.map { it.headword } + word.headword).shuffled()
                        QuizQuestionUi(
                            index = index,
                            word = word,
                            promptEn = clue,
                            promptAr = "طابق البطاقة العربية التالية بالكلمة الإنجليزية الصحيحة:",
                            audioText = null,
                            options = allOpts,
                            correctIndex = allOpts.indexOf(word.headword),
                            correctAnswerText = word.headword,
                            explanationAr = "تطابق: ${word.headword} ⇄ $clue"
                        )
                    }

                    QuizMode.SCRAMBLE -> {
                        val cleanWord = word.headword.split(",", " ").first().trim()
                        QuizQuestionUi(
                            index = index,
                            word = word,
                            promptEn = "${primary?.arabic.orEmpty()} (${primary?.posAr.orEmpty()})",
                            promptAr = "كوّن الكلمة الإنجليزية الصحيحة من الحروف المبعثرة:",
                            audioText = cleanWord,
                            options = emptyList(),
                            correctIndex = 0,
                            correctAnswerText = cleanWord,
                            explanationAr = "التهجئة الصحيحة: $cleanWord — ${primary?.exampleEn.orEmpty()}"
                        )
                    }
                }
            }

            val firstScramble = if (mode == QuizMode.SCRAMBLE) {
                questions.first().correctAnswerText.toList().shuffled()
            } else emptyList()

            _quizState.value = QuizSessionState(
                active = true,
                finished = false,
                mode = mode,
                difficulty = difficulty,
                cefrFilter = cefrFilter,
                secondsPerQuestion = secondsPerQuestion,
                questions = questions,
                currentIndex = 0,
                remainingSeconds = secondsPerQuestion,
                paused = false,
                totalElapsedSeconds = 0,
                reviews = emptyList(),
                selectedOption = null,
                scrambleLetters = firstScramble,
                scrambleBuilt = ""
            )
            startQuestionTimer()
        }
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        val state = _quizState.value
        if (!state.active || state.finished || state.secondsPerQuestion <= 0) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val cur = _quizState.value
                if (!cur.active || cur.finished) break
                if (cur.paused) continue
                val nextElapsed = cur.totalElapsedSeconds + 1
                val nextRem = cur.remainingSeconds - 1
                if (nextRem <= 0) {
                    _quizState.value = cur.copy(
                        totalElapsedSeconds = nextElapsed,
                        remainingSeconds = 0
                    )
                    handleTimeout()
                    break
                } else {
                    _quizState.value = cur.copy(
                        totalElapsedSeconds = nextElapsed,
                        remainingSeconds = nextRem
                    )
                }
            }
        }
    }

    fun toggleQuizPause() {
        val cur = _quizState.value
        _quizState.value = cur.copy(paused = !cur.paused)
    }

    fun tapScrambleLetter(index: Int) {
        val cur = _quizState.value
        val letters = cur.scrambleLetters.toMutableList()
        if (index !in letters.indices) return
        val ch = letters.removeAt(index)
        _quizState.value = cur.copy(
            scrambleLetters = letters,
            scrambleBuilt = cur.scrambleBuilt + ch
        )
    }

    fun clearScrambleBuilt() {
        val cur = _quizState.value
        val q = cur.questions.getOrNull(cur.currentIndex) ?: return
        _quizState.value = cur.copy(
            scrambleLetters = q.correctAnswerText.toList().shuffled(),
            scrambleBuilt = ""
        )
    }

    fun submitScrambleAnswer() {
        val cur = _quizState.value
        val q = cur.questions.getOrNull(cur.currentIndex) ?: return
        val isCorrect = cur.scrambleBuilt.equals(q.correctAnswerText, ignoreCase = true)
        advanceQuiz(
            AnswerInput(chosenIndex = if (isCorrect) 0 else -1, timedOut = false, isCorrect = isCorrect)
        )
    }

    fun selectQuizOption(optionIndex: Int) {
        val cur = _quizState.value
        val q = cur.questions.getOrNull(cur.currentIndex) ?: return
        val isCorrect = optionIndex == q.correctIndex
        advanceQuiz(AnswerInput(chosenIndex = optionIndex, timedOut = false, isCorrect = isCorrect))
    }

    private fun handleTimeout() {
        advanceQuiz(AnswerInput(chosenIndex = -1, timedOut = true, isCorrect = false))
    }

    private data class AnswerInput(val chosenIndex: Int, val timedOut: Boolean, val isCorrect: Boolean)

    private fun advanceQuiz(input: AnswerInput) {
        timerJob?.cancel()
        val cur = _quizState.value
        val q = cur.questions.getOrNull(cur.currentIndex) ?: return
        val review = QuizAnswerReview(
            question = q,
            chosenIndex = input.chosenIndex,
            timedOut = input.timedOut,
            isCorrect = input.isCorrect
        )
        val updatedReviews = cur.reviews + review
        val nextIndex = cur.currentIndex + 1

        if (nextIndex >= cur.questions.size) {
            val finalState = cur.copy(
                finished = true,
                reviews = updatedReviews,
                selectedOption = null
            )
            _quizState.value = finalState
            persistQuizAttempt(finalState)
        } else {
            val nextQ = cur.questions[nextIndex]
            val nextLetters = if (cur.mode == QuizMode.SCRAMBLE) {
                nextQ.correctAnswerText.toList().shuffled()
            } else emptyList()
            _quizState.value = cur.copy(
                currentIndex = nextIndex,
                remainingSeconds = cur.secondsPerQuestion,
                reviews = updatedReviews,
                selectedOption = null,
                scrambleLetters = nextLetters,
                scrambleBuilt = ""
            )
            startQuestionTimer()
        }
    }

    private fun persistQuizAttempt(state: QuizSessionState) {
        val score = state.reviews.count { it.isCorrect }
        viewModelScope.launch {
            val res = progressRepository.recordQuizAttempt(
                category = "VOCABULARY",
                mode = state.mode.name,
                difficulty = state.difficulty.apiName,
                cefrLevel = state.cefrFilter,
                score = score,
                totalQuestions = state.questions.size,
                durationSeconds = state.totalElapsedSeconds.coerceAtLeast(state.questions.size * 3),
                topicTitle = "${state.mode.labelAr} (${state.difficulty.labelAr})"
            )
            res.onFailure { err ->
                _practiceError.value = err.localizedMessage ?: "تعذر حفظ نتيجة الاختبار"
            }
        }
    }

    fun exitQuiz() {
        timerJob?.cancel()
        _quizState.value = QuizSessionState()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private const val TAG = "PracticeVM"
    }
}
