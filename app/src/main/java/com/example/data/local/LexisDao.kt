package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LexisDao {

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Query("SELECT COUNT(*) FROM verbs")
    suspend fun getVerbCount(): Int

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getStoryCount(): Int

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(items: List<WordEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerbs(items: List<VerbEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrammarLessons(items: List<GrammarLessonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(items: List<StoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(items: List<ConversationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoicePersonas(items: List<VoicePersonaEntity>)

    @Query(
        """
        SELECT * FROM words
        WHERE (:level = 'ALL' OR lowestLevel = :level OR highestLevel = :level OR searchText LIKE '%|' || :level || '|%')
          AND (:query = '' OR headwordLower LIKE '%' || :query || '%' OR searchText LIKE '%' || :query || '%')
        ORDER BY
          CASE WHEN headwordLower = :query THEN 0
               WHEN headwordLower LIKE :query || '%' THEN 1
               ELSE 2 END,
          headwordLower ASC
        """
    )
    fun observeWords(query: String, level: String): Flow<List<WordEntryEntity>>

    @Query("SELECT * FROM words ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWords(limit: Int): List<WordEntryEntity>

    @Query("SELECT * FROM words WHERE lowestLevel = :level ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWordsByLevel(level: String, limit: Int): List<WordEntryEntity>

    @Query("SELECT * FROM words WHERE id = :wordId LIMIT 1")
    suspend fun getWordById(wordId: String): WordEntryEntity?

    @Query("SELECT * FROM words WHERE headwordLower = :lower LIMIT 1")
    suspend fun findWordByExactLower(lower: String): WordEntryEntity?

    @Query("UPDATE words SET ipaUs = :ipaUs, ipaUk = :ipaUk, sensesJson = :sensesJson, enriched = 1 WHERE id = :wordId")
    suspend fun updateEnrichedWord(wordId: String, ipaUs: String, ipaUk: String, sensesJson: String)

    @Query(
        """
        SELECT * FROM verbs
        WHERE (:level = 'ALL' OR cefrLevel = :level)
          AND (:onlyIrregular = 0 OR irregular = 1)
          AND (:query = '' OR v1 LIKE '%' || :query || '%' OR v2 LIKE '%' || :query || '%' OR v3 LIKE '%' || :query || '%' OR arabic LIKE '%' || :query || '%')
        ORDER BY irregular DESC, v1 ASC
        """
    )
    fun observeVerbs(query: String, level: String, onlyIrregular: Boolean): Flow<List<VerbEntity>>

    @Query("SELECT * FROM grammar_lessons WHERE (:level = 'ALL' OR cefrLevel = :level) ORDER BY cefrLevel ASC, orderIndex ASC")
    fun observeGrammarLessons(level: String): Flow<List<GrammarLessonEntity>>

    @Query("SELECT * FROM stories WHERE cefrLevel = :level AND (:query = '' OR titleEn LIKE '%' || :query || '%' OR titleAr LIKE '%' || :query || '%' OR theme LIKE '%' || :query || '%') ORDER BY number ASC")
    fun observeStories(level: String, query: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :storyId LIMIT 1")
    fun observeStoryById(storyId: String): Flow<StoryEntity?>

    @Query("UPDATE stories SET paragraphsJson = :paragraphsJson, quizJson = :quizJson, aiGenerated = 1 WHERE id = :storyId")
    suspend fun updateStoryContent(storyId: String, paragraphsJson: String, quizJson: String)

    @Query("SELECT * FROM conversations WHERE cefrLevel = :level AND (:query = '' OR titleEn LIKE '%' || :query || '%' OR titleAr LIKE '%' || :query || '%' OR speaker1Label LIKE '%' || :query || '%' OR speaker2Label LIKE '%' || :query || '%') ORDER BY number ASC")
    fun observeConversations(level: String, query: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :convId LIMIT 1")
    fun observeConversationById(convId: String): Flow<ConversationEntity?>

    @Query("UPDATE conversations SET turnsJson = :turnsJson, aiGenerated = 1 WHERE id = :convId")
    suspend fun updateConversationTurns(convId: String, turnsJson: String)

    @Query("SELECT * FROM voice_personas ORDER BY id ASC")
    fun observeVoicePersonas(): Flow<List<VoicePersonaEntity>>

    @Query("SELECT * FROM voice_personas WHERE id = :id LIMIT 1")
    suspend fun getVoicePersona(id: String): VoicePersonaEntity?
}
