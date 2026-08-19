package dev.slusa.momentum.data

import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Cala logika przenoszenia rzeczy miedzy stanami siedzi tutaj, zeby UI zajmowal
 * sie tylko rysowaniem. Regulami rzadzi docs/spec.html.
 */
class TodoRepository(private val dao: TodoDao) {

    fun open(bucket: Bucket): Flow<List<Todo>> = dao.observeOpen(bucket)

    fun completedLastDay(bucket: Bucket, now: Instant = Instant.now()): Flow<List<Todo>> =
        dao.observeCompletedSince(bucket, now.minus(UNDO_WINDOW))

    suspend fun add(
        title: String,
        bucket: Bucket = Bucket.GLOWNE,
        plannedDate: LocalDate? = null,
        fromSchedule: Boolean = false,
    ): Long {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return -1
        return dao.insert(
            Todo(
                title = trimmed,
                bucket = bucket,
                plannedDate = plannedDate,
                fromSchedule = fromSchedule,
                sortIndex = dao.nextTopSortIndex(bucket),
            )
        )
    }

    /**
     * Przelacza "na dzisiaj". Zdjecie i ponowne zaznaczenie kasuje wiek - to
     * swiadoma furtka, zeby dalo sie zaczac z czystym kontem.
     */
    suspend fun toggleToday(id: Long, today: LocalDate = LocalDate.now()) {
        val todo = dao.byId(id) ?: return
        val onTodayList = todo.plannedDate != null && !todo.plannedDate.isAfter(today)
        dao.update(
            if (onTodayList) {
                todo.copy(plannedDate = null, fromSchedule = false)
            } else {
                todo.copy(plannedDate = today, fromSchedule = false)
            }
        )
    }

    /**
     * Ustawia termin. Data w przyszlosci znika z listy glownej i wraca sama tego dnia;
     * [fromSchedule] rozdziela potem sekcje "z terminem" od recznie oznaczonych.
     */
    suspend fun schedule(id: Long, date: LocalDate) {
        val todo = dao.byId(id) ?: return
        dao.update(todo.copy(plannedDate = date, fromSchedule = true, bucket = Bucket.GLOWNE))
    }

    /**
     * Przenosi miedzy listami. Wyjscie z listy glownej gubi termin - rzecz odlozona
     * "na kiedys" z terminem na wczoraj bylaby sprzecznoscia sama w sobie.
     */
    suspend fun moveTo(id: Long, bucket: Bucket) {
        val todo = dao.byId(id) ?: return
        dao.update(
            if (bucket == Bucket.GLOWNE) {
                todo.copy(bucket = bucket)
            } else {
                todo.copy(bucket = bucket, plannedDate = null, fromSchedule = false)
            }
        )
    }

    suspend fun setDone(id: Long, done: Boolean, now: Instant = Instant.now()) {
        val todo = dao.byId(id) ?: return
        dao.update(todo.copy(completedAt = if (done) now else null))
    }

    suspend fun delete(id: Long) {
        val todo = dao.byId(id) ?: return
        dao.delete(todo)
    }

    suspend fun clearCompleted(bucket: Bucket) = dao.clearCompleted(bucket)

    /** Sprzatanie odhaczonych starszych niz doba. Wolane przy starcie aplikacji. */
    suspend fun purgeOldCompleted(now: Instant = Instant.now()): Int =
        dao.purgeCompletedBefore(now.minus(UNDO_WINDOW))

    companion object {
        val UNDO_WINDOW: Duration = Duration.ofHours(24)
    }
}
