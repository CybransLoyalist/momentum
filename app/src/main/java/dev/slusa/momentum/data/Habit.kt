package dev.slusa.momentum.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    /** true = codziennie, false = tylko dni z [weekdaysMask]. */
    val daily: Boolean = true,

    /**
     * Maska dni tygodnia: bit 0 to poniedzialek, bit 6 to niedziela.
     * Liczba zamiast zbioru, zeby nie ciagnac konwertera kolekcji do Rooma.
     */
    val weekdaysMask: Int = 0,

    /** Tryb urlopowy. Dni w tym zakresie nie licza sie ani jako zrobione, ani jako pominiete. */
    val pausedFrom: LocalDate? = null,
    val pausedTo: LocalDate? = null,

    /** Nawyk zdjety z obiegu. Historia zostaje. */
    val archived: Boolean = false,

    val sortIndex: Int = 0,

    /** Momentum liczy sie od tego dnia - wczesniej nawyk nie istnial. */
    val startDate: LocalDate = LocalDate.now(),
) {
    fun isScheduledOn(date: LocalDate): Boolean =
        if (daily) true else weekdaysMask and (1 shl (date.dayOfWeek.value - 1)) != 0

    fun isPausedOn(date: LocalDate): Boolean {
        val from = pausedFrom ?: return false
        val to = pausedTo ?: return false
        return !date.isBefore(from) && !date.isAfter(to)
    }

    val weekdays: Set<DayOfWeek>
        get() = DayOfWeek.entries.filter { weekdaysMask and (1 shl (it.value - 1)) != 0 }.toSet()

    companion object {
        fun maskOf(days: Set<DayOfWeek>): Int =
            days.fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }
    }
}

/**
 * Jeden wpis na wykonanie nawyku w danym dniu. Trzymane bezterminowo - bez tego
 * nie ma z czego przeliczyc momentum ani narysowac siatki tygodni.
 */
@Entity(tableName = "habit_completions", primaryKeys = ["habitId", "date"])
data class HabitCompletion(
    val habitId: Long,
    val date: LocalDate,
)
