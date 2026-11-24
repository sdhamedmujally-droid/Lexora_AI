package com.example.ui.dictionary

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LexisDao
import com.example.data.local.LexisJsonCodec
import com.example.data.local.WordEntryEntity
import com.example.data.local.WordSense
import com.example.data.remote.AiContentRepository
import com.example.data.remote.UserProgressRepository
import com.example.data.remote.WordProgress
import com.example.di.LexisUiState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DictionaryItemUi(
    val word: WordEntryEntity,
    val senses: List<WordSense>,
    val progress: WordProgress?
)

data class DictionaryScreenState(
    val items: List<DictionaryItemUi> = emptyList(),
    val totalFiltered: Int = 0,
    val favoriteCount: Int = 0,
    val masteredCount: Int = 0,
    val selectedLevel: String = "ALL",
    val onlyFavorites: Boolean = false,
    val query: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModel(
    private val dao: LexisDao,
    private val progressRepository: UserProgressRepository,
    private val aiRepository: AiContentRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _selectedLevel = MutableStateFlow("ALL")
    private val _onlyFavorites = MutableStateFlow(false)

    private val _selectedWord = MutableStateFlow<DictionaryItemUi?>(null)
    val selectedWord: StateFlow<DictionaryItemUi?> = _selectedWord.asStateFlow()

    private val _enrichingWordId = MutableStateFlow<String?>(null)
    val enrichingWordId: StateFlow<String?> = _enrichingWordId.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val wordProgressMapFlow = progressRepository.observeWordProgressMap(currentUserId)
        .map<Map<String, WordProgress>, LexisUiState<Map<String, WordProgress>>> { LexisUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing word progress", rootError)
            emit(LexisUiState.Error(rootError.message ?: "تعذر مزامنة التقدم السحابي"))
        }

    private val filteredWordsFlow = combine(_query, _selectedLevel) { q, lvl ->
        q.trim().lowercase() to lvl
    }.flatMapLatest { (cleanQ, lvl) ->
        dao.observeWords(cleanQ, lvl)
    }

    val uiState: StateFlow<LexisUiState<DictionaryScreenState>> = combine(
        filteredWordsFlow,
        wordProgressMapFlow,
        _query,
        _selectedLevel,
        _onlyFavorites
    ) { words, progressState, q, lvl, onlyFav ->
        if (progressState is LexisUiState.Error) {
            return@combine LexisUiState.Error(progressState.message)
        }
        val progressMap = (progressState as? LexisUiState.Success)?.data ?: emptyMap()
        val favTotal = progressMap.values.count { it.favorite }
        val masteredTotal = progressMap.values.count { it.masteryState == "MASTERED" }

        val mapped = words.mapNotNull { w ->
            val prog = progressMap[w.id]
            if (onlyFav && prog?.favorite != true) return@mapNotNull null
            DictionaryItemUi(
                word = w,
                senses = LexisJsonCodec.parseSenses(w.sensesJson),
                progress = prog
            )
        }
        LexisUiState.Success(
            DictionaryScreenState(
                items = mapped,
                totalFiltered = mapped.size,
                favoriteCount = favTotal,
                masteredCount = masteredTotal,
                selectedLevel = lvl,
                onlyFavorites = onlyFav,
                query = q
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LexisUiState.Loading
    )

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setLevel(level: String) {
        _selectedLevel.value = level
    }

    fun toggleFavoritesFilter() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun openWord(item: DictionaryItemUi) {
        _actionError.value = null
        _selectedWord.value = item
    }

    fun closeWord() {
        _selectedWord.value = null
        _actionError.value = null
    }

    fun clearError() {
        _actionError.value = null
    }

    fun toggleFavorite(item: DictionaryItemUi) {
        viewModelScope.launch {
            val res = progressRepository.toggleWordFavorite(
                wordId = item.word.id,
                headword = item.word.headword,
                cefrLevel = item.word.lowestLevel,
                current = item.progress
            )
            res.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "تعذر تحديث المفضلة"
            }
            res.onSuccess { newFav ->
                val cur = _selectedWord.value
                if (cur != null && cur.word.id == item.word.id) {
                    val updatedProg = (cur.progress ?: WordProgress(
                        id = item.word.id,
                        userId = currentUserId,
                        headword = item.word.headword,
                        cefrLevel = item.word.lowestLevel
                    )).copy(favorite = newFav)
                    _selectedWord.value = cur.copy(progress = updatedProg)
                }
            }
        }
    }

    fun enrichSelectedWordWithAi(item: DictionaryItemUi) {
        if (_enrichingWordId.value != null) return
        viewModelScope.launch {
            _actionError.value = null
            _enrichingWordId.value = item.word.id
            val result = aiRepository.enrichWord(item.word)
            _enrichingWordId.value = null
            result.onSuccess { updatedEntity ->
                val updatedSenses = LexisJsonCodec.parseSenses(updatedEntity.sensesJson)
                if (_selectedWord.value?.word?.id == updatedEntity.id) {
                    _selectedWord.value = item.copy(
                        word = updatedEntity,
                        senses = updatedSenses
                    )
                }
            }.onFailure { err ->
                _actionError.value = err.localizedMessage ?: "تعذر إثراء الكلمة عبر الذكاء الاصطناعي"
            }
        }
    }

    companion object {
        private const val TAG = "DictionaryVM"
    }
}
