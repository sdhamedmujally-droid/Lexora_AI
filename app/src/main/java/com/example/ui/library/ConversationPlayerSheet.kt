package com.example.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.DualAccentAudioEngine
import com.example.data.local.ConversationEntity
import com.example.ui.components.CefrLevelBadge
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrBlock

@Composable
fun ConversationPlayerScreen(
    conversation: ConversationEntity,
    viewModel: LibraryViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean
) {
    BackHandler {
        audioEngine.stopConversation()
        viewModel.closeConversation()
    }

    DisposableEffect(conversation.id) {
        onDispose { audioEngine.stopConversation() }
    }

    val personas by viewModel.personas.collectAsStateWithLifecycle()
    val personasById = remember(personas) { personas.associateBy { it.id } }
    val playback by audioEngine.state.collectAsStateWithLifecycle()
    val generating by viewModel.generatingConv.collectAsStateWithLifecycle()
    val errorMsg by viewModel.libraryError.collectAsStateWithLifecycle()

    val turns = remember(conversation.turnsJson) { viewModel.parseTurns(conversation) }
    var showArabic by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    audioEngine.stopConversation()
                    viewModel.closeConversation()
                },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
            }
            Column(modifier = Modifier.weight(1f)) {
                LtrBlock {
                    Text(
                        text = conversation.titleEn,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${conversation.titleAr} • ${conversation.durationMinutes} دقيقة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CefrLevelBadge(level = conversation.cefrLevel)
        }

        if (errorMsg != null) {
            ErrorBanner(message = errorMsg!!)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LtrBlock {
                        Text(
                            text = "${conversation.speaker1Label}  ⇄  ${conversation.speaker2Label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (playback.conversationPlaying) {
                                audioEngine.stopConversation()
                            } else {
                                audioEngine.startConversation(turns, personasById, 0)
                            }
                        },
                        enabled = turns.isNotEmpty(),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("conv_play_pause_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (playback.conversationPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (playback.conversationPlaying) "إيقاف" else "تشغيل الحوار كاملاً")
                    }

                    listOf(0.8f to "0.8x", 1.0f to "1.0x", 1.2f to "1.2x").forEach { (spd, lbl) ->
                        FilterChip(
                            selected = kotlin.math.abs(playback.playbackSpeed - spd) < 0.05f,
                            onClick = { audioEngine.setPlaybackSpeed(spd) },
                            label = { Text(lbl) }
                        )
                    }

                    FilterChip(
                        selected = playback.shadowingPauseMs > 0L,
                        onClick = {
                            audioEngine.setShadowingPause(if (playback.shadowingPauseMs > 0L) 0L else 2200L)
                        },
                        leadingIcon = { Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("تكرار التظليل") }
                    )

                    FilterChip(
                        selected = showArabic,
                        onClick = { showArabic = !showArabic },
                        leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(if (showArabic) "ترجمة" else "EN فقط") }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.generateFullConversationWithAi(conversation) },
                enabled = !generating,
                modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (generating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("جارٍ توليد سيناريو ${conversation.durationMinutes} د...")
                } else {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (turns.isEmpty()) "توليد الحوار عبر Gemini" else "سيناريو جديد (${conversation.durationMinutes} د)")
                }
            }

            OutlinedButton(
                onClick = { viewModel.markConversationCompleted(conversation) },
                modifier = Modifier.minimumInteractiveComponentSize(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (arabicUi) "أتممتها" else "Done")
            }
        }

        if (turns.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اضغط «توليد الحوار عبر Gemini» لبناء سيناريو حوار كامل بين ${conversation.speaker1Label} و ${conversation.speaker2Label} بطول مناسب لمستوى ${conversation.cefrLevel} (${conversation.durationMinutes} دقيقة).",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(turns) { idx, turn ->
                    val isSpeaker1 = turn.speakerId == conversation.speaker1Id
                    val isActive = playback.conversationPlaying && playback.conversationTurnIndex == idx
                    val persona = personasById[turn.speakerId]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                audioEngine.startConversation(turns, personasById, idx)
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isActive -> MaterialTheme.colorScheme.primaryContainer
                                isSpeaker1 -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ),
                        border = BorderStroke(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${turn.speakerName} (${persona?.accent ?: "US"} · ${persona?.role.orEmpty()})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { audioEngine.speakWithPersona(turn.textEn, persona, "turn_$idx") },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "استمع إلى ${turn.speakerName}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            LtrBlock {
                                Text(
                                    text = turn.textEn,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (showArabic) {
                                Text(
                                    text = turn.textAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
