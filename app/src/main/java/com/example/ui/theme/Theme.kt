package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val LexisLightColorScheme = lightColorScheme(
    primary = SapphirePrimaryLight,
    onPrimary = OnSapphireLight,
    primaryContainer = SapphireContainerLight,
    onPrimaryContainer = OnSapphireContainerLight,
    secondary = AmberGoldSecondaryLight,
    onSecondary = OnAmberGoldLight,
    secondaryContainer = AmberGoldContainerLight,
    onSecondaryContainer = OnAmberGoldContainerLight,
    tertiary = EmeraldTertiaryLight,
    onTertiary = OnEmeraldLight,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = OnEmeraldContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val LexisDarkColorScheme = darkColorScheme(
    primary = SapphirePrimaryDark,
    onPrimary = OnSapphireDark,
    primaryContainer = SapphireContainerDark,
    onPrimaryContainer = OnSapphireContainerDark,
    secondary = AmberGoldSecondaryDark,
    onSecondary = OnAmberGoldDark,
    secondaryContainer = AmberGoldContainerDark,
    onSecondaryContainer = OnAmberGoldContainerDark,
    tertiary = EmeraldTertiaryDark,
    onTertiary = OnEmeraldDark,
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = OnEmeraldContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

val LexisShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LexisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> LexisDarkColorScheme
        else -> LexisLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LexisTypography,
        shapes = LexisShapes,
        content = content
    )
}
