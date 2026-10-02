package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LightColorScheme = lightColorScheme(
    primary = OmaniOasisGreen,
    onPrimary = Color.White,
    primaryContainer = OmaniOasisGreenContainer,
    onPrimaryContainer = OnOmaniOasisGreenContainer,
    secondary = OmaniTerracotta,
    onSecondary = Color.White,
    secondaryContainer = OmaniTerracottaContainer,
    onSecondaryContainer = OnOmaniTerracottaContainer,
    tertiary = OmaniGold,
    onTertiary = Color.White,
    tertiaryContainer = OmaniGoldContainer,
    onTertiaryContainer = OnOmaniGoldContainer,
    background = OmaniSandBackground,
    onBackground = OmaniInkDark,
    surface = OmaniSandSurface,
    onSurface = OmaniInkDark,
    surfaceVariant = OmaniSandSurfaceVariant,
    onSurfaceVariant = OmaniInkMuted
)

private val DarkColorScheme = darkColorScheme(
    primary = OmaniNightGreen,
    onPrimary = Color(0xFF003826),
    primaryContainer = OmaniNightGreenContainer,
    onPrimaryContainer = OmaniOasisGreenContainer,
    secondary = OmaniNightTerracotta,
    onSecondary = Color(0xFF561F0E),
    secondaryContainer = Color(0xFF723522),
    onSecondaryContainer = OmaniTerracottaContainer,
    tertiary = OmaniNightGold,
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = OmaniGoldContainer,
    background = OmaniNightBackground,
    onBackground = Color(0xFFECE0D4),
    surface = OmaniNightSurface,
    onSurface = Color(0xFFECE0D4),
    surfaceVariant = OmaniNightSurfaceVariant,
    onSurfaceVariant = Color(0xFFD0C4B4)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
