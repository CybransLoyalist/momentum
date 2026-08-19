package dev.slusa.momentum.domain

import dev.slusa.momentum.data.Recurrence
import dev.slusa.momentum.data.RecurrenceMode
import dev.slusa.momentum.data.RecurrenceUnit
import java.time.LocalDate

/**
 * Wyliczanie terminu nastepnej instancji zadania cyklicznego.
 *
 * Cala cyklicznosc sprowadza sie do tej jednej funkcji, bo w bazie nie ma zadnego
 * stanu poza sama regula: instancja jest jedna, generuje sie dopiero po odhaczeniu,
 * a wszystko, czego potrzeba do policzenia kolejnego terminu, jest znane w tej chwili.
 */
object Recurring {

    /**
     * Termin nastepnej instancji.
     *
     * @param dueDate termin odhaczonej wlasnie instancji - punkt odniesienia trybu
     *   kalendarzowego, w ktorym data nie zalezy od tego, kiedy faktycznie klikniesz.
     * @param completedOn dzien odhaczenia - punkt odniesienia trybu OD_WYKONANIA.
     *
     * Wynik zawsze wypada po [completedOn]. Zadanie zalegle o trzy miesiace dogania
     * wiec terazniejszosc jednym skokiem, zamiast generowac kolejny zalegly termin,
     * ktory po odhaczeniu wygenerowalby nastepny - i tak az do kolejki zaleglosci
     * do przeklikania. Cena: pominiete cykle znikaja bez sladu, i tak ma byc.
     */
    fun next(rule: Recurrence, dueDate: LocalDate, completedOn: LocalDate): LocalDate {
        val base = when (rule.mode) {
            RecurrenceMode.KALENDARZOWA -> dueDate
            RecurrenceMode.OD_WYKONANIA -> completedOn
        }

        var date = advance(base, rule)
        while (!date.isAfter(completedOn)) {
            date = advance(date, rule)
        }
        return date
    }

    /**
     * Jeden skok o [Recurrence.everyN] jednostek.
     *
     * Kotwica dnia miesiaca jest nakladana po skoku, nie przed: bez tego "31. kazdego
     * miesiaca" po jednym lutym zsuwaloby sie na 28. i juz nigdy nie wrocilo na koniec
     * miesiaca. Z kotwica luty jest jedynym miesiacem, w ktorym termin siada wczesniej.
     */
    private fun advance(date: LocalDate, rule: Recurrence): LocalDate {
        val n = rule.everyN.coerceAtLeast(1).toLong()

        val moved = when (rule.unit) {
            RecurrenceUnit.DZIEN -> date.plusDays(n)
            RecurrenceUnit.TYDZIEN -> date.plusWeeks(n)
            RecurrenceUnit.MIESIAC -> date.plusMonths(n)
            RecurrenceUnit.ROK -> date.plusYears(n)
        }

        // Kotwica dotyczy wylacznie trybu kalendarzowego. "Co 3 miesiace od ostatniego
        // razu" ma wypasc trzy miesiace pozniej, a nie wskoczyc na dziesiatego.
        if (rule.mode != RecurrenceMode.KALENDARZOWA) return moved

        val anchor = rule.anchorDay ?: return moved
        return when (rule.unit) {
            RecurrenceUnit.MIESIAC, RecurrenceUnit.ROK ->
                moved.withDayOfMonth(anchor.coerceIn(1, moved.lengthOfMonth()))

            RecurrenceUnit.DZIEN, RecurrenceUnit.TYDZIEN -> moved
        }
    }
}
