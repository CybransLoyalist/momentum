package dev.slusa.momentum.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Liczenie opoznienia do najblizszej pory. Latwo tu o blad o jedna dobe, a objawilby
 * sie brakiem powiadomienia przez caly dzien - czyli czyms, co zauwaza sie za pozno.
 */
class RemindersTest {

    private val eight = LocalTime.of(8, 0)

    @Test
    fun `przed pora czeka do dzisiaj`() {
        val now = LocalDateTime.of(2026, 8, 20, 6, 30)

        assertEquals(Duration.ofMinutes(90), Reminders.delayUntil(eight, now))
    }

    @Test
    fun `po porze przeskakuje na jutro`() {
        val now = LocalDateTime.of(2026, 8, 20, 9, 0)

        assertEquals(Duration.ofHours(23), Reminders.delayUntil(eight, now))
    }

    @Test
    fun `dokladnie o porze idzie na jutro, zeby nie odpalic dwa razy`() {
        val now = LocalDateTime.of(2026, 8, 20, 8, 0)

        assertEquals(Duration.ofDays(1), Reminders.delayUntil(eight, now))
    }

    @Test
    fun `tuz przed polnoca liczy sie do poranka`() {
        val now = LocalDateTime.of(2026, 8, 20, 23, 45)

        assertEquals(Duration.ofMinutes(8 * 60 + 15), Reminders.delayUntil(eight, now))
    }
}
