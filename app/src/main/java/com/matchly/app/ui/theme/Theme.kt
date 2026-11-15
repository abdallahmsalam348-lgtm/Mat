package com.matchly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Extra brand colours used by the glass components (Material's scheme cannot express them). */
@Immutable
data class MatchlyColors(
    val bgTop: Color,
    val bgBottom: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val borderHi: Color,
    val borderLo: Color,
    val barBackground: Color,
    val emerald: Color,
    val emeraldDeep: Color,
    val gold: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val live: Color,
    val danger: Color,
    val isDark: Boolean,
)

val ForestPalette = MatchlyColors(
    bgTop = Color(0xFF0A2A1E),
    bgBottom = Color(0xFF030D09),
    glassTop = Color(0x2EFFFFFF),
    glassBottom = Color(0x0FFFFFFF),
    borderHi = Color(0x66B8FFE0),
    borderLo = Color(0x1AFFFFFF),
    barBackground = Color(0xE6071A12),
    emerald = Color(0xFF2ECC8F),
    emeraldDeep = Color(0xFF0E8F5F),
    gold = Color(0xFFD9B445),
    textPrimary = Color(0xFFF2F7F4),
    textSecondary = Color(0xFFA3BBAF),
    live = Color(0xFFFF5468),
    danger = Color(0xFFFF6B6B),
    isDark = true,
)

val BlackPalette = ForestPalette.copy(
    bgTop = Color(0xFF040A07),
    bgBottom = Color(0xFF000000),
    barBackground = Color(0xF2000000),
)

val LightPalette = MatchlyColors(
    bgTop = Color(0xFFEAF6EF),
    bgBottom = Color(0xFFD9ECE1),
    glassTop = Color(0xB3FFFFFF),
    glassBottom = Color(0x66FFFFFF),
    borderHi = Color(0xCCFFFFFF),
    borderLo = Color(0x3312805A),
    barBackground = Color(0xF2F4FBF7),
    emerald = Color(0xFF0B8A5C),
    emeraldDeep = Color(0xFF07623F),
    gold = Color(0xFF9A7A12),
    textPrimary = Color(0xFF0B1F17),
    textSecondary = Color(0xFF45594F),
    live = Color(0xFFD42A40),
    danger = Color(0xFFB3261E),
    isDark = false,
)

val LocalMatchlyColors = staticCompositionLocalOf { ForestPalette }

object Matchly {
    val colors: MatchlyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMatchlyColors.current
}

@Composable
fun MatchlyTheme(themeName: String, content: @Composable () -> Unit) {
    val palette = when (themeName) {
        "black" -> BlackPalette
        "light" -> LightPalette
        else -> ForestPalette
    }
    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.emerald,
            onPrimary = Color(0xFF00281B),
            secondary = palette.gold,
            onSecondary = Color(0xFF241B00),
            background = palette.bgBottom,
            onBackground = palette.textPrimary,
            surface = palette.bgTop,
            onSurface = palette.textPrimary,
            surfaceVariant = Color(0xFF123428),
            onSurfaceVariant = palette.textSecondary,
            outline = palette.borderLo,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.emerald,
            onPrimary = Color.White,
            secondary = palette.gold,
            background = palette.bgBottom,
            onBackground = palette.textPrimary,
            surface = palette.bgTop,
            onSurface = palette.textPrimary,
            surfaceVariant = Color(0xFFCFE6DA),
            onSurfaceVariant = palette.textSecondary,
            outline = palette.borderLo,
            error = palette.danger,
        )
    }
    CompositionLocalProvider(LocalMatchlyColors provides palette) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}

@Composable
fun isDarkTheme(themeName: String): Boolean = when (themeName) {
    "light" -> false
    "black", "dark" -> true
    else -> isSystemInDarkTheme()
}
