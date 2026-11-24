package com.example.ui.dictionary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.DualAccentAudioEngine
import com.example.di.LexisUiState
import com.example.ui.components.CefrLevelBadge
import com.example.ui.components.DualAccentButtons
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrBlock
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun DictionaryScreen(
    viewModel: DictionaryViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedWord by viewModel.selectedWord.collectAsStateWithLifecycle()
    val enrichingWordId by viewModel.enrichingWordId.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val ui = state) {
            is LexisUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is LexisUiState.Error -> {
                Column(Modifier.padding(16.dp)) {
                    ErrorBanner(message = ui.message)
                }
            }

            is LexisUiState.Success -> {
                val data = ui.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = data.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dictionary_search_input"),
                        placeholder = {
                            Text(
                                if (arabicUi) "ابحث في 4,949 كلمة إنجليزية أو عربية..."
                                else "Search Oxford 5000 words or Arabic meanings..."
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = "بحث")
                        },
                        trailingIcon = {
                            if (data.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "مسح البحث")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val levels = listOf("ALL", "A1", "A2", "B1", "B2", "C1")
                        levels.forEach { lvl ->
                            FilterChip(
                                selected = data.selectedLevel == lvl,
                                onClick = { viewModel.setLevel(lvl) },
                                modifier = Modifier.testTag("dict_filter_$lvl"),
                                label = {
                                    Text(if (lvl == "ALL") (if (arabicUi) "الكل" else "All") else lvl)
                                }
                            )
                        }

                        FilterChip(
                            selected = data.onlyFavorites,
                            onClick = viewModel::toggleFavoritesFilter,
                            modifier = Modifier.testTag("dict_filter_favorites"),
                            leadingIcon = {
                                Icon(
                                    imageVector = if (data.onlyFavorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "المفضلة",
                                    tint = if (data.onlyFavorites) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    if (arabicUi) "المفضلة (${data.favoriteCount})"
                                    else "Favorites (${data.favoriteCount})"
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (arabicUi) "البطاقات المعروضة: ${data.totalFiltered}" else "Showing ${data.totalFiltered} cards",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (arabicUi) "المتقنة: ${data.masteredCount}" else "Mastered: ${data.masteredCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen
                        )
                    }

                    if (actionError != null) {
                        ErrorBanner(message = actionError!!, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    if (data.items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (arabicUi) "لا توجد كلمات مطابقة لهذا البحث" else "No matching words found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(data.items, key = { it.word.id }) { item ->
                                WordCardItem(
                                    item = item,
                                    audioEngine = audioEngine,
                                    arabicUi = arabicUi,
                                    onOpen = { viewModel.openWord(item) },
                                    onToggleFavorite = { viewModel.toggleFavorite(item) }
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedWord?.let { current ->
            WordDetailSheet(
                item = current,
                audioEngine = audioEngine,
                enriching = enrichingWordId == current.word.id,
                errorMessage = actionError,
                arabicUi = arabicUi,
                onDismiss = viewModel::closeWord,
                onToggleFavorite = { viewModel.toggleFavorite(current) },
                onEnrichAi = { viewModel.enrichSelectedWordWithAi(current) }
            )
        }
    }
}

@Composable
private fun WordCardItem(
    item: DictionaryItemUi,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isFav = item.progress?.favorite == true
    val primarySense = item.senses.firstOrNull()
    val levelsLabel = if (item.word.lowestLevel == item.word.highestLevel) {
        item.word.lowestLevel
    } else {
        "${item.word.lowestLevel}–${item.word.highestLevel}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_card_${item.word.id}")
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LtrBlock {
                        Text(
                            text = item.word.headword,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CefrLevelBadge(level = item.word.lowestLevel)
                    if (item.word.senseCount > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (arabicUi) "${item.word.senseCount} أنواع ($levelsLabel)" else "${item.word.senseCount} senses",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (item.word.enriched) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "مثرى بالكامل",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fav_btn_${item.word.id}")
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFav) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                        tint = if (isFav) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (primarySense != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${primarySense.posAr} (${primarySense.pos}): ${primarySense.arabic}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DualAccentButtons(
                    textToSpeak = item.word.headword,
                    audioEngine = audioEngine,
                    compact = true,
                    tagPrefix = "word_${item.word.id}"
                )

                Text(
                    text = if (arabicUi) "التفاصيل والأمثلة ←" else "Details & Examples →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
