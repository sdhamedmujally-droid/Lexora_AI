package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.audio.DualAccentAudioEngine
import com.example.ui.theme.AccentUkCrimson
import com.example.ui.theme.AccentUsBlue
import com.example.ui.theme.CefrA1Color
import com.example.ui.theme.CefrA2Color
import com.example.ui.theme.CefrB1Color
import com.example.ui.theme.CefrB2Color
import com.example.ui.theme.CefrC1Color

fun cefrColor(level: String): Color = when (level) {
    "A1" -> CefrA1Color
    "A2" -> CefrA2Color
    "B1" -> CefrB1Color
    "B2" -> CefrB2Color
    "C1" -> CefrC1Color
    else -> CefrA1Color
}

@Composable
fun LtrBlock(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        content()
    }
}

@Composable
fun CefrLevelBadge(
    level: String,
    modifier: Modifier = Modifier
) {
    val color = cefrColor(level)
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = level,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun DualAccentButtons(
    textToSpeak: String,
    audioEngine: DualAccentAudioEngine,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tagPrefix: String = "pronounce"
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { audioEngine.speakUs(textToSpeak) },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .height(if (compact) 40.dp else 44.dp)
                .testTag("${tagPrefix}_us_button"),
            contentPadding = PaddingValues(horizontal = if (compact) 10.dp else 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AccentUsBlue.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AccentUsBlue.copy(alpha = 0.08f),
                contentColor = AccentUsBlue
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "نطق أمريكي",
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (compact) "US" else "أمريكي US",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }

        OutlinedButton(
            onClick = { audioEngine.speakUk(textToSpeak) },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .height(if (compact) 40.dp else 44.dp)
                .testTag("${tagPrefix}_uk_button"),
            contentPadding = PaddingValues(horizontal = if (compact) 10.dp else 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, AccentUkCrimson.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = AccentUkCrimson.copy(alpha = 0.08f),
                contentColor = AccentUkCrimson
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "نطق بريطاني",
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (compact) "UK" else "بريطاني UK",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LexisHeroBanner(
    title: String,
    subtitle: String,
    badge: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
            MaterialTheme.colorScheme.tertiary
        )
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(gradient)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!badge.isNullOrBlank()) {
                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.bodyMedium
            )
            content()
        }
    }
}

@Composable
fun StatMetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = tint.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(18.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = "خطأ",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
