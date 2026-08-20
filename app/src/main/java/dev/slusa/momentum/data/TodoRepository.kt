package dev.slusa.momentum.data

import dev.slusa.momentum.domain.Recurring
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Cala logika przenoszenia rzeczy miedzy stanami siedzi tutaj, zeby UI zajmowal
 * sie tylko rysowaniem. Regulami rzadzi docs/spec.html.
 */
class TodoRepository(
    private val dao: TodoDao,
    private val recurrences: RecurrenceDao,
) {

    fun open(bucket: Bucket): Flow<List<Todo>> = dao.observeOpen(bucket)

    fun completedLastDay(bucket: Bucket, now: Instant = Instant.now()): Flow<List<Todo>> =
        dao.observeCompletedSince(bucket, now.minus(UNDO_WINDOW))

    /** Reguly powtarzania, do etykiet na kafelkach. Jest ich garstka, wiec ida hurtem. */
    fun rules(): Flow<List<Recurrence>> = recurrences.observeAll()

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
     * Ustawia termin i powtarzanie za jednym razem. Data w przyszlosci znika z listy
     * glownej i wraca sama tego dnia; [Todo.fromSchedule] rozdziela potem sekcje
     * "z terminem" od recznie oznaczonych.
     *
     * Jedna metoda, a nie dwie skladane po stronie UI: termin i regula siedza w tym
     * samym rekordzie, wiec dwa osobne odczyty i zapisy moglyby sie wzajemnie
     * nadpisac. [rule] rowne null oznacza "to sie nie powtarza".
     */
    suspend fun plan(id: Long, date: LocalDate, rule: Recurrence?) {
        val todo = dao.byId(id) ?: return
        val existing = todo.recurrenceId?.let { recurrences.byId(it) }

        val ruleId = when {
            rule == null -> {
                existing?.let { recurrences.delete(it.id) }
                null
            }

            existing != null -> {
                recurrences.update(rule.copy(id = existing.id))
                existing.id
            }

            else -> recurrences.insert(rule.copy(id = 0))
        }

        dao.update(
            todo.copy(
                bucket = Bucket.GLOWNE,
                plannedDate = date,
                fromSchedule = true,
                recurrenceId = ruleId,
            )
        )
    }

    /** Dopisanie od razu z terminem i ewentualna regula - sciezka z ekranu Plan. */
    suspend fun addScheduled(title: String, date: LocalDate, rule: Recurrence?): Long {
        val id = add(title = title, plannedDate = date, fromSchedule = true)
        if (id > 0 && rule != null) plan(id, date, rule)
        return id
    }

    /**
     * Przenosi miedzy listami. Wyjscie z listy glownej gubi termin - rzecz odlozona
     * "na kiedys" z terminem na wczoraj bylaby sprzecznoscia sama w sobie - a razem
     * z terminem gubi tez cyklicznosc, bo powtarzanie bez daty nie ma od czego liczyc.
     */
    suspend fun moveTo(id: Long, bucket: Bucket) {
        val todo = dao.byId(id) ?: return
        if (bucket == Bucket.GLOWNE) {
            dao.update(todo.copy(bucket = bucket))
            return
        }
        todo.recurrenceId?.let { recurrences.delete(it) }
        dao.update(
            todo.copy(
                bucket = bucket,
                plannedDate = null,
                fromSchedule = false,
                recurrenceId = null,
            )
        )
    }

    /**
     * Odhaczenie zadania cyklicznego generuje kolejna instancje - i tylko wtedy, bo
     * inaczej zalegly czynsz rozmnozylby sie na cztery kopie. Cofniecie w oknie doby
     * musi te instancje skasowac, zeby po przypadkowym klikniecu nie zostaly dwie.
     */
    suspend fun setDone(id: Long, done: Boolean, now: Instant = Instant.now(), today: LocalDate = LocalDate.now()) {
        val todo = dao.byId(id) ?: return
        dao.update(todo.copy(completedAt = if (done) now else null))

        val ruleId = todo.recurrenceId ?: return
        if (done) {
            val rule = recurrences.byId(ruleId) ?: return
            dao.insert(
                todo.copy(
                    id = 0,
                    plannedDate = Recurring.next(rule, todo.plannedDate ?: today, today),
                    fromSchedule = true,
                    createdAt = now,
                    completedAt = null,
                )
            )
        } else {
            dao.openByRecurrence(ruleId)
                .filter { it.id != todo.id }
                .forEach { dao.delete(it) }
        }
    }

    /** Usuniecie instancji cyklicznej konczy caly cykl - inaczej regula zostalaby sierota. */
    suspend fun delete(id: Long) {
        val todo = dao.byId(id) ?: return
        todo.recurrenceId?.let { recurrences.delete(it) }
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
