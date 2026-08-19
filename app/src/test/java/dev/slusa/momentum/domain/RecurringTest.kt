package dev.slusa.momentum.domain

import dev.slusa.momentum.data.Recurrence
import dev.slusa.momentum.data.RecurrenceMode
import dev.slusa.momentum.data.RecurrenceUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.time.LocalDate
import org.junit.Test

/**
 * Cyklicznosci tez nie da sie sprawdzic klikaniem - roznica miedzy trybami wychodzi
 * dopiero po kilku miesiacach uzywania. Kazdy test odpowiada jednej regule ze spec.
 */
class RecurringTest {

    private fun kalendarzowa(
        everyN: Int = 1,
        unit: RecurrenceUnit = RecurrenceUnit.MIESIAC,
        anchorDay: Int? = null,
    ) = Recurrence(
        id = 1,
        mode = RecurrenceMode.KALENDARZOWA,
        everyN = everyN,
        unit = unit,
        anchorDay = anchorDay,
    )

    private fun odWykonania(everyN: Int, unit: RecurrenceUnit) = Recurrence(
        id = 2,
        mode = RecurrenceMode.OD_WYKONANIA,
        everyN = everyN,
        unit = unit,
    )

    @Test
    fun `codzienne zadanie wraca jutro`() {
        val dzis = LocalDate.of(2026, 8, 19)

        assertEquals(
            LocalDate.of(2026, 8, 20),
            Recurring.next(kalendarzowa(unit = RecurrenceUnit.DZIEN), dzis, dzis),
        )
    }

    @Test
    fun `co dwa tygodnie liczy sie od terminu`() {
        val termin = LocalDate.of(2026, 8, 19)

        assertEquals(
            LocalDate.of(2026, 9, 2),
            Recurring.next(kalendarzowa(everyN = 2, unit = RecurrenceUnit.TYDZIEN), termin, termin),
        )
    }

    @Test
    fun `tryb kalendarzowy nie zalezy od tego, kiedy odhaczysz`() {
        val rule = kalendarzowa(anchorDay = 10)
        val termin = LocalDate.of(2026, 8, 10)

        // Czynsz zaplacony dziewiec dni po terminie i tak wraca dziesiatego wrzesnia.
        assertEquals(
            LocalDate.of(2026, 9, 10),
            Recurring.next(rule, termin, LocalDate.of(2026, 8, 19)),
        )
    }

    @Test
    fun `tryb od wykonania liczy od dnia odhaczenia`() {
        val rule = odWykonania(3, RecurrenceUnit.MIESIAC)

        // Termin byl w maju, klikniete w sierpniu - nastepny raz trzy miesiace pozniej.
        assertEquals(
            LocalDate.of(2026, 11, 19),
            Recurring.next(rule, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 8, 19)),
        )
    }

    @Test
    fun `zalegly cykl dogania terazniejszosc zamiast odtwarzac przeszlosc`() {
        val rule = kalendarzowa(anchorDay = 10)
        val odhaczone = LocalDate.of(2026, 8, 19)

        // Termin z maja, odhaczone w sierpniu. Nastepny ma wypasc we wrzesniu, a nie
        // w czerwcu - inaczej po kazdym odhaczeniu zostawalaby kolejka zaleglosci.
        assertEquals(
            LocalDate.of(2026, 9, 10),
            Recurring.next(rule, LocalDate.of(2026, 5, 10), odhaczone),
        )
    }

    @Test
    fun `wynik zawsze wypada po dniu odhaczenia`() {
        val odhaczone = LocalDate.of(2026, 8, 19)
        val termin = LocalDate.of(2026, 6, 1)

        val rules = listOf(
            kalendarzowa(unit = RecurrenceUnit.DZIEN),
            kalendarzowa(everyN = 2, unit = RecurrenceUnit.TYDZIEN),
            kalendarzowa(anchorDay = 1),
            odWykonania(1, RecurrenceUnit.DZIEN),
        )

        rules.forEach { rule ->
            assertTrue(
                "regula ${rule.describe()} dala termin w przeszlosci",
                Recurring.next(rule, termin, odhaczone).isAfter(odhaczone),
            )
        }
    }

    @Test
    fun `trzydziesty pierwszy schodzi w lutym na ostatni dzien miesiaca`() {
        val rule = kalendarzowa(anchorDay = 31)
        val styczen = LocalDate.of(2026, 1, 31)

        assertEquals(LocalDate.of(2026, 2, 28), Recurring.next(rule, styczen, styczen))
    }

    @Test
    fun `po lutym termin wraca na trzydziesty pierwszy`() {
        val rule = kalendarzowa(anchorDay = 31)
        val luty = LocalDate.of(2026, 2, 28)

        // Bez kotwicy zsunelibysmy sie na 28. i nigdy nie wrocili na koniec miesiaca.
        assertEquals(LocalDate.of(2026, 3, 31), Recurring.next(rule, luty, luty))
    }

    @Test
    fun `dwudziesty dziewiaty lutego w roku nieprzestepnym schodzi na dwudziesty osmy`() {
        val rule = kalendarzowa(unit = RecurrenceUnit.ROK, anchorDay = 29)
        val przestepny = LocalDate.of(2024, 2, 29)

        assertEquals(LocalDate.of(2025, 2, 28), Recurring.next(rule, przestepny, przestepny))
    }

    @Test
    fun `kotwica nie dotyczy trybu od wykonania`() {
        val rule = odWykonania(1, RecurrenceUnit.MIESIAC).copy(anchorDay = 10)
        val odhaczone = LocalDate.of(2026, 8, 19)

        // Miesiac od wykonania to dziewietnasty, a nie skok na dziesiatego.
        assertEquals(
            LocalDate.of(2026, 9, 19),
            Recurring.next(rule, odhaczone, odhaczone),
        )
    }

    @Test
    fun `opis reguly odmienia sie po polsku`() {
        assertEquals("co miesiąc", kalendarzowa().describe())
        assertEquals("co 2 miesiące", kalendarzowa(everyN = 2).describe())
        assertEquals("co 5 miesięcy", kalendarzowa(everyN = 5).describe())
        assertEquals("co 12 dni", kalendarzowa(everyN = 12, unit = RecurrenceUnit.DZIEN).describe())
        assertEquals("co 22 dni", kalendarzowa(everyN = 22, unit = RecurrenceUnit.DZIEN).describe())
        assertEquals("co 3 tygodnie", kalendarzowa(everyN = 3, unit = RecurrenceUnit.TYDZIEN).describe())
        assertEquals("co rok", kalendarzowa(unit = RecurrenceUnit.ROK).describe())
        assertEquals("co 3 lata od wykonania", odWykonania(3, RecurrenceUnit.ROK).describe())
    }
}
