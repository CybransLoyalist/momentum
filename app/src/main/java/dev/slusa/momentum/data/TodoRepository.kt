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
        forToday: Boolean,
        today: LocalDate = LocalDate.now(),
    ): Long {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return -1
        return dao.insert(
            Todo(
                title = trimmed,
                bucket = bucket,
                plannedDate = if (forToday) today else null,
                firstTodayDate = if (forToday) today else null,
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
                todo.copy(plannedDate = null, firstTodayDate = null, fromSchedule = false)
            } else {
                todo.copy(plannedDate = today, firstTodayDate = today, fromSchedule = false)
            }
        )
    }

    suspend fun setDone(id: Long, done: Boolean, now: Instant = Instant.now()) {
        val todo = dao.byId(id) ?: return
        dao.update(todo.copy(completedAt = if (done) now else null))
    }

    suspend fun delete(todo: Todo) = dao.delete(todo)

    /** Sprzatanie odhaczonych starszych niz doba. Wolane przy starcie aplikacji. */
    suspend fun purgeOldCompleted(now: Instant = Instant.now()): Int =
        dao.purgeCompletedBefore(now.minus(UNDO_WINDOW))

    companion object {
        val UNDO_WINDOW: Duration = Duration.ofHours(24)
    }
}
