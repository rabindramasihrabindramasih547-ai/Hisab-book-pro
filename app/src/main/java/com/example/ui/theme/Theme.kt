package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class HisabExtraColors(
    val navyHeader: Color,
    val navyHeaderAlt: Color,
    val emerald: Color,
    val emeraldContainer: Color,
    val gold: Color,
    val goldContainer: Color,
    val crimson: Color,
    val crimsonContainer: Color,
    val cardBorder: Color,
    val textMuted: Color
)

private val LightExtraColors = HisabExtraColors(
    navyHeader = NavyDeep,
    navyHeaderAlt = NavyPrimary,
    emerald = EmeraldPrimary,
    emeraldContainer = EmeraldContainerLight,
    gold = GoldPrimary,
    goldContainer = GoldContainerLight,
    crimson = CrimsonAlert,
    crimsonContainer = CrimsonContainerLight,
    cardBorder = LightOutline,
    textMuted = LightTextSecondary
)

private val DarkExtraColors = HisabExtraColors(
    navyHeader = NavyDeep,
    navyHeaderAlt = NavyPrimary,
    emerald = EmeraldBright,
    emeraldContainer = EmeraldContainerDark,
    gold = GoldBright,
    goldContainer = GoldContainerDark,
    crimson = CrimsonBright,
    crimsonContainer = CrimsonContainerDark,
    cardBorder = DarkOutline,
    textMuted = DarkTextSecondary
)

val LocalHisabColors = staticCompositionLocalOf { LightExtraColors }

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyContainer,
    onPrimaryContainer = Color.White,
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = EmeraldContainerDark,
    tertiary = GoldPrimary,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainerLight,
    onTertiaryContainer = GoldContainerDark,
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = CrimsonContainerDark,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = GoldBright,
    onPrimary = NavyDeep,
    primaryContainer = NavyContainer,
    onPrimaryContainer = Color.White,
    secondary = EmeraldBright,
    onSecondary = NavyDeep,
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = EmeraldContainerLight,
    tertiary = GoldBright,
    onTertiary = NavyDeep,
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = GoldContainerLight,
    error = CrimsonBright,
    onError = NavyDeep,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = CrimsonContainerLight,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

val HisabShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun HisabBookTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalHisabColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HisabTypography,
            shapes = HisabShapes,
            content = content
        )
    }
}
