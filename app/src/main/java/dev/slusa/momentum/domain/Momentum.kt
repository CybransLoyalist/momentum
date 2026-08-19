package dev.slusa.momentum.domain

import dev.slusa.momentum.data.Habit
import java.time.LocalDate

/** Stan pojedynczego dnia w siatce tygodni. */
enum class DayState { ZROBIONE, POMINIETE, WOLNE, DZISIAJ, PAUZA, PRZYSZLOSC, PRZED_STARTEM }

/**
 * Nagroda za konsekwencje, ktora nie kasuje sie do zera po jednym potknieciu.
 *
 * Momentum liczymy z historii przy kazdym odczycie zamiast trzymac je w bazie.
 * Przechowywana liczba i historia moglyby sie rozjechac - przy odhaczaniu wstecz,
 * przy zmianie harmonogramu, przy pauzie - a wtedy nie wiadomo, ktora ma racje.
 * Kilkaset iteracji po dniach to koszt nie do zauwazenia.
 */
object Momentum {

    const val MAX = 10

    /** Dalej wstecz momentum i tak jest wysycone, wiec nie ma po co liczyc. */
    private const val HORIZON_DAYS = 400L

    fun compute(habit: Habit, completions: Set<LocalDate>, today: LocalDate): Int {
        var value = 0
        var day = maxOf(habit.startDate, today.minusDays(HORIZON_DAYS))

        while (!day.isAfter(today)) {
            value = step(habit, completions, value, day, today)
            day = day.plusDays(1)
        }
        return value
    }

    private fun step(
        habit: Habit,
        completions: Set<LocalDate>,
        current: Int,
        day: LocalDate,
        today: LocalDate,
    ): Int {
        if (habit.isPausedOn(day)) return current

        val done = day in completions
        val scheduled = habit.isScheduledOn(day)

        return when {
            // Zrobione liczy sie zawsze - takze w dzien, w ktory nawyk nie wypada.
            // Nadprogramowa robota ma sie oplacac.
            done -> (current + 1).coerceAtMost(MAX)

            // Dzisiejszy dzien jeszcze trwa, wiec brak odhaczenia nie jest jeszcze
            // pominieciem. Inaczej kazdy poranek zaczynalby sie od kary.
            scheduled && day.isBefore(today) -> (current - 1).coerceAtLeast(0)

            else -> current
        }
    }

    /** Siatka ostatnich [weeks] tygodni, od poniedzialku najstarszego tygodnia. */
    fun grid(
        habit: Habit,
        completions: Set<LocalDate>,
        today: LocalDate,
        weeks: Int = 8,
    ): List<Pair<LocalDate, DayState>> {
        val start = today
            .minusWeeks((weeks - 1).toLong())
            .with(java.time.DayOfWeek.MONDAY)

        return (0 until weeks * 7).map { offset ->
            val day = start.plusDays(offset.toLong())
            day to stateOf(habit, completions, day, today)
        }
    }

    private fun stateOf(
        habit: Habit,
        completions: Set<LocalDate>,
        day: LocalDate,
        today: LocalDate,
    ): DayState = when {
        day.isAfter(today) -> DayState.PRZYSZLOSC
        day.isBefore(habit.startDate) -> DayState.PRZED_STARTEM
        // Pauza przed wykonaniem, tak samo jak w liczeniu momentum - dzien urlopowy
        // nie liczy sie w zadna strone, nawet jesli cos w nim odhaczylas.
        habit.isPausedOn(day) -> DayState.PAUZA
        day in completions -> DayState.ZROBIONE
        !habit.isScheduledOn(day) -> DayState.WOLNE
        day == today -> DayState.DZISIAJ
        else -> DayState.POMINIETE
    }
}
