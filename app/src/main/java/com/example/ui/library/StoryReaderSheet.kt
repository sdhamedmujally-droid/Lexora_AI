package com.example.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Quiz
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.DualAccentAudioEngine
import com.example.data.local.StoryEntity
import com.example.ui.components.CefrLevelBadge
import com.example.ui.components.DualAccentButtons
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrBlock
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun StoryReaderScreen(
    story: StoryEntity,
    viewModel: LibraryViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean
) {
    BackHandler { viewModel.closeStory() }

    val generating by viewModel.generatingStory.collectAsStateWithLifecycle()
    val errorMsg by viewModel.libraryError.collectAsStateWithLifecycle()

    val paragraphs = remember(story.paragraphsJson) { viewModel.parseParagraphs(story) }
    val quiz = remember(story.quizJson) { viewModel.parseStoryQuiz(story) }

    var showArabic by remember { mutableStateOf(true) }
    var showQuiz by remember { mutableStateOf(false) }
    val selectedAnswers = remember(story.id) { mutableStateMapOf<Int, Int>() }
    var submitted by remember(story.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = viewModel::closeStory,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
            }
            Column(modifier = Modifier.weight(1f)) {
                LtrBlock {
                    Text(
                        text = story.titleEn,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${story.titleAr} • ${story.targetLines} سطراً",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CefrLevelBadge(level = story.cefrLevel)
        }

        if (errorMsg != null) {
            ErrorBanner(message = errorMsg!!)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = showArabic,
                onClick = { showArabic = !showArabic },
                leadingIcon = { Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(if (showArabic) "الترجمة ظاهرة" else "إظهار الترجمة") }
            )

            if (paragraphs.isNotEmpty()) {
                val fullText = paragraphs.joinToString(" ") { it.en }
                DualAccentButtons(
                    textToSpeak = fullText,
                    audioEngine = audioEngine,
                    compact = true,
                    tagPrefix = "story_full"
                )
            }
        }

        Button(
            onClick = { viewModel.generateFullStoryWithAi(story) },
            enabled = !generating,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("generate_story_ai_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            if (generating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("جارٍ تأليف القصة (${story.targetLines} سطراً) + 10 أسئلة...")
            } else {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (paragraphs.isEmpty()) "توليد نص القصة والاختبار (10 أسئلة) عبر Gemini"
                    else "إعادة صياغة قصة إبداعية جديدة لنفس العنوان"
                )
            }
        }

        if (paragraphs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LtrBlock {
                        Text(story.previewEn, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        text = if (arabicUi) "هذه القصة من الفهرس المصنف لـ 500 قصة. اضغط الزر أعلاه لتوليد النص الكامل (${story.targetLines} سطراً) مع اختبارها المكون من 10 أسئلة وحفظها محلياً على جهازك." else "Tap Generate above to build full story + 10-question quiz.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            paragraphs.forEachIndexed { idx, p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
                                text = "سطر ${idx + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            DualAccentButtons(
                                textToSpeak = p.en,
                                audioEngine = audioEngine,
                                compact = true,
                                tagPrefix = "story_line_$idx"
                            )
                        }
                        LtrBlock {
                            Text(
                                text = p.en,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (showArabic) {
                            Text(
                                text = p.ar,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (quiz.isNotEmpty()) {
                Button(
                    onClick = { showQuiz = !showQuiz },
                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.Quiz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (showQuiz) "إخفاء اختبار القصة"
                        else "اختبار القصة (${quiz.size} أسئلة بأنماط متعددة)"
                    )
                }
            }

            if (showQuiz && quiz.isNotEmpty()) {
                quiz.forEachIndexed { qIdx, q ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "سؤال ${qIdx + 1} · ${q.pattern}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            LtrBlock {
                                Text(
                                    text = q.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            q.options.forEachIndexed { optIdx, opt ->
                                val chosen = selectedAnswers[qIdx] == optIdx
                                val isCorrect = optIdx == q.correctIndex
                                val borderColor = when {
                                    submitted && isCorrect -> SuccessGreen
                                    submitted && chosen && !isCorrect -> ErrorRed
                                    chosen -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                }
                                OutlinedButton(
                                    onClick = { if (!submitted) selectedAnswers[qIdx] = optIdx },
                                    modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(if (chosen || (submitted && isCorrect)) 2.dp else 1.dp, borderColor)
                                ) {
                                    Text(opt, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                }
                            }
                            if (submitted) {
                                Text(
                                    text = q.explanationAr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (!submitted) {
                    Button(
                        onClick = {
                            submitted = true
                            val score = quiz.indices.count { i -> selectedAnswers[i] == quiz[i].correctIndex }
                            viewModel.submitStoryQuiz(story, score, quiz.size)
                        },
                        enabled = selectedAnswers.size == quiz.size,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("إنهاء واعتماد نتيجة القصة")
                    }
                } else {
                    val score = quiz.indices.count { i -> selectedAnswers[i] == quiz[i].correctIndex }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SuccessGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "نتيجتك في اختبار القصة: $score / ${quiz.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
