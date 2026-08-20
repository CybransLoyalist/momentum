package dev.slusa.momentum.domain

import dev.slusa.momentum.data.Bucket
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Routing z mostu glosowego. Zle rozdzielenie objawia sie tym, ze podyktowana rzecz
 * ladauje na innej liscie niz sie spodziewasz - i szukasz jej w zlym miejscu.
 */
class RoutingTest {

    @Test
    fun `slowo kluczowe na poczatku kieruje do zakupow i znika z tytulu`() {
        val routed = Routing.route("kup mleko")

        assertEquals(Bucket.ZAKUPY, routed.bucket)
        assertEquals("mleko", routed.title)
    }

    @Test
    fun `rozpoznawanie nie zwraca uwagi na wielkosc liter`() {
        assertEquals(Bucket.ZAKUPY, Routing.route("Kup chleb").bucket)
        assertEquals(Bucket.ZAKUPY, Routing.route("ZAKUPY pomidory").bucket)
    }

    @Test
    fun `slowo kluczowe w srodku zdania nie kieruje do zakupow`() {
        // To jest zadanie, a nie pozycja na liscie zakupow.
        val routed = Routing.route("zadzwonić do Ani, żeby kupiła mleko")

        assertEquals(Bucket.GLOWNE, routed.bucket)
        assertEquals("zadzwonić do Ani, żeby kupiła mleko", routed.title)
    }

    @Test
    fun `zwykle zadanie idzie na liste glowna bez zmian`() {
        val routed = Routing.route("zapłacić czynsz")

        assertEquals(Bucket.GLOWNE, routed.bucket)
        assertEquals("zapłacić czynsz", routed.title)
    }

    @Test
    fun `interpunkcja po slowie kluczowym nie psuje rozpoznania`() {
        val routed = Routing.route("kup: masło")

        assertEquals(Bucket.ZAKUPY, routed.bucket)
        assertEquals("masło", routed.title)
    }

    @Test
    fun `samo slowo kluczowe zostaje tytulem`() {
        // Pusty tytul bylby gorszy niz doslowne "kup".
        val routed = Routing.route("zakupy")

        assertEquals(Bucket.ZAKUPY, routed.bucket)
        assertEquals("zakupy", routed.title)
    }

    @Test
    fun `wlasne slowa kluczowe zastepuja domyslne`() {
        val own = listOf("sklep")

        assertEquals(Bucket.ZAKUPY, Routing.route("sklep mleko", own).bucket)
        assertEquals(Bucket.GLOWNE, Routing.route("kup mleko", own).bucket)
    }

    @Test
    fun `puste wejscie nie wywraca routingu`() {
        val routed = Routing.route("   ")

        assertEquals(Bucket.GLOWNE, routed.bucket)
        assertEquals("", routed.title)
    }

    @Test
    fun `slowa kluczowe czytaja sie z wiersza tekstu`() {
        assertEquals(listOf("kup", "sklep"), Routing.parseKeywords("kup, sklep"))
        assertEquals(listOf("kup", "sklep"), Routing.parseKeywords(" kup ;sklep "))
    }

    @Test
    fun `pusta lista slow wraca do domyslnych`() {
        // Wyczyszczone pole w ustawieniach ma znaczyc "domyslne", a nie "nic nie
        // rozpoznawaj" - inaczej wszystko po cichu ladowaloby na liscie glownej.
        assertEquals(Routing.DEFAULT_KEYWORDS, Routing.parseKeywords(""))
        assertEquals(Routing.DEFAULT_KEYWORDS, Routing.parseKeywords(null))
        assertEquals(Routing.DEFAULT_KEYWORDS, Routing.parseKeywords("  ,  , "))
    }
}
