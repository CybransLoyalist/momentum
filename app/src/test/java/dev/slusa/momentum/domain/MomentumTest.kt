package dev.slusa.momentum.domain

import dev.slusa.momentum.data.Habit
import dev.slusa.momentum.data.Vacation
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Regul momentum nie da sie sprawdzic klikaniem - efekty widac po tygodniach.
 * Kazdy test odpowiada jednej decyzji z docs/spec.html.
 */
class MomentumTest {

    private val today = LocalDate.of(2026, 8, 19) // sroda

    private fun daily(start: LocalDate = today.minusDays(30)) =
        Habit(id = 1, name = "Duolingo", daily = true, startDate = start)

    private fun onDays(vararg days: DayOfWeek, start: LocalDate = today.minusDays(60)) =
        Habit(
            id = 2,
            name = "Silownia",
            daily = false,
            weekdaysMask = Habit.maskOf(days.toSet()),
            startDate = start,
        )

    @Test
    fun `dziesiec dni z rzedu daje maksa`() {
        val habit = daily(start = today.minusDays(9))
        val done = (0..9).map { today.minusDays(it.toLong()) }.toSet()

        assertEquals(Momentum.MAX, Momentum.compute(habit, done, today))
    }

    @Test
    fun `momentum nie przekracza dziesiatki`() {
        val habit = daily(start = today.minusDays(40))
        val done = (0..40).map { today.minusDays(it.toLong()) }.toSet()

        assertEquals(Momentum.MAX, Momentum.compute(habit, done, today))
    }

    @Test
    fun `jeden opuszczony dzien kosztuje jeden punkt, nie wszystko`() {
        val habit = daily(start = today.minusDays(9))
        // Wszystko oprocz przedwczoraj.
        val done = (0..9).map { today.minusDays(it.toLong()) }.toSet() - today.minusDays(2)

        // Siedem trafien przed przerwa, kara schodzi z 7 na 6, potem dwa trafienia.
        // Bez potkniecia byloby 10 - jeden opuszczony dzien kosztuje wiec dwa punkty
        // wzgledem idealu, a nie caly dorobek.
        assertEquals(8, Momentum.compute(habit, done, today))
    }

    @Test
    fun `dzisiejszy brak odhaczenia nie jest jeszcze kara`() {
        val habit = daily(start = today.minusDays(4))
        val done = (1..4).map { today.minusDays(it.toLong()) }.toSet()

        // Cztery dni zrobione, dzisiaj jeszcze trwa - ma byc 4, nie 3.
        assertEquals(4, Momentum.compute(habit, done, today))
    }

    @Test
    fun `nawyk dwa razy w tygodniu nie traci punktow w wolne dni`() {
        val habit = onDays(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, start = today.minusWeeks(3))
        val done = generateSequence(today.minusWeeks(3)) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .filter { it.dayOfWeek == DayOfWeek.TUESDAY || it.dayOfWeek == DayOfWeek.THURSDAY }
            .toSet()

        // Same trafienia, zadnych kar - momentum rosnie o liczbe wystapien.
        assertEquals(done.size.coerceAtMost(Momentum.MAX), Momentum.compute(habit, done, today))
    }

    @Test
    fun `robota w dzien wolny daje bonus`() {
        val habit = onDays(DayOfWeek.TUESDAY, start = today.minusDays(2))
        // Wtorek byl wczoraj i zrobiony, dzisiaj sroda - te; zrobiona mimo ze nie wypada.
        val done = setOf(today.minusDays(1), today)

        assertEquals(2, Momentum.compute(habit, done, today))
    }

    @Test
    fun `urlop nie nalicza ani nagrody ani kary`() {
        val habit = daily(start = today.minusDays(5))
        val urlop = Vacation(from = today.minusDays(4), until = today.minusDays(1))
        val done = setOf(today.minusDays(5))

        // Jedno trafienie przed urlopem, potem cztery dni zamrozone.
        assertEquals(1, Momentum.compute(habit, done, today, urlop))
    }

    @Test
    fun `urlop bez daty konca trwa az do odwolania`() {
        val habit = daily(start = today.minusDays(10))
        val urlop = Vacation(from = today.minusDays(6), until = null)
        val done = (7..10).map { today.minusDays(it.toLong()) }.toSet()

        // Cztery trafienia przed urlopem, potem nic sie nie zmienia mimo braku odhaczen.
        assertEquals(4, Momentum.compute(habit, done, today, urlop))
    }

    @Test
    fun `urlop bije odhaczenie - dzien urlopowy nie liczy sie w zadna strone`() {
        val habit = daily(start = today.minusDays(3))
        val urlop = Vacation(from = today.minusDays(3), until = today)
        val done = (0..3).map { today.minusDays(it.toLong()) }.toSet()

        assertEquals(0, Momentum.compute(habit, done, today, urlop))
    }

    @Test
    fun `momentum nie schodzi ponizej zera`() {
        val habit = daily(start = today.minusDays(20))

        assertEquals(0, Momentum.compute(habit, emptySet(), today))
    }

    @Test
    fun `siatka pokazuje osiem tygodni`() {
        val habit = daily()
        val grid = Momentum.grid(habit, emptySet(), today)

        assertEquals(56, grid.size)
        assertEquals(DayOfWeek.MONDAY, grid.first().first.dayOfWeek)
    }

    @Test
    fun `dni po dzisiaj sa oznaczone jako przyszlosc`() {
        val habit = daily()
        val grid = Momentum.grid(habit, emptySet(), today)

        val future = grid.filter { it.first.isAfter(today) }
        assertEquals(true, future.all { it.second == DayState.PRZYSZLOSC })
    }
}
