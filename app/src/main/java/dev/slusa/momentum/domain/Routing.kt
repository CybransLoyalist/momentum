package dev.slusa.momentum.domain

import dev.slusa.momentum.data.Bucket

/** Gdzie ma trafic podyktowana rzecz i pod jakim tytulem. */
data class Routed(
    val title: String,
    val bucket: Bucket,
)

/**
 * Rozdzielanie tego, co przyszlo z mostu glosowego.
 *
 * Wszystko wchodzi jedna rura, bo Samsung kieruje "dodaj zadanie X" do Google Tasks,
 * a "dodaj do listy zakupow" do Samsung Notes, ktore nie ma zadnego API. Zamiast
 * zmieniac nawyk mowienia, rozdzielamy po swojej stronie - i wtedy routing nie zalezy
 * od tego, co Samsung wymysli w kolejnej aktualizacji.
 */
object Routing {

    val DEFAULT_KEYWORDS = listOf("kup", "kupić", "kupic", "zakupy", "zakup")

    /**
     * Slowo kluczowe liczy sie **tylko na poczatku**, nie gdziekolwiek w zdaniu.
     *
     * Szukanie w calym tekscie wygladalo kuszaco, ale "zadzwonić do Ani, żeby kupiła
     * mleko" to zadanie, a nie pozycja na liscie zakupow. Pierwsze slowo jest tez
     * przewidywalne: mowisz "kup mleko" i wiesz, gdzie to wyladuje, bez zgadywania.
     *
     * Rozpoznane slowo znika z tytulu, bo na liscie zakupow "kup" niczego nie wnosi.
     * Zostaje tylko wtedy, gdy bylo cala trescia - "kup" bez reszty to lepszy tytul
     * niz pusty.
     */
    fun route(raw: String, keywords: List<String> = DEFAULT_KEYWORDS): Routed {
        val text = raw.trim()
        if (text.isEmpty()) return Routed(text, Bucket.GLOWNE)

        val firstWord = text.substringBefore(' ')
        val normalized = firstWord.trim(*PUNCTUATION).lowercase()

        val matches = keywords.any { it.trim().lowercase() == normalized && it.isNotBlank() }
        if (!matches) return Routed(text, Bucket.GLOWNE)

        val rest = text.substringAfter(' ', missingDelimiterValue = "")
            .trimStart(*PUNCTUATION)
            .trim()

        return Routed(rest.ifEmpty { text }, Bucket.ZAKUPY)
    }

    /** Slowa kluczowe trzymamy jako jeden wiersz tekstu, bo tak sie je tez edytuje. */
    fun parseKeywords(text: String?): List<String> {
        val parsed = text.orEmpty()
            .split(',', ';', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return parsed.ifEmpty { DEFAULT_KEYWORDS }
    }

    fun joinKeywords(keywords: List<String>): String = keywords.joinToString(", ")

    private val PUNCTUATION = charArrayOf(':', ',', '.', ';', '!', '?', '-', '–', '—')
}
