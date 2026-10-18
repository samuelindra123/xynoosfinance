package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.domain.AppThemeMode

private val XynoosDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmeraldDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldPrimary,
    secondary = SapphireSecondary,
    onSecondary = OnSapphireDark,
    secondaryContainer = SapphireContainerDark,
    onSecondaryContainer = SapphireSecondary,
    tertiary = WarmAmber,
    onTertiary = ObsidianBackground,
    tertiaryContainer = WarmAmberContainerDark,
    onTertiaryContainer = WarmAmber,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSubtleDark,
    outlineVariant = ObsidianSurfaceHighlight,
    error = RefinedRed,
    onError = TextPrimaryDark,
    errorContainer = RefinedRedContainerDark,
    onErrorContainer = RefinedRed
)

private val XynoosLightColorScheme = lightColorScheme(
    primary = DaylightPrimary,
    onPrimary = OnDaylightPrimary,
    primaryContainer = DaylightPrimaryContainer,
    onPrimaryContainer = DaylightPrimary,
    secondary = DaylightSecondary,
    onSecondary = OnDaylightPrimary,
    secondaryContainer = DaylightSecondaryContainer,
    onSecondaryContainer = DaylightSecondary,
    tertiary = WarmAmberLight,
    onTertiary = DaylightSurface,
    background = DaylightBackground,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderSubtleLight,
    outlineVariant = BorderSubtleLight,
    error = RefinedRedLight,
    onError = DaylightSurface
)

val XynoosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun XynoosFinanceTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) XynoosDarkColorScheme else XynoosLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = XynoosShapes,
        content = content
    )
}
