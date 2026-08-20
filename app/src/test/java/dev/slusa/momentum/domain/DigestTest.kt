package dev.slusa.momentum.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tresci powiadomien nie da sie sprawdzic klikaniem - trzeba by doczekac do osmej rano
 * i miec akurat wlasciwa liczbe zadan. Stad testy na sam tekst.
 */
class DigestTest {

    private fun tasks(n: Int) = (1..n).map { DigestItem(it.toLong(), "Zadanie $it", habit = false) }
    private fun habits(n: Int) = (1..n).map { DigestItem(it.toLong(), "Nawyk $it", habit = true) }

    @Test
    fun `pusty dzien mowi wprost, ze jest czysto`() {
        val digest = Digest()

        assertEquals("Dzisiaj czysto", digest.morningTitle())
        assertEquals("Wszystko odhaczone", digest.afternoonTitle())
        assertEquals(true, digest.isEmpty)
    }

    @Test
    fun `liczebniki odmieniaja sie po polsku`() {
        assertEquals("Dzisiaj 1 zadanie", Digest(tasks = tasks(1)).morningTitle())
        assertEquals("Dzisiaj 2 zadania", Digest(tasks = tasks(2)).morningTitle())
        assertEquals("Dzisiaj 5 zadań", Digest(tasks = tasks(5)).morningTitle())
        assertEquals("Dzisiaj 12 zadań", Digest(tasks = tasks(12)).morningTitle())
        assertEquals("Dzisiaj 22 zadania", Digest(tasks = tasks(22)).morningTitle())
    }

    @Test
    fun `nawyki i zadania lacza sie w jednym zdaniu`() {
        val digest = Digest(tasks = tasks(3), habits = habits(2))

        assertEquals("Dzisiaj 3 zadania i 2 nawyki", digest.morningTitle())
        assertEquals("Zostało 3 zadania i 2 nawyki", digest.afternoonTitle())
    }

    @Test
    fun `sama jedna kategoria nie ciagnie pustej drugiej`() {
        assertEquals("Dzisiaj 1 nawyk", Digest(habits = habits(1)).morningTitle())
        assertEquals("Zostało 4 zadania", Digest(tasks = tasks(4)).afternoonTitle())
    }

    @Test
    fun `zaleglosci sa osobna linia, nie doklejka do tytulu`() {
        val digest = Digest(tasks = tasks(3), overdue = 2)

        // Tytul milczy o zaleglosciach - to jedyna liczba, ktora ma znaczyc
        // nieprzyjemnie, i doklejona do reszty by zginela.
        assertEquals("Dzisiaj 3 zadania", digest.morningTitle())
        assertEquals("W tym 2 zaległe", digest.overdueLine())
    }

    @Test
    fun `bez zaleglosci nie ma linii o zaleglosciach`() {
        assertNull(Digest(tasks = tasks(3)).overdueLine())
    }

    @Test
    fun `pieciu zaleglosciom odmienia sie inaczej niz dwóm`() {
        assertEquals("W tym 1 zaległe", Digest(overdue = 1).overdueLine())
        assertEquals("W tym 3 zaległe", Digest(overdue = 3).overdueLine())
        assertEquals("W tym 5 zaległych", Digest(overdue = 5).overdueLine())
    }

    @Test
    fun `lista w powiadomieniu jest przycieta`() {
        val digest = Digest(tasks = tasks(10))

        assertEquals(Digest.MAX_ITEMS, digest.lines().size)
        assertEquals("Zadanie 1", digest.lines().first())
    }

    @Test
    fun `zadania ida przed nawykami`() {
        val digest = Digest(tasks = tasks(1), habits = habits(1))

        assertEquals(listOf("Zadanie 1", "Nawyk 1"), digest.lines())
    }
}
