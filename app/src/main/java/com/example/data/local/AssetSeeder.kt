package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AssetSeeder(
    private val context: Context,
    private val database: LexisDatabase
) {
    private val dao = database.lexisDao()
    private val mutex = Mutex()

    suspend fun ensureSeeded(): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                if (dao.getWordCount() > 0 &&
                    dao.getVerbCount() > 0 &&
                    dao.getStoryCount() > 0 &&
                    dao.getConversationCount() > 0
                ) {
                    return@withContext Result.success(Unit)
                }

                val words = parseWords()
                val verbs = parseVerbs()
                val grammar = parseGrammar()
                val stories = parseStories()
                val (personas, conversations) = parseConversations()

                database.withTransaction {
                    words.chunked(500).forEach { dao.insertWords(it) }
                    verbs.chunked(500).forEach { dao.insertVerbs(it) }
                    dao.insertGrammarLessons(grammar)
                    stories.chunked(100).forEach { dao.insertStories(it) }
                    dao.insertVoicePersonas(personas)
                    conversations.chunked(100).forEach { dao.insertConversations(it) }
                }

                Log.i(
                    TAG,
                    "Seeded Lexis DB: words=${words.size}, verbs=${verbs.size}, grammar=${grammar.size}, stories=${stories.size}, conversations=${conversations.size}, personas=${personas.size}"
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed Lexis assets into Room", e)
                Result.failure(e)
            }
        }
    }

    private fun readAsset(name: String): String {
        return context.assets.open(name).bufferedReader().use { it.readText() }
    }

    private fun parseWords(): List<WordEntryEntity> {
        val arr = JSONArray(readAsset("oxford_5000.json"))
        val out = ArrayList<WordEntryEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val sensesArr = obj.getJSONArray("senses")
            val levels = LinkedHashSet<String>()
            val searchParts = StringBuilder()
            var anyEnriched = false
            for (j in 0 until sensesArr.length()) {
                val s = sensesArr.getJSONObject(j)
                val lvl = s.optString("level", "A1")
                levels.add(lvl)
                searchParts.append('|').append(lvl).append('|')
                    .append(s.optString("pos", "").lowercase()).append(' ')
                    .append(s.optString("posAr", "")).append(' ')
                    .append(s.optString("arabic", "")).append(' ')
                if (s.optBoolean("enriched", false)) anyEnriched = true
            }
            val headword = obj.getString("headword")
            out.add(
                WordEntryEntity(
                    id = obj.getString("id"),
                    headword = headword,
                    headwordLower = headword.lowercase(),
                    lowestLevel = obj.optString("lowestLevel", "A1"),
                    highestLevel = obj.optString("highestLevel", "A1"),
                    ipaUs = obj.optString("ipaUs", ""),
                    ipaUk = obj.optString("ipaUk", ""),
                    sensesJson = sensesArr.toString(),
                    searchText = "${headword.lowercase()} $searchParts",
                    senseCount = sensesArr.length(),
                    enriched = anyEnriched
                )
            )
        }
        return out
    }

    private fun parseVerbs(): List<VerbEntity> {
        val arr = JSONArray(readAsset("verbs_1244.json"))
        val out = ArrayList<VerbEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            out.add(
                VerbEntity(
                    id = obj.getString("id"),
                    v1 = obj.getString("v1"),
                    v2 = obj.getString("v2"),
                    v3 = obj.getString("v3"),
                    cefrLevel = obj.optString("cefrLevel", "B1"),
                    irregular = obj.optBoolean("irregular", false),
                    arabic = obj.optString("arabic", ""),
                    exampleV1 = obj.optString("exampleV1", ""),
                    exampleV2 = obj.optString("exampleV2", ""),
                    exampleV3 = obj.optString("exampleV3", "")
                )
            )
        }
        return out
    }

    private fun parseGrammar(): List<GrammarLessonEntity> {
        val arr = JSONArray(readAsset("grammar_curriculum.json"))
        val out = ArrayList<GrammarLessonEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            out.add(
                GrammarLessonEntity(
                    id = obj.getString("id"),
                    cefrLevel = obj.getString("cefrLevel"),
                    orderIndex = obj.optInt("order", i + 1),
                    titleEn = obj.getString("titleEn"),
                    titleAr = obj.getString("titleAr"),
                    summaryAr = obj.optString("summaryAr", ""),
                    explanationAr = obj.optString("explanationAr", ""),
                    formula = obj.optString("formula", ""),
                    commonMistakeAr = obj.optString("commonMistakeAr", ""),
                    examplesJson = obj.getJSONArray("examples").toString(),
                    quizJson = obj.getJSONArray("quiz").toString()
                )
            )
        }
        return out
    }

    private fun parseStories(): List<StoryEntity> {
        val arr = JSONArray(readAsset("stories_catalog_500.json"))
        val out = ArrayList<StoryEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            out.add(
                StoryEntity(
                    id = obj.getString("id"),
                    number = obj.optInt("number", i + 1),
                    cefrLevel = obj.getString("cefrLevel"),
                    titleEn = obj.getString("titleEn"),
                    titleAr = obj.getString("titleAr"),
                    theme = obj.optString("theme", ""),
                    targetLines = obj.optInt("targetLines", 12),
                    estimatedMinutes = obj.optInt("estimatedMinutes", 3),
                    previewEn = obj.optString("previewEn", ""),
                    paragraphsJson = obj.optJSONArray("paragraphs")?.toString() ?: "[]",
                    quizJson = obj.optJSONArray("quiz")?.toString() ?: "[]",
                    aiGenerated = obj.optBoolean("aiGenerated", false)
                )
            )
        }
        return out
    }

    private fun parseConversations(): Pair<List<VoicePersonaEntity>, List<ConversationEntity>> {
        val root = JSONObject(readAsset("conversations_catalog_500.json"))
        val personasArr = root.getJSONArray("personas")
        val personas = ArrayList<VoicePersonaEntity>(personasArr.length())
        for (i in 0 until personasArr.length()) {
            val p = personasArr.getJSONObject(i)
            personas.add(
                VoicePersonaEntity(
                    id = p.getString("id"),
                    name = p.getString("name"),
                    nameAr = p.getString("nameAr"),
                    gender = p.getString("gender"),
                    accent = p.getString("accent"),
                    pitch = p.optDouble("pitch", 1.0).toFloat(),
                    rate = p.optDouble("rate", 1.0).toFloat(),
                    role = p.optString("role", "")
                )
            )
        }
        val convArr = root.getJSONArray("conversations")
        val conversations = ArrayList<ConversationEntity>(convArr.length())
        for (i in 0 until convArr.length()) {
            val c = convArr.getJSONObject(i)
            conversations.add(
                ConversationEntity(
                    id = c.getString("id"),
                    number = c.optInt("number", i + 1),
                    cefrLevel = c.getString("cefrLevel"),
                    titleEn = c.getString("titleEn"),
                    titleAr = c.getString("titleAr"),
                    durationMinutes = c.optInt("durationMinutes", 4),
                    speaker1Id = c.getString("speaker1Id"),
                    speaker2Id = c.getString("speaker2Id"),
                    speaker1Label = c.optString("speaker1Label", ""),
                    speaker2Label = c.optString("speaker2Label", ""),
                    turnsJson = c.optJSONArray("turns")?.toString() ?: "[]",
                    aiGenerated = c.optBoolean("aiGenerated", false)
                )
            )
        }
        return personas to conversations
    }

    companion object {
        private const val TAG = "AssetSeeder"
    }
}
