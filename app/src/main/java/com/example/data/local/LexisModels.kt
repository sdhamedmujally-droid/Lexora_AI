package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class WordSense(
    val pos: String,
    val posAr: String,
    val level: String,
    val qualifier: String,
    val arabic: String,
    val exampleEn: String,
    val exampleAr: String,
    val enriched: Boolean = false
)

data class GrammarExample(
    val en: String,
    val ar: String
)

data class GrammarQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationAr: String
)

data class StoryParagraph(
    val en: String,
    val ar: String
)

data class StoryQuizQuestion(
    val pattern: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationAr: String
)

data class ConversationTurn(
    val speakerId: String,
    val speakerName: String,
    val textEn: String,
    val textAr: String
)

@Entity(tableName = "words")
data class WordEntryEntity(
    @PrimaryKey val id: String,
    val headword: String,
    val headwordLower: String,
    val lowestLevel: String,
    val highestLevel: String,
    val ipaUs: String,
    val ipaUk: String,
    val sensesJson: String,
    val searchText: String,
    val senseCount: Int,
    val enriched: Boolean
)

@Entity(tableName = "verbs")
data class VerbEntity(
    @PrimaryKey val id: String,
    val v1: String,
    val v2: String,
    val v3: String,
    val cefrLevel: String,
    val irregular: Boolean,
    val arabic: String,
    val exampleV1: String,
    val exampleV2: String,
    val exampleV3: String
)

@Entity(tableName = "grammar_lessons")
data class GrammarLessonEntity(
    @PrimaryKey val id: String,
    val cefrLevel: String,
    val orderIndex: Int,
    val titleEn: String,
    val titleAr: String,
    val summaryAr: String,
    val explanationAr: String,
    val formula: String,
    val commonMistakeAr: String,
    val examplesJson: String,
    val quizJson: String
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val number: Int,
    val cefrLevel: String,
    val titleEn: String,
    val titleAr: String,
    val theme: String,
    val targetLines: Int,
    val estimatedMinutes: Int,
    val previewEn: String,
    val paragraphsJson: String,
    val quizJson: String,
    val aiGenerated: Boolean
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val number: Int,
    val cefrLevel: String,
    val titleEn: String,
    val titleAr: String,
    val durationMinutes: Int,
    val speaker1Id: String,
    val speaker2Id: String,
    val speaker1Label: String,
    val speaker2Label: String,
    val turnsJson: String,
    val aiGenerated: Boolean
)

@Entity(tableName = "voice_personas")
data class VoicePersonaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameAr: String,
    val gender: String,
    val accent: String,
    val pitch: Float,
    val rate: Float,
    val role: String
)

object LexisJsonCodec {

    fun parseSenses(json: String): List<WordSense> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            WordSense(
                pos = obj.optString("pos", ""),
                posAr = obj.optString("posAr", ""),
                level = obj.optString("level", "A1"),
                qualifier = obj.optString("qualifier", ""),
                arabic = obj.optString("arabic", ""),
                exampleEn = obj.optString("exampleEn", ""),
                exampleAr = obj.optString("exampleAr", ""),
                enriched = obj.optBoolean("enriched", false)
            )
        }
    }

    fun encodeSenses(senses: List<WordSense>): String {
        val arr = JSONArray()
        senses.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("pos", s.pos)
                    put("posAr", s.posAr)
                    put("level", s.level)
                    put("qualifier", s.qualifier)
                    put("arabic", s.arabic)
                    put("exampleEn", s.exampleEn)
                    put("exampleAr", s.exampleAr)
                    put("enriched", s.enriched)
                }
            )
        }
        return arr.toString()
    }

    fun parseGrammarExamples(json: String): List<GrammarExample> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            GrammarExample(
                en = obj.optString("en", ""),
                ar = obj.optString("ar", "")
            )
        }
    }

    fun parseGrammarQuiz(json: String): List<GrammarQuizQuestion> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            val optsArr = obj.optJSONArray("options") ?: JSONArray()
            val opts = List(optsArr.length()) { i -> optsArr.optString(i, "") }
            GrammarQuizQuestion(
                question = obj.optString("question", ""),
                options = opts,
                correctIndex = obj.optInt("correctIndex", 0),
                explanationAr = obj.optString("explanationAr", "")
            )
        }
    }

    fun parseStoryParagraphs(json: String): List<StoryParagraph> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            StoryParagraph(
                en = obj.optString("en", ""),
                ar = obj.optString("ar", "")
            )
        }
    }

    fun encodeStoryParagraphs(items: List<StoryParagraph>): String {
        val arr = JSONArray()
        items.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("en", p.en)
                    put("ar", p.ar)
                }
            )
        }
        return arr.toString()
    }

    fun parseStoryQuiz(json: String): List<StoryQuizQuestion> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            val optsArr = obj.optJSONArray("options") ?: JSONArray()
            val opts = List(optsArr.length()) { i -> optsArr.optString(i, "") }
            StoryQuizQuestion(
                pattern = obj.optString("pattern", "COMPREHENSION"),
                question = obj.optString("question", ""),
                options = opts,
                correctIndex = obj.optInt("correctIndex", 0),
                explanationAr = obj.optString("explanationAr", "")
            )
        }
    }

    fun encodeStoryQuiz(items: List<StoryQuizQuestion>): String {
        val arr = JSONArray()
        items.forEach { q ->
            arr.put(
                JSONObject().apply {
                    put("pattern", q.pattern)
                    put("question", q.question)
                    put("options", JSONArray(q.options))
                    put("correctIndex", q.correctIndex)
                    put("explanationAr", q.explanationAr)
                }
            )
        }
        return arr.toString()
    }

    fun parseConversationTurns(json: String): List<ConversationTurn> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return List(arr.length()) { idx ->
            val obj = arr.getJSONObject(idx)
            ConversationTurn(
                speakerId = obj.optString("speakerId", ""),
                speakerName = obj.optString("speakerName", ""),
                textEn = obj.optString("textEn", ""),
                textAr = obj.optString("textAr", "")
            )
        }
    }

    fun encodeConversationTurns(turns: List<ConversationTurn>): String {
        val arr = JSONArray()
        turns.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("speakerId", t.speakerId)
                    put("speakerName", t.speakerName)
                    put("textEn", t.textEn)
                    put("textAr", t.textAr)
                }
            )
        }
        return arr.toString()
    }
}
