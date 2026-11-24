package com.example.ui.practice

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.DualAccentAudioEngine
import com.example.data.remote.Sm2Quality
import com.example.ui.components.CefrLevelBadge
import com.example.ui.components.DualAccentButtons
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrBlock
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun PracticeHubScreen(
    viewModel: PracticeViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val errorMsg by viewModel.practiceError.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                modifier = Modifier.testTag("tab_flashcards")
            ) {
                Text(if (arabicUi) "البطاقات الذكية (SM-2)" else "Smart Flashcards")
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("tab_quiz_center")
            ) {
                Text(if (arabicUi) "مركز الاختبارات (5 أنماط)" else "Quiz Center (5 Modes)")
            }
        }

        if (errorMsg != null) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message = errorMsg!!)
        }

        Spacer(Modifier.height(10.dp))

        if (selectedTab == 0) {
            FlashcardsSection(
                viewModel = viewModel,
                audioEngine = audioEngine,
                arabicUi = arabicUi,
                modifier = Modifier.weight(1f)
            )
        } else {
            QuizCenterSection(
                viewModel = viewModel,
                audioEngine = audioEngine,
                arabicUi = arabicUi,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FlashcardsSection(
    viewModel: PracticeViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    val deck by viewModel.flashcards.collectAsStateWithLifecycle()
    val index by viewModel.flashcardIndex.collectAsStateWithLifecycle()
    val flipped by viewModel.flashcardFlipped.collectAsStateWithLifecycle()
    val mode by viewModel.flashcardMode.collectAsStateWithLifecycle()
    val level by viewModel.flashcardLevel.collectAsStateWithLifecycle()
    val reviewedCount by viewModel.sessionReviewed.collectAsStateWithLifecycle()

    val item = deck.getOrNull(index)
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(320),
        label = "flashcardFlip"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlashcardMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { viewModel.setFlashcardMode(m) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (m == FlashcardMode.VISUAL) Icons.Rounded.Visibility else Icons.Rounded.Hearing,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(if (arabicUi) m.labelAr else m.labelEn) }
                )
            }
            listOf("ALL", "A1", "A2", "B1", "B2", "C1").forEach { lvl ->
                FilterChip(
                    selected = level == lvl,
                    onClick = { viewModel.loadFlashcardDeck(lvl) },
                    label = { Text(if (lvl == "ALL") (if (arabicUi) "كل المستويات" else "All") else lvl) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (arabicUi) "البطاقة ${index + 1} من ${deck.size.coerceAtLeast(1)}" else "Card ${index + 1} / ${deck.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (arabicUi) "تمت مراجعتها الآن: $reviewedCount" else "Session reviewed: $reviewedCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (item != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12 * density
                    }
                    .testTag("flashcard_card")
                    .clickable { viewModel.flipCard() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (rotation <= 90f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            ) {
                if (rotation <= 90f) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CefrLevelBadge(level = item.word.lowestLevel)
                        Spacer(Modifier.height(14.dp))
                        if (mode == FlashcardMode.VISUAL) {
                            LtrBlock {
                                Text(
                                    text = item.word.headword,
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "${item.senses.firstOrNull()?.posAr.orEmpty()} (${item.senses.firstOrNull()?.pos.orEmpty()})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = if (arabicUi) "استمع جيداً ثم اقلب البطاقة للتحقق" else "Listen carefully & flip to verify",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        DualAccentButtons(
                            textToSpeak = item.word.headword,
                            audioEngine = audioEngine,
                            compact = true,
                            tagPrefix = "flashcard"
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.Flip, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (arabicUi) "اضغط لقلب البطاقة" else "Tap to flip",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LtrBlock {
                            Text(
                                text = item.word.headword,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        item.senses.take(3).forEach { s ->
                            Text(
                                text = "• [${s.posAr}] ${s.arabic}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            LtrBlock {
                                Text(
                                    text = s.exampleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        DualAccentButtons(
                            textToSpeak = item.senses.firstOrNull()?.exampleEn ?: item.word.headword,
                            audioEngine = audioEngine,
                            compact = true,
                            tagPrefix = "flashcard_back"
                        )
                    }
                }
            }

            Text(
                text = if (arabicUi) "قيّم مدى إتقانك للمفردة (التكرار المتباعد SM-2):" else "Rate your recall (SM-2 Spaced Repetition):",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RatingBtn(
                    label = if (arabicUi) "أعدها" else "Again",
                    color = ErrorRed,
                    modifier = Modifier.weight(1f).testTag("rate_again"),
                    onClick = { viewModel.rateFlashcard(Sm2Quality.AGAIN) }
                )
                RatingBtn(
                    label = if (arabicUi) "صعبة" else "Hard",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f).testTag("rate_hard"),
                    onClick = { viewModel.rateFlashcard(Sm2Quality.HARD) }
                )
                RatingBtn(
                    label = if (arabicUi) "جيدة" else "Good",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).testTag("rate_good"),
                    onClick = { viewModel.rateFlashcard(Sm2Quality.GOOD) }
                )
                RatingBtn(
                    label = if (arabicUi) "سهلة" else "Easy",
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f).testTag("rate_easy"),
                    onClick = { viewModel.rateFlashcard(Sm2Quality.EASY) }
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RatingBtn(
    label: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize().height(48.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizCenterSection(
    viewModel: PracticeViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    val state by viewModel.quizState.collectAsStateWithLifecycle()

    if (!state.active) {
        var selectedMode by remember { mutableStateOf(QuizMode.MCQ) }
        var selectedDifficulty by remember { mutableStateOf(QuizDifficulty.BEGINNER) }
        var selectedLevel by remember { mutableStateOf("ALL") }
        var selectedSeconds by remember { mutableIntStateOf(20) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (arabicUi) "1. اختر نمط الاختبار التفاعلي (5 أنماط)" else "1. Select Quiz Mode (5 Modes)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            QuizMode.entries.forEach { mode ->
                val isSelected = selectedMode == mode
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quiz_mode_${mode.name}")
                        .clickable { selectedMode = mode },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = mode.labelAr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = mode.descAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = if (arabicUi) "2. مستوى الصعوبة والمستوى اللغوي" else "2. Difficulty & CEFR Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuizDifficulty.entries.forEach { diff ->
                    FilterChip(
                        selected = selectedDifficulty == diff,
                        onClick = { selectedDifficulty = diff },
                        label = { Text(diff.labelAr) }
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "A1", "A2", "B1", "B2", "C1").forEach { lvl ->
                    FilterChip(
                        selected = selectedLevel == lvl,
                        onClick = { selectedLevel = lvl },
                        label = { Text(if (lvl == "ALL") "كل المستويات" else lvl) }
                    )
                }
            }

            Text(
                text = if (arabicUi) "3. التحكم الكامل بوقت السؤال" else "3. Per-Question Timer Control",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0, 10, 20, 30, 45, 60).forEach { sec ->
                    FilterChip(
                        selected = selectedSeconds == sec,
                        onClick = { selectedSeconds = sec },
                        leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text(if (sec == 0) "بدون مؤقت" else "$sec ثانية") }
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.configureAndStartQuiz(
                        mode = selectedMode,
                        difficulty = selectedDifficulty,
                        cefrFilter = selectedLevel,
                        secondsPerQuestion = selectedSeconds
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .height(54.dp)
                    .testTag("start_quiz_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (arabicUi) "ابدأ الاختبار الآن (10 أسئلة)" else "Start 10-Question Quiz",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    } else if (!state.finished) {
        val q = state.questions.getOrNull(state.currentIndex) ?: return
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "السؤال ${state.currentIndex + 1} / ${state.questions.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.secondsPerQuestion > 0) {
                        Surface(
                            color = if (state.remainingSeconds <= 5) ErrorRed.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "${state.remainingSeconds} ث",
                                color = if (state.remainingSeconds <= 5) ErrorRed else MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        IconButton(onClick = viewModel::toggleQuizPause) {
                            Icon(
                                imageVector = if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (state.paused) "استئناف الوقت" else "إيقاف الوقت مؤقتاً"
                            )
                        }
                    }
                    IconButton(onClick = viewModel::exitQuiz) {
                        Icon(Icons.Rounded.Close, contentDescription = "إنهاء الاختبار")
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (state.currentIndex + 1f) / state.questions.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = q.promptAr,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LtrBlock {
                        Text(
                            text = q.promptEn,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!q.audioText.isNullOrBlank()) {
                        DualAccentButtons(
                            textToSpeak = q.audioText,
                            audioEngine = audioEngine,
                            compact = true,
                            tagPrefix = "quiz_q_${q.index}"
                        )
                    }
                }
            }

            if (state.mode == QuizMode.SCRAMBLE) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LtrBlock {
                            Text(
                                text = state.scrambleBuilt.ifBlank { "_ ".repeat(q.correctAnswerText.length) },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = viewModel::clearScrambleBuilt) {
                            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "إعادة ترتيب الحروف")
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.scrambleLetters.forEachIndexed { idx, ch ->
                        OutlinedButton(
                            onClick = { viewModel.tapScrambleLetter(idx) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(ch.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = viewModel::submitScrambleAnswer,
                    enabled = state.scrambleBuilt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("تحقق من الإجابة")
                }
            } else {
                q.options.forEachIndexed { idx, opt ->
                    OutlinedButton(
                        onClick = { viewModel.selectQuizOption(idx) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("quiz_option_$idx"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    } else {
        // Post-Quiz Complete Answer Review
        val score = state.reviews.count { it.isCorrect }
        val total = state.reviews.size.coerceAtLeast(1)
        val pct = (score * 100) / total

        Column(modifier = modifier.fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (arabicUi) "النتيجة النهائية: $score / $total ($pct%)" else "Final Score: $score / $total ($pct%)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "الزمن المستغرق: ${state.totalElapsedSeconds} ثانية • ${state.mode.labelAr}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = viewModel::exitQuiz, shape = RoundedCornerShape(12.dp)) {
                        Text(if (arabicUi) "اختبار جديد" else "New Quiz")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = if (arabicUi) "مراجعة تفصيلية للإجابات:" else "Detailed Answer Review:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(state.reviews) { i, rev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            if (rev.isCorrect) SuccessGreen.copy(alpha = 0.5f) else ErrorRed.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (rev.isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = if (rev.isCorrect) SuccessGreen else ErrorRed
                                )
                                Text(
                                    text = "سؤال ${i + 1}: ${rev.question.word.headword}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "الإجابة الصحيحة: ${rev.question.correctAnswerText}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SuccessGreen
                            )
                            if (rev.timedOut) {
                                Text("انتهى وقت السؤال", style = MaterialTheme.typography.labelLarge, color = ErrorRed)
                            }
                            Text(
                                text = rev.question.explanationAr,
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
