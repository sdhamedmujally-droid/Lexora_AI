package com.example.ui.encyclopedia

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.VerbEntity
import com.example.ui.components.CefrLevelBadge
import com.example.ui.components.DualAccentButtons
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LtrBlock
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun EncyclopediaScreen(
    viewModel: EncyclopediaViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val selectedLesson by viewModel.selectedLesson.collectAsStateWithLifecycle()
    val errorMsg by viewModel.actionError.collectAsStateWithLifecycle()

    if (selectedLesson != null) {
        GrammarLessonDetailView(
            lesson = selectedLesson!!,
            audioEngine = audioEngine,
            arabicUi = arabicUi,
            onBack = viewModel::closeLesson,
            onQuizFinished = { score, total ->
                viewModel.submitGrammarQuizResult(selectedLesson!!, score, total)
            }
        )
        return
    }

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
                modifier = Modifier.testTag("tab_grammar")
            ) {
                Text(if (arabicUi) "موسوعة القواعد (A1–C1)" else "Grammar (A1–C1)")
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                modifier = Modifier.testTag("tab_verbs")
            ) {
                Text(if (arabicUi) "الأفعال 1,244 (3 تصاريف)" else "1,244 Verbs (V1/V2/V3)")
            }
        }

        if (errorMsg != null) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message = errorMsg!!)
        }

        Spacer(Modifier.height(10.dp))

        if (selectedTab == 0) {
            GrammarListTab(viewModel, arabicUi, Modifier.weight(1f))
        } else {
            VerbsListTab(viewModel, audioEngine, arabicUi, Modifier.weight(1f))
        }
    }
}

@Composable
private fun GrammarListTab(
    viewModel: EncyclopediaViewModel,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    val lessons by viewModel.grammarLessons.collectAsStateWithLifecycle()
    val levelFilter by viewModel.grammarLevel.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "A1", "A2", "B1", "B2", "C1").forEach { lvl ->
                FilterChip(
                    selected = levelFilter == lvl,
                    onClick = { viewModel.setGrammarLevel(lvl) },
                    label = { Text(if (lvl == "ALL") (if (arabicUi) "كل المستويات (35)" else "All (35)") else lvl) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(lessons, key = { it.entity.id }) { lesson ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grammar_card_${lesson.entity.id}")
                        .clickable { viewModel.openLesson(lesson) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CefrLevelBadge(level = lesson.entity.cefrLevel)
                                Text(
                                    text = lesson.entity.titleAr,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (lesson.completed) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "مكتمل",
                                    tint = SuccessGreen
                                )
                            }
                        }
                        LtrBlock {
                            Text(
                                text = lesson.entity.titleEn,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = lesson.entity.summaryAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (lesson.completed) "أفضل نتيجة اختبار: ${lesson.bestScorePct}%" else "يتضمن اختبار قاعدة تفاعلي",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (lesson.completed) SuccessGreen else MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (arabicUi) "افتح الدرس ←" else "Open Lesson →",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrammarLessonDetailView(
    lesson: GrammarLessonUi,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    onBack: () -> Unit,
    onQuizFinished: (Int, Int) -> Unit
) {
    BackHandler(onBack = onBack)

    var showQuiz by remember { mutableStateOf(false) }
    val selectedAnswers = remember { mutableStateMapOf<Int, Int>() }
    var submitted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "رجوع")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.entity.titleAr,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                LtrBlock {
                    Text(
                        text = lesson.entity.titleEn,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            CefrLevelBadge(level = lesson.entity.cefrLevel)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (arabicUi) "القاعدة التركيبية:" else "Grammar Formula:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                LtrBlock {
                    Text(
                        text = lesson.entity.formula,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (arabicUi) "الشرح المفصل:" else "Detailed Explanation:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = lesson.entity.explanationAr,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column {
                    Text(
                        text = if (arabicUi) "خطأ شائع احذر منه:" else "Common Mistake:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lesson.entity.commonMistakeAr,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Text(
            text = if (arabicUi) "أمثلة تطبيقية مع النطق:" else "Examples with Audio:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        lesson.examples.forEachIndexed { idx, ex ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LtrBlock {
                        Text(ex.en, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Text(ex.ar, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DualAccentButtons(ex.en, audioEngine, compact = true, tagPrefix = "gram_ex_$idx")
                }
            }
        }

        Button(
            onClick = { showQuiz = !showQuiz },
            modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.Quiz, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (showQuiz) "إخفاء اختبار القاعدة" else "ابدأ اختبار القاعدة (${lesson.quiz.size} أسئلة)")
        }

        if (showQuiz) {
            lesson.quiz.forEachIndexed { qIdx, q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${qIdx + 1}. ${q.question}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                        val score = lesson.quiz.indices.count { i -> selectedAnswers[i] == lesson.quiz[i].correctIndex }
                        onQuizFinished(score, lesson.quiz.size)
                    },
                    enabled = selectedAnswers.size == lesson.quiz.size,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("اعتماد إجابات القاعدة")
                }
            } else {
                val score = lesson.quiz.indices.count { i -> selectedAnswers[i] == lesson.quiz[i].correctIndex }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SuccessGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "أحسنت! نتيجتك في هذه القاعدة: $score / ${lesson.quiz.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun VerbsListTab(
    viewModel: EncyclopediaViewModel,
    audioEngine: DualAccentAudioEngine,
    arabicUi: Boolean,
    modifier: Modifier = Modifier
) {
    val verbs by viewModel.verbs.collectAsStateWithLifecycle()
    val query by viewModel.verbQuery.collectAsStateWithLifecycle()
    val level by viewModel.verbLevel.collectAsStateWithLifecycle()
    val onlyIrregular by viewModel.onlyIrregular.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setVerbQuery,
            modifier = Modifier.fillMaxWidth().testTag("verbs_search_input"),
            placeholder = {
                Text(if (arabicUi) "ابحث في 1,244 فعلاً (V1 / V2 / V3)..." else "Search 1,244 verbs...")
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
            FilterChip(
                selected = onlyIrregular,
                onClick = viewModel::toggleOnlyIrregular,
                label = { Text(if (arabicUi) "الأفعال الشاذة فقط" else "Irregular Only") }
            )
            listOf("ALL", "A1", "A2", "B1", "B2", "C1").forEach { lvl ->
                FilterChip(
                    selected = level == lvl,
                    onClick = { viewModel.setVerbLevel(lvl) },
                    label = { Text(if (lvl == "ALL") (if (arabicUi) "الكل" else "All") else lvl) }
                )
            }
        }

        Text(
            text = if (arabicUi) "الأفعال المعروضة: ${verbs.size} فعل (تصريف ثلاثي كامل)" else "Showing ${verbs.size} verbs",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(verbs, key = { it.id }) { verb ->
                VerbCard(
                    verb = verb,
                    audioEngine = audioEngine,
                    onPracticed = { viewModel.markVerbPracticed(verb) }
                )
            }
        }
    }
}

@Composable
private fun VerbCard(
    verb: VerbEntity,
    audioEngine: DualAccentAudioEngine,
    onPracticed: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CefrLevelBadge(level = verb.cefrLevel)
                    Surface(
                        color = if (verb.irregular) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (verb.irregular) "شاذ Irregular" else "قياسي Regular",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = verb.arabic,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LtrBlock {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerbFormPill("V1 (Base)", verb.v1, Modifier.weight(1f)) {
                        audioEngine.speakUs(verb.v1)
                        onPracticed()
                    }
                    VerbFormPill("V2 (Past)", verb.v2, Modifier.weight(1f)) {
                        audioEngine.speakUs(verb.v2)
                        onPracticed()
                    }
                    VerbFormPill("V3 (P.P.)", verb.v3, Modifier.weight(1f)) {
                        audioEngine.speakUs(verb.v3)
                        onPracticed()
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                LtrBlock {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("V1: ${verb.exampleV1}", style = MaterialTheme.typography.bodyMedium)
                        Text("V2: ${verb.exampleV2}", style = MaterialTheme.typography.bodyMedium)
                        Text("V3: ${verb.exampleV3}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                DualAccentButtons(
                    textToSpeak = "${verb.v1}, ${verb.v2}, ${verb.v3}",
                    audioEngine = audioEngine,
                    compact = true,
                    tagPrefix = "verb_${verb.id}"
                )
            }
        }
    }
}

@Composable
private fun VerbFormPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "نطق $value",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
