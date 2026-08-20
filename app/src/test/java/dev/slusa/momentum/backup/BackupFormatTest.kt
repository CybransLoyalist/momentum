package dev.slusa.momentum.backup

import dev.slusa.momentum.data.Bucket
import dev.slusa.momentum.data.Habit
import dev.slusa.momentum.data.HabitCompletion
import dev.slusa.momentum.data.Recurrence
import dev.slusa.momentum.data.RecurrenceMode
import dev.slusa.momentum.data.RecurrenceUnit
import dev.slusa.momentum.data.ShoppingList
import dev.slusa.momentum.data.Todo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Kopia zapasowa to jedyna funkcja, w ktorej ciche zepsucie oznacza utrate danych -
 * i dowiadujesz sie o tym dopiero wtedy, gdy juz ich potrzebujesz. Stad test na to,
 * ze zapis i odczyt daja dokladnie to samo.
 */
class BackupFormatTest {

    private val sample = BackupData(
        todos = listOf(
            Todo(
                id = 1,
                title = "Czynsz",
                bucket = Bucket.GLOWNE,
                plannedDate = LocalDate.of(2026, 9, 10),
                fromSchedule = true,
                sortIndex = -3,
                createdAt = Instant.ofEpochMilli(1_700_000_000_000),
                completedAt = null,
                recurrenceId = 7,
            ),
            Todo(
                id = 2,
                title = "Mleko",
                bucket = Bucket.ZAKUPY,
                plannedDate = null,
                sortIndex = 0,
                createdAt = Instant.ofEpochMilli(1_700_000_100_000),
                completedAt = Instant.ofEpochMilli(1_700_000_200_000),
            ),
            Todo(
                id = 3,
                title = "Regał Kallax",
                bucket = Bucket.ZAKUPY,
                plannedDate = null,
                sortIndex = -1,
                createdAt = Instant.ofEpochMilli(1_700_000_300_000),
                shoppingListId = 9,
            ),
        ),
        recurrences = listOf(
            Recurrence(
                id = 7,
                mode = RecurrenceMode.KALENDARZOWA,
                everyN = 1,
                unit = RecurrenceUnit.MIESIAC,
                anchorDay = 10,
            ),
            Recurrence(
                id = 8,
                mode = RecurrenceMode.OD_WYKONANIA,
                everyN = 3,
                unit = RecurrenceUnit.TYDZIEN,
                anchorDay = null,
            ),
        ),
        habits = listOf(
            Habit(
                id = 4,
                name = "Siłownia",
                daily = false,
                weekdaysMask = Habit.maskOf(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)),
                archived = false,
                sortIndex = 1,
                startDate = LocalDate.of(2026, 1, 5),
            ),
        ),
        shoppingLists = listOf(
            ShoppingList(id = 9, name = "Ikea", sortIndex = 1),
        ),
        completions = listOf(
            HabitCompletion(habitId = 4, date = LocalDate.of(2026, 8, 18)),
            HabitCompletion(habitId = 4, date = LocalDate.of(2026, 8, 20)),
        ),
    )

    @Test
    fun `zapis i odczyt daja to samo`() {
        val back = BackupFormat.fromJson(BackupFormat.toJson(sample))

        assertEquals(sample.todos, back.todos)
        assertEquals(sample.recurrences, back.recurrences)
        assertEquals(sample.habits, back.habits)
        assertEquals(sample.completions.toSet(), back.completions.toSet())
        assertEquals(sample.shoppingLists, back.shoppingLists)
    }

    @Test
    fun `przypisanie do podlisty zakupow przezywa obieg`() {
        val back = BackupFormat.fromJson(BackupFormat.toJson(sample))

        assertEquals(9L, back.todos.first { it.id == 3L }.shoppingListId)
        assertNull(back.todos.first { it.id == 2L }.shoppingListId)
    }

    @Test
    fun `puste pola przezywaja obieg jako puste, a nie jako smieci`() {
        val back = BackupFormat.fromJson(BackupFormat.toJson(sample))
        val zakup = back.todos.first { it.id == 2L }

        assertNull(zakup.plannedDate)
        assertNull(zakup.recurrenceId)
        assertEquals(false, zakup.fromSchedule)
    }

    @Test
    fun `kopia z brakujacymi polami wczytuje sie na wartosciach domyslnych`() {
        // Plik sprzed zmiany formatu: sam tytul, reszty nie ma.
        val json = """{"wersja":1,"todosy":[{"id":5,"tytul":"Coś"}]}"""

        val back = BackupFormat.fromJson(json)

        assertEquals(1, back.todos.size)
        assertEquals("Coś", back.todos.first().title)
        assertEquals(Bucket.GLOWNE, back.todos.first().bucket)
        assertNull(back.todos.first().plannedDate)
    }

    @Test
    fun `nieznana wartosc pola wyliczeniowego nie wywala calej kopii`() {
        val json = """{"todosy":[{"id":5,"tytul":"Coś","lista":"NIE_MA_TAKIEJ"}]}"""

        assertEquals(Bucket.GLOWNE, BackupFormat.fromJson(json).todos.first().bucket)
    }

    @Test
    fun `pusty plik daje pusta kopie, a nie wyjatek`() {
        val back = BackupFormat.fromJson("""{"wersja":1}""")

        assertEquals(0, back.todos.size)
        assertEquals(0, back.habits.size)
        assertEquals(0, back.completions.size)
        assertEquals(0, back.recurrences.size)
    }
}
