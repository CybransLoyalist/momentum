package dev.slusa.momentum.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta z specyfikacji. Akcent to gleboka zielen, neutralne maja lekki zielony bias,
// zeby szarosci nie wygladaly na przypadkowe.

val GreenDeep = Color(0xFF1D5C45)
val GreenBright = Color(0xFF6FCFA0)
val GreenSoftLight = Color(0xFFE2EFE8)
val GreenSoftDark = Color(0xFF172A21)

val InkLight = Color(0xFF141B17)
val InkDark = Color(0xFFE6EDE8)

val SurfaceLight = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF5F8F6)
val SurfaceVariantLight = Color(0xFFE9EFEB)
val OutlineLight = Color(0xFFD5DED8)

val SurfaceDark = Color(0xFF161D19)
val BackgroundDark = Color(0xFF0F1412)
val SurfaceVariantDark = Color(0xFF1D2620)
val OutlineDark = Color(0xFF28322C)

/**
 * Rampa starzenia sie zadan: swieze -> zgnile. Uzywana jako pasek przy lewej
 * krawedzi kafelka, nigdy jako tlo calego kafelka.
 */
object AgeRamp {
    val light = listOf(
        Color(0xFF3E9B72),
        Color(0xFF7FA23F),
        Color(0xFFC9A227),
        Color(0xFFCE6B3A),
        Color(0xFF9E3030),
        Color(0xFF191517),
    )

    val dark = listOf(
        Color(0xFF4FB585),
        Color(0xFF93B84C),
        Color(0xFFD6B23A),
        Color(0xFFDC7A48),
        Color(0xFFB94141),
        Color(0xFF0B0A0B),
    )
}
