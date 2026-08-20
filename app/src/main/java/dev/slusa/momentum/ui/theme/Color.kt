package dev.slusa.momentum.ui.theme

import androidx.compose.ui.graphics.Color

// Akcent to gleboki blekit, neutralne maja lekki niebieski bias, zeby szarosci nie
// wygladaly na przypadkowe. Wczesniej cala paleta byla zielona, ale ten sam zielony
// niosl dwie rzeczy naraz - marke i "swieze" z rampy starzenia - i przez to pasek
// wieku gubil sie na tle wlasnego akcentu.

val BlueDeep = Color(0xFF1D4E77)
val BlueBright = Color(0xFF6FB8E8)
val BlueSoftLight = Color(0xFFE2EAF3)
val BlueSoftDark = Color(0xFF16232E)

val InkLight = Color(0xFF141920)
val InkDark = Color(0xFFE6EBF0)

val SurfaceLight = Color(0xFFFFFFFF)
val BackgroundLight = Color(0xFFF4F7FB)
val SurfaceVariantLight = Color(0xFFE8EDF4)
val OutlineLight = Color(0xFFD4DCE6)

val SurfaceDark = Color(0xFF151A21)
val BackgroundDark = Color(0xFF0E1218)
val SurfaceVariantDark = Color(0xFF1B222B)
val OutlineDark = Color(0xFF26303B)

/**
 * Rampa starzenia sie zadan: swieze -> zgnile. Uzywana jako pasek przy lewej
 * krawedzi kafelka, nigdy jako tlo calego kafelka.
 *
 * Rampa zostaje ciepla mimo niebieskiego akcentu, bo niesie znaczenie, a nie marke:
 * zielone-swieze i czerwone-spoznione czyta kazdy bez tlumaczenia, a przelozenie tego
 * na odcienie niebieskiego zamienilo by sygnal w dekoracje. Pierwszy przystanek zszedl
 * tylko w strone morskiej zieleni, zeby swiezy pasek nie klocil sie z reszta ekranu.
 */
object AgeRamp {
    val light = listOf(
        Color(0xFF2E9B86),
        Color(0xFF7FA23F),
        Color(0xFFC9A227),
        Color(0xFFCE6B3A),
        Color(0xFF9E3030),
        Color(0xFF191517),
    )

    val dark = listOf(
        Color(0xFF45B8A0),
        Color(0xFF93B84C),
        Color(0xFFD6B23A),
        Color(0xFFDC7A48),
        Color(0xFFB94141),
        Color(0xFF0B0A0B),
    )
}
