package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class HormoneColors(
    val testosterone: Color,
    val cortisol: Color,
    val dopamine: Color,
    val serotonin: Color
)

val LocalHormoneColors = staticCompositionLocalOf {
    HormoneColors(
        testosterone = TestosteroneAmber,
        cortisol = CortisolCoral,
        dopamine = DopamineCobalt,
        serotonin = SerotoninEmerald
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = TestosteroneAmber,
    onSecondary = Color(0xFF281800),
    secondaryContainer = Color(0xFF452B00),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = DopamineCobalt,
    onTertiary = Color(0xFF001C3B),
    background = TealObsidianBg,
    onBackground = TextPrimaryDark,
    surface = TealSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = TealSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHigh = TealSurfaceElevated,
    outline = OutlineDark,
    error = CortisolCoral
)

private val LightColorScheme = lightColorScheme(
    primary = MineralPrimary,
    onPrimary = MineralOnPrimary,
    primaryContainer = MineralPrimaryContainer,
    onPrimaryContainer = MineralOnPrimaryContainer,
    secondary = TestosteroneAmberLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = DopamineCobaltLight,
    onTertiary = Color.White,
    background = MineralBg,
    onBackground = TextPrimaryLight,
    surface = MineralSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = MineralSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    error = CortisolCoralLight
)

val EndoPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EndoPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val hormoneColors = if (darkTheme) {
        HormoneColors(
            testosterone = TestosteroneAmber,
            cortisol = CortisolCoral,
            dopamine = DopamineCobalt,
            serotonin = SerotoninEmerald
        )
    } else {
        HormoneColors(
            testosterone = TestosteroneAmberLight,
            cortisol = CortisolCoralLight,
            dopamine = DopamineCobaltLight,
            serotonin = SerotoninEmeraldLight
        )
    }

    CompositionLocalProvider(LocalHormoneColors provides hormoneColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = EndoPulseTypography,
            shapes = EndoPulseShapes,
            content = content
        )
    }
}
