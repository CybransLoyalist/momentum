package dev.slusa.momentum.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = GreenDeep,
    onPrimary = Color.White,
    primaryContainer = GreenSoftLight,
    onPrimaryContainer = GreenDeep,
    background = BackgroundLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3F4C45),
    outline = OutlineLight,
    outlineVariant = OutlineLight,
)

private val DarkScheme = darkColorScheme(
    primary = GreenBright,
    onPrimary = Color(0xFF06150F),
    primaryContainer = GreenSoftDark,
    onPrimaryContainer = GreenBright,
    background = BackgroundDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFAEBCB4),
    outline = OutlineDark,
    outlineVariant = OutlineDark,
)

/** Rampa starzenia nie miesci sie w schemacie Material3, wiec jedzie osobnym kanalem. */
val LocalAgeRamp: ProvidableCompositionLocal<List<Color>> =
    staticCompositionLocalOf { AgeRamp.light }

@Composable
fun MomentumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val ramp = if (darkTheme) AgeRamp.dark else AgeRamp.light

    CompositionLocalProvider(LocalAgeRamp provides ramp) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MomentumTypography,
            content = content,
        )
    }
}
