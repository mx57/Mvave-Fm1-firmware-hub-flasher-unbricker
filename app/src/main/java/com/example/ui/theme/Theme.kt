package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SynthStudioColorScheme = darkColorScheme(
    primary = SynthOrange,
    onPrimary = Color(0xFF140702),
    primaryContainer = SynthOrangeContainer,
    onPrimaryContainer = OnSynthOrangeContainer,
    secondary = SynthPhosphor,
    onSecondary = Color(0xFF021A12),
    secondaryContainer = SynthPhosphorContainer,
    onSecondaryContainer = OnSynthPhosphorContainer,
    tertiary = SynthCyan,
    onTertiary = Color(0xFF041A26),
    tertiaryContainer = SynthCyanContainer,
    onTertiaryContainer = OnSynthCyanContainer,
    background = SynthChassisBg,
    onBackground = SynthTextPrimary,
    surface = SynthSurface,
    onSurface = SynthTextPrimary,
    surfaceVariant = SynthSurfaceElevated,
    onSurfaceVariant = SynthTextSecondary,
    outline = SynthSurfaceBorder,
    error = SynthCrimsonError,
    onError = Color.White,
    errorContainer = SynthCrimsonContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

val SynthShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SynthStudioColorScheme,
        typography = Typography,
        shapes = SynthShapes,
        content = content
    )
}
