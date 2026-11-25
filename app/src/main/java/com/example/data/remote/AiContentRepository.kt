package com.example.data.remote

import android.util.Log
import com.example.data.local.ConversationEntity
import com.example.data.local.ConversationTurn
import com.example.data.local.LexisDao
import com.example.data.local.LexisJsonCodec
import com.example.data.local.StoryEntity
import com.example.data.local.StoryParagraph
import com.example.data.local.StoryQuizQuestion
import com.example.data.local.VoicePersonaEntity
import com.example.data.local.WordEntryEntity
import com.example.data.local.WordSense
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class AiContentRepository(
    private val dao: LexisDao
) {
    private val wordSchema = Schema.obj(
        properties = mapOf(
            "ipaUs" to Schema.string("General American IPA pronunciation inside slashes, e.g. /ˈækses/"),
            "ipaUk" to Schema.string("Received Pronunciation British IPA inside slashes, e.g. /ˈækses/"),
            "senses" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "pos" to Schema.string("Exact part of speech matching the input sense"),
                        "level" to Schema.string("Exact CEFR level matching the input sense"),
                        "arabic" to Schema.string("Accurate, natural Arabic translation tailored to this specific part of speech and CEFR level"),
                        "exampleEn" to Schema.string("Distinct, vivid English sentence demonstrating this exact grammatical part of speech"),
                        "exampleAr" to Schema.string("Natural Arabic translation of exampleEn")
                    )
                )
            )
        )
    )

    private val storySchema = Schema.obj(
        properties = mapOf(
            "paragraphs" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "en" to Schema.string("One vivid English story sentence/line"),
                        "ar" to Schema.string("Natural Arabic translation of that line")
                    )
                )
            ),
            "quiz" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "pattern" to Schema.string("One of: MAIN_IDEA, DETAIL, VOCABULARY, INFERENCE, GRAMMAR, TRUE_FALSE"),
                        "question" to Schema.string("Comprehension question testing the story"),
                        "options" to Schema.array(items = Schema.string()),
                        "correctIndex" to Schema.integer("0-based index of the correct option"),
                        "explanationAr" to Schema.string("Clear Arabic explanation why the answer is correct")
                    )
                )
            )
        )
    )

    private val conversationSchema = Schema.obj(
        properties = mapOf(
            "turns" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "speakerId" to Schema.string("Exact speaker ID provided in the prompt"),
                        "speakerName" to Schema.string("Speaker first name"),
                        "textEn" to Schema.string("Natural spoken English dialogue turn"),
                        "textAr" to Schema.string("Natural Arabic translation of the dialogue turn")
                    )
                )
            )
        )
    )

    private val wordModel by lazy {
        try { Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = wordSchema
            }
        ) } catch (_: Exception) { null }
    }

    private val storyModel by lazy {
        try { Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = storySchema
            }
        ) } catch (_: Exception) { null }
    }

    private val conversationModel by lazy {
        try { Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = conversationSchema
            }
        ) } catch (_: Exception) { null }
    }

    suspend fun enrichWord(word: WordEntryEntity): Result<WordEntryEntity> {
        val currentSenses = LexisJsonCodec.parseSenses(word.sensesJson)
        val senseDescription = currentSenses.joinToString("\n") {
            "- pos: ${it.pos} | level: ${it.level} | qualifier: ${it.qualifier}"
        }
        val prompt = """
            You are an expert English-Arabic lexicographer (Oxford Learner's Dictionary standard).
            Word: "${word.headword}"
            Senses to enrich:
            $senseDescription

            Requirements:
            1. Provide accurate US IPA and UK IPA.
            2. Return one entry in "senses" for EACH listed sense in the exact same order.
            3. Each sense must have an accurate Arabic meaning tailored to its grammatical type (noun/verb/adjective/adverb/etc.) and CEFR level (${word.lowestLevel}-${word.highestLevel}).
            4. Write a COMPLETELY DISTINCT, realistic contextual English sentence for each sense (never reuse the same scenario across senses) and its natural Arabic translation.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                wordModel?.generateContent(prompt)
                // wordMo?del.generateContent(prompt)
            }
        val raw = response?.text ?: throw IllegalStateException("Empty AI response for word enrichment.")
        //     val raw = respon?se.text ?: throw IllegalStateException("Empty AI response for word enrichment.")
            val json = JSONObject(raw)
            val ipaUs = json.optString("ipaUs", word.ipaUs)
            val ipaUk = json.optString("ipaUk", word.ipaUk)
            val arr = json.getJSONArray("senses")

            val merged = currentSenses.mapIndexed { idx, old ->
                val obj = if (idx < arr.length()) arr.getJSONObject(idx) else null
                if (obj == null) old else old.copy(
                    arabic = obj.optString("arabic", old.arabic).ifBlank { old.arabic },
                    exampleEn = obj.optString("exampleEn", old.exampleEn).ifBlank { old.exampleEn },
                    exampleAr = obj.optString("exampleAr", old.exampleAr).ifBlank { old.exampleAr },
                    enriched = true
                )
            }

            val encoded = LexisJsonCodec.encodeSenses(merged)
            dao.updateEnrichedWord(word.id, ipaUs, ipaUk, encoded)
            val updated = word.copy(
                ipaUs = ipaUs,
                ipaUk = ipaUk,
                sensesJson = encoded,
                enriched = true
            )
            Result.success(updated)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Word enrichment timed out", e)
            Result.failure(Exception("انتهت مهلة الذكاء الاصطناعي (60 ثانية). حاول مرة أخرى."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Word enrichment failed for ${word.headword}", e)
            Result.failure(e)
        }
    }

    suspend fun generateStoryAndQuiz(story: StoryEntity): Result<StoryEntity> {
        val prompt = """
            Write an original, captivating English graded story and a 10-question multi-pattern quiz.
            Story Title: "${story.titleEn}" (${story.titleAr})
            Theme: ${story.theme}
            CEFR Level: ${story.cefrLevel}
            Exact number of story lines/sentences: ${story.targetLines}

            Requirements:
            1. Write exactly ${story.targetLines} cohesive, engaging English lines in "paragraphs", calibrated strictly to CEFR ${story.cefrLevel} vocabulary and grammar.
            2. Provide an eloquent, accurate Arabic translation for every line.
            3. Create EXACTLY 10 distinct comprehension questions in "quiz" covering varied patterns (MAIN_IDEA, DETAIL, VOCABULARY, INFERENCE, GRAMMAR, TRUE_FALSE). Each question must have 4 options (2 for TRUE_FALSE or 4 with distractors), accurate correctIndex (0-based), and a concise Arabic explanation.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                storyModel?.generateContent(prompt)
                // storyModel.generateContent(prompt)
            }
            val raw = response?.text ?: throw IllegalStateException("Empty AI response for story generation.")
            // val raw = response.text ?: throw IllegalStateException("Empty AI response for story generation.")
            val json = JSONObject(raw)
            val pArr = json.getJSONArray("paragraphs")
            val paragraphs = List(pArr.length()) { i ->
                val o = pArr.getJSONObject(i)
                StoryParagraph(
                    en = o.getString("en"),
                    ar = o.getString("ar")
                )
            }
            val qArr = json.getJSONArray("quiz")
            val quiz = List(qArr.length()) { i ->
                val o = qArr.getJSONObject(i)
                val optsJson = o.getJSONArray("options")
                val opts = List(optsJson.length()) { j -> optsJson.getString(j) }
                StoryQuizQuestion(
                    pattern = o.optString("pattern", "COMPREHENSION"),
                    question = o.getString("question"),
                    options = opts,
                    correctIndex = o.optInt("correctIndex", 0).coerceIn(0, (opts.size - 1).coerceAtLeast(0)),
                    explanationAr = o.optString("explanationAr", "")
                )
            }

            if (paragraphs.isEmpty() || quiz.isEmpty()) {
                throw IllegalStateException("Generated story payload was incomplete.")
            }

            val pJson = LexisJsonCodec.encodeStoryParagraphs(paragraphs)
            val qJson = LexisJsonCodec.encodeStoryQuiz(quiz)
            dao.updateStoryContent(story.id, pJson, qJson)
            Result.success(story.copy(paragraphsJson = pJson, quizJson = qJson, aiGenerated = true))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Story generation timed out", e)
            Result.failure(Exception("انتهت مهلة توليد القصة (60 ثانية). حاول مرة أخرى."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Story generation failed for ${story.id}", e)
            Result.failure(e)
        }
    }

    suspend fun generateConversation(
        conv: ConversationEntity,
        speaker1: VoicePersonaEntity?,
        speaker2: VoicePersonaEntity?
    ): Result<ConversationEntity> {
        val targetTurns = when (conv.durationMinutes) {
            in 0..5 -> 16
            in 6..8 -> 22
            in 9..11 -> 28
            else -> 34
        }
        val s1Id = speaker1?.id ?: conv.speaker1Id
        val s1Name = speaker1?.name ?: "Speaker A"
        val s2Id = speaker2?.id ?: conv.speaker2Id
        val s2Name = speaker2?.name ?: "Speaker B"

        val prompt = """
            Write an authentic, natural two-person spoken English listening conversation.
            Topic: "${conv.titleEn}" (${conv.titleAr})
            CEFR Level: ${conv.cefrLevel}
            Target Listening Duration Tier: ${conv.durationMinutes} minutes (generate ~$targetTurns rich, natural conversational turns)
            Speaker 1: id="$s1Id", name="$s1Name", profile="${speaker1?.gender ?: "ADULT"} (${speaker1?.accent ?: "US"}), role=${speaker1?.role ?: "Host"}"
            Speaker 2: id="$s2Id", name="$s2Name", profile="${speaker2?.gender ?: "ADULT"} (${speaker2?.accent ?: "UK"}), role=${speaker2?.role ?: "Guest"}"

            Requirements:
            1. Alternate naturally between "$s1Id" ($s1Name) and "$s2Id" ($s2Name).
            2. Match CEFR ${conv.cefrLevel} realism: clear everyday phrasing at A1/A2, richer idioms, follow-up nuances, and natural discourse markers at B1/B2/C1.
            3. Provide full Arabic translation ("textAr") for every turn.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                conversationModel?.generateContent(prompt)
                // conversationModel.generateContent(prompt)
            }
            val raw = response?.text ?: throw IllegalStateException("Empty AI response for conversation generation.")
            // val raw = response.text ?: throw IllegalStateException("Empty AI response for conversation generation.")
            val json = JSONObject(raw)
            val tArr = json.getJSONArray("turns")
            val turns = List(tArr.length()) { i ->
                val o = tArr.getJSONObject(i)
                ConversationTurn(
                    speakerId = o.optString("speakerId", if (i % 2 == 0) s1Id else s2Id),
                    speakerName = o.optString("speakerName", if (i % 2 == 0) s1Name else s2Name),
                    textEn = o.getString("textEn"),
                    textAr = o.getString("textAr")
                )
            }
            if (turns.isEmpty()) {
                throw IllegalStateException("Generated conversation contained zero turns.")
            }
            val encoded = LexisJsonCodec.encodeConversationTurns(turns)
            dao.updateConversationTurns(conv.id, encoded)
            Result.success(conv.copy(turnsJson = encoded, aiGenerated = true))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Conversation generation timed out", e)
            Result.failure(Exception("انتهت مهلة توليد المحادثة (60 ثانية). حاول مرة أخرى."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Conversation generation failed for ${conv.id}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AiContentRepo"
    }
}
