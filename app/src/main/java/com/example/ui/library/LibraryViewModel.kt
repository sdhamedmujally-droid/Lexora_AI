package com.example.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.ConversationTurn
import com.example.data.local.LexisDao
import com.example.data.local.LexisJsonCodec
import com.example.data.local.StoryEntity
import com.example.data.local.StoryParagraph
import com.example.data.local.StoryQuizQuestion
import com.example.data.local.VoicePersonaEntity
import com.example.data.remote.AiContentRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val dao: LexisDao,
    private val progressRepository: UserProgressRepository,
    private val aiRepository: AiContentRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _storyLevel = MutableStateFlow("A1")
    val storyLevel: StateFlow<String> = _storyLevel.asStateFlow()

    private val _storyQuery = MutableStateFlow("")
    val storyQuery: StateFlow<String> = _storyQuery.asStateFlow()

    private val _convLevel = MutableStateFlow("A1")
    val convLevel: StateFlow<String> = _convLevel.asStateFlow()

    private val _convQuery = MutableStateFlow("")
    val convQuery: StateFlow<String> = _convQuery.asStateFlow()

    private val _selectedPersonaFilter = MutableStateFlow("ALL")
    val selectedPersonaFilter: StateFlow<String> = _selectedPersonaFilter.asStateFlow()

    private val _activeStoryId = MutableStateFlow<String?>(null)
    val activeStoryId: StateFlow<String?> = _activeStoryId.asStateFlow()

    private val _activeConvId = MutableStateFlow<String?>(null)
    val activeConvId: StateFlow<String?> = _activeConvId.asStateFlow()

    private val _generatingStory = MutableStateFlow(false)
    val generatingStory: StateFlow<Boolean> = _generatingStory.asStateFlow()

    private val _generatingConv = MutableStateFlow(false)
    val generatingConv: StateFlow<Boolean> = _generatingConv.asStateFlow()

    private val _libraryError = MutableStateFlow<String?>(null)
    val libraryError: StateFlow<String?> = _libraryError.asStateFlow()

    val contentProgressMap: StateFlow<Map<String, ContentProgress>> =
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

    val personas: StateFlow<List<VoicePersonaEntity>> = dao.observeVoicePersonas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stories: StateFlow<List<StoryEntity>> = combine(_storyLevel, _storyQuery) { lvl, q ->
        lvl to q.trim()
    }.flatMapLatest { (lvl, q) ->
        dao.observeStories(lvl, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<ConversationEntity>> = combine(
        _convLevel,
        _convQuery,
        _selectedPersonaFilter
    ) { lvl, q, personaId ->
        Triple(lvl, q.trim(), personaId)
    }.flatMapLatest { (lvl, q, personaId) ->
        dao.observeConversations(lvl, q).combine(MutableStateFlow(personaId)) { list, pid ->
            if (pid == "ALL") list else list.filter { it.speaker1Id == pid || it.speaker2Id == pid }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeStory: StateFlow<StoryEntity?> = _activeStoryId.flatMapLatest { id ->
        if (id == null) MutableStateFlow(null) else dao.observeStoryById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeConversation: StateFlow<ConversationEntity?> = _activeConvId.flatMapLatest { id ->
        if (id == null) MutableStateFlow(null) else dao.observeConversationById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    fun setStoryLevel(level: String) { _storyLevel.value = level }
    fun setStoryQuery(q: String) { _storyQuery.value = q }
    fun setConvLevel(level: String) { _convLevel.value = level }
    fun setConvQuery(q: String) { _convQuery.value = q }
    fun setPersonaFilter(personaId: String) { _selectedPersonaFilter.value = personaId }
    fun clearError() { _libraryError.value = null }

    fun openStory(story: StoryEntity) {
        _libraryError.value = null
        _activeStoryId.value = story.id
    }

    fun closeStory() {
        _activeStoryId.value = null
        _libraryError.value = null
    }

    fun openConversation(conv: ConversationEntity) {
        _libraryError.value = null
        _activeConvId.value = conv.id
    }

    fun closeConversation() {
        _activeConvId.value = null
        _libraryError.value = null
    }

    fun generateFullStoryWithAi(story: StoryEntity) {
        if (_generatingStory.value) return
        viewModelScope.launch {
            _libraryError.value = null
            _generatingStory.value = true
            aiRepository.generateStoryAndQuiz(story)
                .onFailure { err ->
                    _libraryError.value = err.localizedMessage ?: "تعذر توليد القصة بالذكاء الاصطناعي"
                }
            _generatingStory.value = false
        }
    }

    fun generateFullConversationWithAi(conv: ConversationEntity) {
        if (_generatingConv.value) return
        viewModelScope.launch {
            _libraryError.value = null
            _generatingConv.value = true
            val s1 = dao.getVoicePersona(conv.speaker1Id)
            val s2 = dao.getVoicePersona(conv.speaker2Id)
            aiRepository.generateConversation(conv, s1, s2)
                .onFailure { err ->
                    _libraryError.value = err.localizedMessage ?: "تعذر توليد المحادثة بالذكاء الاصطناعي"
                }
            _generatingConv.value = false
        }
    }

    fun submitStoryQuiz(story: StoryEntity, score: Int, total: Int) {
        viewModelScope.launch {
            val pct = if (total > 0) (score * 100) / total else 100
            progressRepository.markContentProgress(
                itemId = story.id,
                contentType = "STORY",
                cefrLevel = story.cefrLevel,
                completed = true,
                scorePct = pct,
                minutesSpent = story.estimatedMinutes
            )
            progressRepository.recordQuizAttempt(
                category = "STORY",
                mode = "STORY_10Q",
                difficulty = when (story.cefrLevel) {
                    "A1", "A2" -> "BEGINNER"
                    "B1", "B2" -> "INTERMEDIATE"
                    else -> "ADVANCED"
                },
                cefrLevel = story.cefrLevel,
                score = score,
                totalQuestions = total.coerceAtLeast(1),
                durationSeconds = total * 15,
                topicTitle = story.titleEn
            )
        }
    }

    fun markConversationCompleted(conv: ConversationEntity) {
        viewModelScope.launch {
            progressRepository.markContentProgress(
                itemId = conv.id,
                contentType = "CONVERSATION",
                cefrLevel = conv.cefrLevel,
                completed = true,
                scorePct = 100,
                minutesSpent = conv.durationMinutes
            )
        }
    }

    fun parseParagraphs(story: StoryEntity): List<StoryParagraph> =
        LexisJsonCodec.parseStoryParagraphs(story.paragraphsJson)

    fun parseStoryQuiz(story: StoryEntity): List<StoryQuizQuestion> =
        LexisJsonCodec.parseStoryQuiz(story.quizJson)

    fun parseTurns(conv: ConversationEntity): List<ConversationTurn> =
        LexisJsonCodec.parseConversationTurns(conv.turnsJson)

    companion object {
        private const val TAG = "LibraryVM"
    }
}
