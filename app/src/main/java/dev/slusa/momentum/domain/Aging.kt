package dev.slusa.momentum.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor

/**
 * Im dluzej rzecz wisi na liscie na dzisiaj, tym bardziej gnije wizualnie.
 * Kolor niesie waski pasek przy lewej krawedzi kafelka - celowo nie tlo calego
 * kafelka, zeby lista po tygodniu marnego humoru nie byla sciana czerni.
 */
object Aging {

    const val DEFAULT_THRESHOLD_DAYS = 7

    /** Wiek liczony od pierwszego dnia na liscie, nie od utworzenia. */
    fun ageDays(firstTodayDate: LocalDate?, today: LocalDate): Int {
        if (firstTodayDate == null) return 0
        return ChronoUnit.DAYS.between(firstTodayDate, today).toInt().coerceAtLeast(0)
    }

    /**
     * Mapuje wiek na rampe kolorow z motywu. Pomiedzy przystankami interpolujemy,
     * zeby przejscie bylo plynne, a nie skokowe co dzien.
     */
    fun color(ageDays: Int, ramp: List<Color>, thresholdDays: Int = DEFAULT_THRESHOLD_DAYS): Color {
        if (ramp.isEmpty()) return Color.Transparent
        if (ramp.size == 1) return ramp.first()

        val progress = (ageDays.toFloat() / thresholdDays.coerceAtLeast(1)).coerceIn(0f, 1f)
        val position = progress * (ramp.size - 1)
        val lower = floor(position).toInt().coerceIn(0, ramp.size - 2)
        return lerp(ramp[lower], ramp[lower + 1], position - lower)
    }
}

/**
 * Czarny albo bialy tekst na podanym tle, zaleznie od tego, ktory bedzie czytelny.
 *
 * Potrzebne wszedzie tam, gdzie tlo jedzie po rampie kolorow: to samo pole bywa
 * jasnozielone i prawie czarne, wiec kolor tekstu wpisany na sztywno musialby przegrac
 * na jednym koncu skali.
 */
fun contrastOn(background: Color): Color =
    if (background.luminance() > 0.45f) Color(0xFF141920) else Color.White
