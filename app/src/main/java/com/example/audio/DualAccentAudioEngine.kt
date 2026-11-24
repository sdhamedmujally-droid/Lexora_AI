package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.data.local.ConversationTurn
import com.example.data.local.VoicePersonaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class AudioPlaybackState(
    val ready: Boolean = false,
    val speaking: Boolean = false,
    val activeAccent: String = "US",
    val activeUtteranceId: String? = null,
    val conversationTurnIndex: Int = -1,
    val conversationPlaying: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val shadowingPauseMs: Long = 0L
)

class DualAccentAudioEngine(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private var usMaleVoice: Voice? = null
    private var usFemaleVoice: Voice? = null
    private var ukMaleVoice: Voice? = null
    private var ukFemaleVoice: Voice? = null

    private var queueTurns: List<ConversationTurn> = emptyList()
    private var queuePersonas: Map<String, VoicePersonaEntity> = emptyMap()
    private var queueIndex: Int = 0
    private var queueActive: Boolean = false

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                discoverVoices()
                registerListener()
                _state.value = _state.value.copy(ready = true)
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status=$status")
            }
        }
    }

    private fun discoverVoices() {
        val engine = tts ?: return
        val voices = runCatching { engine.voices ?: emptySet() }.getOrDefault(emptySet())
        val usVoices = voices.filter { it.locale.language == "en" && it.locale.country == "US" }
            .sortedBy { it.latency }
        val ukVoices = voices.filter { it.locale.language == "en" && it.locale.country == "GB" }
            .sortedBy { it.latency }

        usFemaleVoice = usVoices.firstOrNull {
            it.name.contains("female", ignoreCase = true) ||
                it.name.contains("sfg", ignoreCase = true) ||
                it.name.contains("tpf", ignoreCase = true) ||
                it.name.contains("iol", ignoreCase = true)
        } ?: usVoices.firstOrNull()

        usMaleVoice = usVoices.firstOrNull {
            it.name.contains("male", ignoreCase = true) && !it.name.contains("female", ignoreCase = true) ||
                it.name.contains("iom", ignoreCase = true) ||
                it.name.contains("sfb", ignoreCase = true)
        } ?: usVoices.lastOrNull() ?: usFemaleVoice

        ukFemaleVoice = ukVoices.firstOrNull {
            it.name.contains("female", ignoreCase = true) ||
                it.name.contains("gba", ignoreCase = true) ||
                it.name.contains("gbc", ignoreCase = true) ||
                it.name.contains("rjs", ignoreCase = true)
        } ?: ukVoices.firstOrNull()

        ukMaleVoice = ukVoices.firstOrNull {
            it.name.contains("male", ignoreCase = true) && !it.name.contains("female", ignoreCase = true) ||
                it.name.contains("gbb", ignoreCase = true) ||
                it.name.contains("gbd", ignoreCase = true)
        } ?: ukVoices.lastOrNull() ?: ukFemaleVoice
    }

    private fun registerListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.value = _state.value.copy(
                    speaking = true,
                    activeUtteranceId = utteranceId
                )
            }

            override fun onDone(utteranceId: String?) {
                if (queueActive && utteranceId?.startsWith("conv_") == true) {
                    val next = queueIndex + 1
                    if (next < queueTurns.size) {
                        queueIndex = next
                        playCurrentQueueTurn()
                        return
                    } else {
                        queueActive = false
                        _state.value = _state.value.copy(
                            speaking = false,
                            conversationPlaying = false,
                            conversationTurnIndex = -1,
                            activeUtteranceId = null
                        )
                        return
                    }
                }
                _state.value = _state.value.copy(speaking = false, activeUtteranceId = null)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                queueActive = false
                _state.value = _state.value.copy(
                    speaking = false,
                    conversationPlaying = false,
                    activeUtteranceId = null
                )
            }
        })
    }

    fun speakUs(text: String, tag: String = "us_${UUID.randomUUID()}") {
        stopConversation()
        speakInternal(
            text = text,
            locale = Locale.US,
            voice = usFemaleVoice,
            pitch = 1.0f,
            rate = _state.value.playbackSpeed,
            utteranceId = tag,
            accentLabel = "US"
        )
    }

    fun speakUk(text: String, tag: String = "uk_${UUID.randomUUID()}") {
        stopConversation()
        speakInternal(
            text = text,
            locale = Locale.UK,
            voice = ukMaleVoice ?: ukFemaleVoice,
            pitch = 0.96f,
            rate = _state.value.playbackSpeed * 0.96f,
            utteranceId = tag,
            accentLabel = "UK"
        )
    }

    fun speakWithPersona(text: String, persona: VoicePersonaEntity?, tag: String = "persona_${UUID.randomUUID()}") {
        val accent = persona?.accent ?: "US"
        val gender = persona?.gender ?: "WOMAN"
        val locale = if (accent == "UK") Locale.UK else Locale.US
        val voice = when {
            accent == "UK" && gender == "MAN" -> ukMaleVoice ?: ukFemaleVoice
            accent == "UK" -> ukFemaleVoice ?: ukMaleVoice
            gender == "MAN" -> usMaleVoice ?: usFemaleVoice
            else -> usFemaleVoice ?: usMaleVoice
        }
        val pitch = (persona?.pitch ?: 1.0f).coerceIn(0.65f, 1.55f)
        val rate = ((persona?.rate ?: 1.0f) * _state.value.playbackSpeed).coerceIn(0.55f, 1.85f)
        speakInternal(
            text = text,
            locale = locale,
            voice = voice,
            pitch = pitch,
            rate = rate,
            utteranceId = tag,
            accentLabel = accent
        )
    }

    fun startConversation(
        turns: List<ConversationTurn>,
        personasById: Map<String, VoicePersonaEntity>,
        startIndex: Int = 0
    ) {
        if (turns.isEmpty()) return
        queueTurns = turns
        queuePersonas = personasById
        queueIndex = startIndex.coerceIn(0, turns.lastIndex)
        queueActive = true
        playCurrentQueueTurn()
    }

    fun stopConversation() {
        queueActive = false
        tts?.stop()
        _state.value = _state.value.copy(
            speaking = false,
            conversationPlaying = false,
            conversationTurnIndex = -1,
            activeUtteranceId = null
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed.coerceIn(0.6f, 1.6f))
    }

    fun setShadowingPause(pauseMs: Long) {
        _state.value = _state.value.copy(shadowingPauseMs = pauseMs.coerceIn(0L, 4000L))
    }

    private fun playCurrentQueueTurn() {
        val turn = queueTurns.getOrNull(queueIndex) ?: return
        val persona = queuePersonas[turn.speakerId]
        _state.value = _state.value.copy(
            conversationPlaying = true,
            conversationTurnIndex = queueIndex,
            activeAccent = persona?.accent ?: "US"
        )
        speakWithPersona(turn.textEn, persona, tag = "conv_$queueIndex")
    }

    private fun speakInternal(
        text: String,
        locale: Locale,
        voice: Voice?,
        pitch: Float,
        rate: Float,
        utteranceId: String,
        accentLabel: String
    ) {
        val engine = tts ?: return
        if (!_state.value.ready || text.isBlank()) return
        _state.value = _state.value.copy(activeAccent = accentLabel)
        engine.language = locale
        if (voice != null) {
            runCatching { engine.voice = voice }
        }
        engine.setPitch(pitch)
        engine.setSpeechRate(rate)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        if (queueActive && _state.value.shadowingPauseMs > 0L) {
            engine.playSilentUtterance(_state.value.shadowingPauseMs, TextToSpeech.QUEUE_FLUSH, "silence_$utteranceId")
            engine.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
        } else {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    fun shutdown() {
        queueActive = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        private const val TAG = "DualAccentAudioEngine"
    }
}
