package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ThemeMode
import com.example.data.remote.DailyActivity
import com.example.di.LexisUiState
import com.example.ui.components.CefrLevelBadge
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LexisHeroBanner
import com.example.ui.components.StatMetricTile
import com.example.ui.components.cefrColor
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen

@Composable
fun HomeStatsScreen(
    viewModel: StatsViewModel,
    onNavigateTab: (Int) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    when (val ui = state) {
        is LexisUiState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is LexisUiState.Error -> {
            Column(modifier.fillMaxSize().padding(16.dp)) {
                ErrorBanner(message = ui.message)
            }
        }

        is LexisUiState.Success -> {
            val data = ui.data
            val arabicUi = data.settings.arabicUi
            val profile = data.profile

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LexisHeroBanner(
                    title = if (arabicUi) "أهلاً ${profile.displayName} 👋" else "Welcome, ${profile.displayName}",
                    subtitle = if (arabicUi) "لوحة الإحصائيات الشاملة ومتابعة رحلة إتقان الإنجليزية" else "Comprehensive English mastery dashboard",
                    badge = if (arabicUi) "المستوى المستهدف: ${profile.preferredLevel} • سلسلة ${profile.streakDays} يوم" else "Target: ${profile.preferredLevel} • ${profile.streakDays}d streak"
                ) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("A1", "A2", "B1", "B2", "C1").forEach { lvl ->
                                val selected = profile.preferredLevel == lvl
                                Surface(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clickable { viewModel.setPreferredLevel(lvl) },
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = lvl,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "الإعدادات",
                                tint = Color.White
                            )
                        }
                    }
                }

                // 6-tile KPI Grid (2x3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricTile(
                        label = if (arabicUi) "كلمات متقنة" else "Mastered",
                        value = "${profile.wordsMasteredCount} / 4949",
                        icon = Icons.Rounded.CheckCircle,
                        tint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        label = if (arabicUi) "قيد التعلم" else "Learning",
                        value = "${profile.wordsLearningCount}",
                        icon = Icons.Rounded.Psychology,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricTile(
                        label = if (arabicUi) "سلسلة الأيام" else "Streak",
                        value = "${profile.streakDays} يوم",
                        icon = Icons.Rounded.LocalFireDepartment,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        label = if (arabicUi) "دقائق الدراسة" else "Minutes",
                        value = "${profile.totalStudyMinutes} د",
                        icon = Icons.Rounded.Schedule,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricTile(
                        label = if (arabicUi) "متوسط الاختبارات" else "Quiz Avg",
                        value = "${data.averageQuizPct}% (${profile.quizzesCompleted})",
                        icon = Icons.Rounded.Quiz,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricTile(
                        label = if (arabicUi) "المفضلة" else "Favorites",
                        value = "${data.favoriteWordsCount}",
                        icon = Icons.Rounded.Favorite,
                        tint = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Content Completion Summary Card
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
                        Text(
                            text = if (arabicUi) "تقدم المناهج والمحتوى التفاعلي" else "Curriculum & Library Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ProgressCounterRow(
                            label = if (arabicUi) "القصص المتدرجة المكتملة" else "Stories Completed",
                            valueText = "${profile.storiesCompleted} / 500",
                            fraction = (profile.storiesCompleted / 500f).coerceIn(0f, 1f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ProgressCounterRow(
                            label = if (arabicUi) "المحادثات الصوتية المكتملة" else "Conversations Completed",
                            valueText = "${profile.conversationsCompleted} / 500",
                            fraction = (profile.conversationsCompleted / 500f).coerceIn(0f, 1f),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        ProgressCounterRow(
                            label = if (arabicUi) "القواعد اللغوية المتقنة" else "Grammar Rules Mastered",
                            valueText = "${profile.grammarCompleted} / 35",
                            fraction = (profile.grammarCompleted / 35f).coerceIn(0f, 1f),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        ProgressCounterRow(
                            label = if (arabicUi) "الأفعال المتدرب على تصاريفها" else "Verbs Practiced",
                            valueText = "${profile.verbsPracticed} / 1244",
                            fraction = (profile.verbsPracticed / 1244f).coerceIn(0f, 1f),
                            color = SuccessGreen
                        )
                    }
                }

                // CEFR Vocabulary Breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (arabicUi) "توزيع المفردات النشطة حسب مستوى CEFR" else "Active Vocabulary by CEFR Level",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        listOf("A1", "A2", "B1", "B2", "C1").forEach { lvl ->
                            val learned = data.cefrTotalLearnedCounts[lvl] ?: 0
                            val mastered = data.cefrMasteredCounts[lvl] ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CefrLevelBadge(level = lvl)
                                LinearProgressIndicator(
                                    progress = { (learned / 100f).coerceIn(0.03f, 1f) },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                    color = cefrColor(lvl)
                                )
                                Text(
                                    text = "$mastered متقنة / $learned",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Canvas Activity Chart
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
                        Text(
                            text = if (arabicUi) "النشاط اليومي (الدقائق والهدف: ${data.settings.dailyGoalMinutes} د)" else "Daily Study Activity (Goal: ${data.settings.dailyGoalMinutes}m)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        ActivityBarChart(
                            activities = data.dailyActivities.takeLast(7),
                            goalMinutes = data.settings.dailyGoalMinutes,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Quick Jump Shortcuts
                Text(
                    text = if (arabicUi) "انطلاق سريع:" else "Quick Start:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = if (arabicUi) "القاموس" else "Dictionary",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        onClick = { onNavigateTab(1) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = if (arabicUi) "اختبارات" else "Practice",
                        icon = Icons.Rounded.Psychology,
                        onClick = { onNavigateTab(2) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = if (arabicUi) "القواعد" else "Grammar",
                        icon = Icons.Rounded.School,
                        onClick = { onNavigateTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        title = if (arabicUi) "القصص" else "Library",
                        icon = Icons.Rounded.AutoStories,
                        onClick = { onNavigateTab(4) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (data.recentQuizzes.isNotEmpty()) {
                    Text(
                        text = if (arabicUi) "أحدث الاختبارات المكتملة:" else "Recent Quiz History:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    data.recentQuizzes.take(5).forEach { attempt ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(attempt.topicTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${attempt.category} · ${attempt.cefrLevel}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "${attempt.score}/${attempt.totalQuestions}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            if (showSettings) {
                SettingsBottomSheet(
                    settings = data.settings,
                    onThemeChange = viewModel::setThemeMode,
                    onArabicUiChange = viewModel::setArabicUi,
                    onGoalChange = viewModel::setDailyGoalMinutes,
                    onSignOut = {
                        showSettings = false
                        onSignOut()
                    },
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

@Composable
private fun ProgressCounterRow(
    label: String,
    valueText: String,
    fraction: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0.02f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp),
            color = color
        )
    }
}

@Composable
private fun ActivityBarChart(
    activities: List<DailyActivity>,
    goalMinutes: Int,
    primaryColor: Color,
    secondaryColor: Color
) {
    if (activities.isEmpty()) {
        Text(
            text = "ابدأ أول جلسة مراجعة أو اختبار اليوم ليظهر الرسم البياني لأدائك الأسبوعي.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val maxVal = maxOf(goalMinutes, activities.maxOfOrNull { it.minutesStudied.toInt() } ?: 10).coerceAtLeast(10)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val count = activities.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.52f).coerceAtMost(46f)
            val goalY = size.height * (1f - (goalMinutes.toFloat() / maxVal).coerceIn(0f, 1f))

            drawLine(
                color = secondaryColor.copy(alpha = 0.65f),
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 3f
            )

            activities.forEachIndexed { idx, act ->
                val ratio = (act.minutesStudied.toFloat() / maxVal).coerceIn(0.06f, 1f)
                val barH = size.height * ratio
                val left = idx * slotW + (slotW - barW) / 2f
                drawRoundRect(
                    color = if (act.minutesStudied >= goalMinutes) secondaryColor else primaryColor,
                    topLeft = Offset(left, size.height - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            activities.forEach { act ->
                Text(
                    text = act.dateKey.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    settings: com.example.data.local.UserSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onArabicUiChange: (Boolean) -> Unit,
    onGoalChange: (Int) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (settings.arabicUi) "إعدادات التطبيق والحساب" else "App & Account Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (settings.arabicUi) "مظهر التطبيق" else "Appearance",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { m ->
                    val label = when (m) {
                        ThemeMode.SYSTEM -> "تلقائي"
                        ThemeMode.LIGHT -> "نهاري"
                        ThemeMode.DARK -> "ليلي"
                    }
                    FilterChip(
                        selected = settings.themeMode == m,
                        onClick = { onThemeChange(m) },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (settings.arabicUi) "واجهة عربية (RTL)" else "Arabic RTL Interface",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = settings.arabicUi,
                    onCheckedChange = onArabicUiChange
                )
            }

            Column {
                Text(
                    text = "الهدف اليومي للدراسة: ${settings.dailyGoalMinutes} دقيقة",
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = settings.dailyGoalMinutes.toFloat(),
                    onValueChange = { onGoalChange(it.toInt()) },
                    valueRange = 10f..90f
                )
            }

            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().height(50.dp).testTag("sign_out_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (settings.arabicUi) "تسجيل الخروج" else "Sign Out")
            }
        }
    }
}
