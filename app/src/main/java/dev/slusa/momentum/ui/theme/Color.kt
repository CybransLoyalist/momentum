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
 * Kolor nagrody: cieple limonkowe zielone, wspolne dla obu ramp.
 *
 * Zimna morska zielen wygladala jak status systemu, a nie jak dobra wiadomosc. Ta sama
 * wartosc konczy rampe dorobku i zaczyna rampe starzenia, bo "zrobione" i "swieze" to
 * ta sama informacja - i jedyne miejsce, w ktorym obie skale trzeba trzymac w zgodzie.
 */
val RewardGreen = Color(0xFF93B84C)

/**
 * Rampa starzenia sie zadan: swieze -> zgnile. Uzywana jako plama przy lewej krawedzi
 * kafelka, nigdy jako tlo calego kafelka.
 *
 * Rampa jest ciepla mimo niebieskiego akcentu, bo niesie znaczenie, a nie marke:
 * zielone-swieze i czerwone-spoznione czyta kazdy bez tlumaczenia, a przelozenie tego
 * na odcienie niebieskiego zamienilo by sygnal w dekoracje. Przystanki ida po odcieniu -
 * limonka, zloto, bursztyn, czerwien, ciemna czerwien, czern - zeby kazdy krok byl
 * widoczny jako krok, a nie jako przyciemnienie poprzedniego.
 */
object AgeRamp {
    val light = listOf(
        RewardGreen,
        Color(0xFFC2B23C),
        Color(0xFFD08A2E),
        Color(0xFFC4562F),
        Color(0xFF8F2A2A),
        Color(0xFF191517),
    )

    val dark = listOf(
        RewardGreen,
        Color(0xFFD6C64A),
        Color(0xFFE09B3C),
        Color(0xFFD96A3E),
        Color(0xFFA83535),
        Color(0xFF0B0A0B),
    )
}

/**
 * Rampa dziennego dorobku: nic zrobione -> dzien udany. Odwrotnie niz [AgeRamp], bo
 * mierzy przyrost, a nie gnicie.
 *
 * Dolny koniec jest ciemnoszary, a nie czarny. Czern zarezerwowana jest dla zaleglosci
 * i banner witajacy czernia o siodmej rano bylby oskarzeniem, zanim dzien sie zaczal -
 * czyli dokladnie ta pulapka, w ktora wpadl pasek starzenia. Droga wiedzie od zimnej
 * szarosci ku cieplej limonce, wiec sam odcien ociepla sie razem z dorobkiem.
 */
object DoneRamp {
    /** Ile rzeczy dziennie daje pelna zielen. */
    const val TARGET = 5

    val light = listOf(
        Color(0xFF4A5560),
        Color(0xFF5E7454),
        Color(0xFF779551),
        RewardGreen,
    )

    val dark = listOf(
        Color(0xFF5A6672),
        Color(0xFF6E8560),
        Color(0xFF85A557),
        RewardGreen,
    )
}
