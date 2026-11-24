package com.example.ui.dictionary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.DualAccentAudioEngine
import com.example.data.local.WordSense
import com.example.ui.components.CefrLevelBadge
import com.example.ui.components.DualAccentButtons
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrBlock
import com.example.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailSheet(
    item: DictionaryItemUi,
    audioEngine: DualAccentAudioEngine,
    enriching: Boolean,
    errorMessage: String?,
    arabicUi: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEnrichAi: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isFav = item.progress?.favorite == true

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    LtrBlock {
                        Text(
                            text = item.word.headword,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (item.word.ipaUs.isNotBlank() || item.word.ipaUk.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        LtrBlock {
                            Text(
                                text = "US ${item.word.ipaUs}  •  UK ${item.word.ipaUk}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("detail_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "حفظ في المفضلة",
                        tint = if (isFav) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            DualAccentButtons(
                textToSpeak = item.word.headword,
                audioEngine = audioEngine,
                compact = false,
                tagPrefix = "detail_headword"
            )

            if (errorMessage != null) {
                ErrorBanner(message = errorMessage)
            }

            Button(
                onClick = onEnrichAi,
                enabled = !enriching,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("ai_enrich_word_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (enriching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (arabicUi) "جارٍ صياغة الترجمة المتخصصة والأمثلة..."
                        else "Generating bespoke translations & examples..."
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (arabicUi) "إثراء فوري بترجمة دقيقة وأمثلة متخصصة + IPA"
                        else "Enrich IPA, Sense Meanings & Distinct Examples"
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Text(
                text = if (arabicUi) {
                    "المعاني والأنواع النحوية المجمّعة (${item.senses.size})"
                } else {
                    "Grouped Grammatical Senses (${item.senses.size})"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            item.senses.forEachIndexed { index, sense ->
                SenseCard(
                    index = index + 1,
                    sense = sense,
                    audioEngine = audioEngine,
                    arabicUi = arabicUi
                )
            }
        }
    }
}

@Composable
private fun SenseCard(
    index: Int,
    sense: WordSense,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#$index · ${sense.posAr} (${sense.pos})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    CefrLevelBadge(level = sense.level)
                }

                if (sense.qualifier.isNotBlank()) {
                    LtrBlock {
                        Text(
                            text = sense.qualifier,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = sense.arabic,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (arabicUi) "جملة سياقية لهذا النوع النحوي:" else "Contextual Example:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LtrBlock {
                        Text(
                            text = sense.exampleEn,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = sense.exampleAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    DualAccentButtons(
                        textToSpeak = sense.exampleEn,
                        audioEngine = audioEngine,
                        compact = true,
                        tagPrefix = "sense_$index"
                    )
                }
            }
        }
    }
}
