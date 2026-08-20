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

private val INK = Color(0xFF141920)

/**
 * Ciemny albo bialy tekst na podanym tle - ten z dwoch, ktory ma wyzszy kontrast.
 *
 * Prog jasnosci wpisany z reki byl blizej, niz wygladalo: ciepla limonka ma jasnosc
 * ponizej polowy, a mimo to czyta sie na niej duzo lepiej ciemny napis niz bialy.
 * Liczenie wspolczynnika kontrastu z definicji jest krotsze niz zgadywanie progu
 * i nie trzeba go poprawiac przy kazdej zmianie palety.
 */
fun contrastOn(background: Color): Color {
    val lum = background.luminance()
    return if (ratio(lum, INK.luminance()) >= ratio(lum, WHITE_LUMINANCE)) INK else Color.White
}

private const val WHITE_LUMINANCE = 1f

private fun ratio(a: Float, b: Float): Float =
    (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
