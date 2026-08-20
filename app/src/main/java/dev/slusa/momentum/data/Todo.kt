package dev.slusa.momentum.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/** Na ktorej liscie rzecz mieszka. Jedna tabela, trzy ekrany. */
enum class Bucket { GLOWNE, KIEDYS, ZAKUPY }

/**
 * Jedna encja obsluguje "na dzisiaj", "ogolne", "zaplanowane", "kiedys" i zakupy.
 * Rozroznia je [bucket] i [plannedDate] - patrz docs/spec.html.
 */
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val bucket: Bucket = Bucket.GLOWNE,

    /**
     * null = rzecz ogolna, dzis lub wczesniej = na liscie na dzisiaj,
     * data przyszla = zaplanowane.
     *
     * Rollover nie wymaga zadnego zadania w tle: niedokonczone zadanie ma date
     * wczorajsza, a warunek "plannedDate <= dzis" nadal je lapie.
     *
     * To samo pole jest punktem odniesienia dla wieku i koloru paska. Nie trzeba
     * do tego osobnej daty: data zostaje niezmieniona przy rolloverze, a kazde
     * swiadome dzialanie uzytkowniczki - zdjecie z dzisiaj, przelozenie terminu -
     * ma wiek zresetowac, co dzieje sie samo przez nadpisanie tego pola.
     */
    val plannedDate: LocalDate? = null,

    /**
     * Czy rzecz przyszla z terminu ustawionego z wyprzedzeniem, czy zostala
     * recznie oznaczona "na dzisiaj". Rozdziela dwie sekcje na ekranie Dzisiaj.
     */
    val fromSchedule: Boolean = false,

    val sortIndex: Int = 0,

    val createdAt: Instant = Instant.now(),

    val completedAt: Instant? = null,

    /**
     * Wypelnione, jesli to instancja zadania cyklicznego. Instancja jest zawsze jedna:
     * zalegly czynsz przesuwa sie na kolejny dzien jak zwykly todo i normalnie czernieje,
     * a nastepny cykl powstaje dopiero po odhaczeniu tego.
     */
    val recurrenceId: Long? = null,

    /**
     * Na ktorej podliscie zakupow rzecz mieszka. null znaczy liste glowna - patrz
     * [ShoppingList]. Poza koszykiem zakupow pole jest zawsze puste.
     */
    val shoppingListId: Long? = null,
) {
    val isDone: Boolean get() = completedAt != null
}
