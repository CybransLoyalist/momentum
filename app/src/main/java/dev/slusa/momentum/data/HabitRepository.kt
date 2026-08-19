package dev.slusa.momentum.data

import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

class HabitRepository(private val dao: HabitDao) {

    fun active(): Flow<List<Habit>> = dao.observeActive()

    fun all(): Flow<List<Habit>> = dao.observeAll()

    fun completionsSince(since: LocalDate): Flow<List<HabitCompletion>> =
        dao.observeCompletionsSince(since)

    suspend fun add(name: String, daily: Boolean = true, days: Set<DayOfWeek> = emptySet()): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1
        return dao.insert(
            Habit(
                name = trimmed,
                daily = daily,
                weekdaysMask = Habit.maskOf(days),
                sortIndex = dao.nextSortIndex(),
            )
        )
    }

    suspend fun save(habit: Habit) = dao.update(habit)

    suspend fun setDone(habitId: Long, date: LocalDate, done: Boolean) {
        if (done) dao.markDone(HabitCompletion(habitId, date)) else dao.unmarkDone(habitId, date)
    }

    suspend fun setArchived(habitId: Long, archived: Boolean) {
        val habit = dao.byId(habitId) ?: return
        dao.update(habit.copy(archived = archived))
    }

    /** Usuwa nawyk razem z historia - archiwizacja jest dla tych, ktore maja zostac. */
    suspend fun delete(habitId: Long) {
        val habit = dao.byId(habitId) ?: return
        dao.deleteHistory(habitId)
        dao.delete(habit)
    }
}
