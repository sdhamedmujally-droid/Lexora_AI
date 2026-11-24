package com.example.ui.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.DualAccentAudioEngine
import com.example.data.local.ConversationEntity
import com.example.data.local.StoryEntity
import com.example.ui.components.CefrLevelBadge
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrBlock
import com.example.ui.theme.SuccessGreen

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val activeStory by viewModel.activeStory.collectAsStateWithLifecycle()
    val activeConv by viewModel.activeConversation.collectAsStateWithLifecycle()
    val errorMsg by viewModel.libraryError.collectAsStateWithLifecycle()

    if (activeStory != null) {
        StoryReaderScreen(
            story = activeStory!!,
            viewModel = viewModel,
            audioEngine = audioEngine,
            arabicUi = arabicUi
        )
        return
    }

    if (activeConv != null) {
        ConversationPlayerScreen(
            conversation = activeConv!!,
            viewModel = viewModel,
            audioEngine = audioEngine,
            arabicUi = arabicUi
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("tab_stories_500")
                ) {
                    Text(if (arabicUi) "500 قصة متدرجة" else "500 Graded Stories")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("tab_conversations_500")
                ) {
                    Text(if (arabicUi) "500 محادثة (22 صوتاً)" else "500 Dialogues (22 Voices)")
                }
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(8.dp))
                ErrorBanner(message = errorMsg!!)
            }

            Spacer(Modifier.height(10.dp))

            if (selectedTab == 0) {
                StoriesCatalogTab(viewModel, arabicUi, Modifier.weight(1f))
            } else {
                ConversationsCatalogTab(viewModel, arabicUi, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StoriesCatalogTab(
    viewModel: LibraryViewModel,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val level by viewModel.storyLevel.collectAsStateWithLifecycle()
    val query by viewModel.storyQuery.collectAsStateWithLifecycle()
    val progressMap by viewModel.contentProgressMap.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setStoryQuery,
            modifier = Modifier.fillMaxWidth().testTag("stories_search_input"),
            placeholder = {
                Text(if (arabicUi) "ابحث في 100 قصة لكل مستوى..." else "Search 100 stories per level...")
            },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("A1", "A2", "B1", "B2", "C1").forEach { lvl ->
                FilterChip(
                    selected = level == lvl,
                    onClick = { viewModel.setStoryLevel(lvl) },
                    modifier = Modifier.testTag("story_level_$lvl"),
                    label = { Text("$lvl (100)") }
                )
            }
        }

        Text(
            text = if (arabicUi) "المعروض في مستوى $level: ${stories.size} قصة • لكل قصة اختبار 10 أسئلة" else "$level Stories: ${stories.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(stories, key = { it.id }) { story ->
                val prog = progressMap[story.id]
                StoryCatalogCard(
                    story = story,
                    completed = prog?.completed == true,
                    bestScorePct = (prog?.bestScorePct ?: 0L).toInt(),
                    arabicUi = arabicUi,
                    onClick = { viewModel.openStory(story) }
                )
            }
        }
    }
}

@Composable
private fun StoryCatalogCard(
    story: StoryEntity,
    completed: Boolean,
    bestScorePct: Int,
    arabicUi: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("story_card_${story.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CefrLevelBadge(level = story.cefrLevel)
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#${story.number} · ${story.targetLines} سطر",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (story.aiGenerated) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "نص كامل جاهز",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (completed) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$bestScorePct%", style = MaterialTheme.typography.labelLarge, color = SuccessGreen)
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "مكتملة", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }

            LtrBlock {
                Text(
                    text = story.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = story.titleAr,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.AutoStories, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (arabicUi) "قراءة + استماع + 10 أسئلة" else "Read + Audio + 10Q Quiz",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${story.estimatedMinutes} د",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun ConversationsCatalogTab(
    viewModel: LibraryViewModel,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val personas by viewModel.personas.collectAsStateWithLifecycle()
    val level by viewModel.convLevel.collectAsStateWithLifecycle()
    val query by viewModel.convQuery.collectAsStateWithLifecycle()
    val personaFilter by viewModel.selectedPersonaFilter.collectAsStateWithLifecycle()
    val progressMap by viewModel.contentProgressMap.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setConvQuery,
            modifier = Modifier.fillMaxWidth().testTag("conversations_search_input"),
            placeholder = {
                Text(if (arabicUi) "ابحث في 500 محادثة (4–15 دقيقة)..." else "Search 500 conversations (4–15 min)...")
            },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("A1", "A2", "B1", "B2", "C1").forEach { lvl ->
                FilterChip(
                    selected = level == lvl,
                    onClick = { viewModel.setConvLevel(lvl) },
                    modifier = Modifier.testTag("conv_level_$lvl"),
                    label = { Text("$lvl (100)") }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = personaFilter == "ALL",
                onClick = { viewModel.setPersonaFilter("ALL") },
                label = { Text(if (arabicUi) "كل المتحدثين (22)" else "All 22 Voices") }
            )
            personas.forEach { p ->
                val tagIcon = when (p.gender) {
                    "CHILD" -> "طفل"
                    "WOMAN" -> "امرأة"
                    else -> "رجل"
                }
                FilterChip(
                    selected = personaFilter == p.id,
                    onClick = { viewModel.setPersonaFilter(p.id) },
                    label = { Text("${p.nameAr} ($tagIcon·${p.accent})") }
                )
            }
        }

        Text(
            text = if (arabicUi) "المحادثات المعروضة في $level: ${conversations.size}" else "$level Conversations: ${conversations.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(conversations, key = { it.id }) { conv ->
                val completed = progressMap[conv.id]?.completed == true
                ConversationCatalogCard(
                    conv = conv,
                    completed = completed,
                    onClick = { viewModel.openConversation(conv) }
                )
            }
        }
    }
}

@Composable
private fun ConversationCatalogCard(
    conv: ConversationEntity,
    completed: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conv_card_${conv.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CefrLevelBadge(level = conv.cefrLevel)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text("${conv.durationMinutes} دقيقة", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                if (completed) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "مكتملة", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                }
            }

            LtrBlock {
                Text(
                    text = conv.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(text = conv.titleAr, style = MaterialTheme.typography.bodyLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.tertiary)
                    LtrBlock {
                        Text(
                            text = "${conv.speaker1Label} ↔ ${conv.speaker2Label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
