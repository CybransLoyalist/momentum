package dev.slusa.momentum.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.slusa.momentum.domain.plural

/**
 * KALENDARZOWA - "10. kazdego miesiaca", termin nie zalezy od tego, kiedy odhaczysz.
 * OD_WYKONANIA - "co 3 miesiace od ostatniego razu".
 */
enum class RecurrenceMode { KALENDARZOWA, OD_WYKONANIA }

enum class RecurrenceUnit { DZIEN, TYDZIEN, MIESIAC, ROK }

/**
 * Regula powtarzania. Osobna tabelka, bo cyklicznosc ma wlasne reguly i nie ma po co
 * upychac jej w kolumnach todosa - patrz docs/spec.html.
 *
 * Regula nie trzyma tytulu ani daty ostatniego wykonania. Tytul mieszka na instancji
 * w [Todo] i kopiuje sie do nastepnej, a data odhaczenia jest znana w momencie
 * generowania - kazde dodatkowe pole stanu to kolejna rzecz do rozjechania sie
 * z historia.
 */
@Entity(tableName = "recurrences")
data class Recurrence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val mode: RecurrenceMode = RecurrenceMode.KALENDARZOWA,

    val everyN: Int = 1,

    val unit: RecurrenceUnit = RecurrenceUnit.MIESIAC,

    /**
     * Dzien miesiaca dla trybu kalendarzowego. Przy 31 w lutym schodzi na ostatni
     * dzien miesiaca, ale w kolejnym cyklu wraca na 31 - patrz [dev.slusa.momentum.domain.Recurring].
     */
    val anchorDay: Int? = null,
) {
    /** Etykieta na kafelku, np. "co 3 miesiace od wykonania". */
    fun describe(): String {
        val n = everyN.coerceAtLeast(1)
        val noun = when (unit) {
            RecurrenceUnit.DZIEN -> plural(n, "dzień", "dni", "dni")
            RecurrenceUnit.TYDZIEN -> plural(n, "tydzień", "tygodnie", "tygodni")
            RecurrenceUnit.MIESIAC -> plural(n, "miesiąc", "miesiące", "miesięcy")
            RecurrenceUnit.ROK -> plural(n, "rok", "lata", "lat")
        }
        val base = if (n == 1) "co $noun" else "co $n $noun"
        return if (mode == RecurrenceMode.OD_WYKONANIA) "$base od wykonania" else base
    }
}
