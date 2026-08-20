package dev.slusa.momentum.domain

/** Jedna rzecz do odhaczenia prosto z powiadomienia. */
data class DigestItem(
    val id: Long,
    val title: String,
    val habit: Boolean,
)

/**
 * Tresc powiadomienia policzona z danych, bez dotykania Androida - dzieki temu da
 * sie ja sprawdzic testem zamiast czekaniem do osmej rano.
 *
 * @param overdue ile z [tasks] ma termin sprzed dzisiaj.
 */
data class Digest(
    val tasks: List<DigestItem> = emptyList(),
    val habits: List<DigestItem> = emptyList(),
    val overdue: Int = 0,
) {
    val items: List<DigestItem> get() = tasks + habits

    val isEmpty: Boolean get() = items.isEmpty()

    /** Podsumowanie o poranku: z czym masz dzisiaj do czynienia. */
    fun morningTitle(): String = when {
        isEmpty -> "Dzisiaj czysto"
        tasks.isEmpty() -> "Dzisiaj ${counted(habits.size, "nawyk", "nawyki", "nawyków")}"
        habits.isEmpty() -> "Dzisiaj ${counted(tasks.size, "zadanie", "zadania", "zadań")}"
        else -> "Dzisiaj ${counted(tasks.size, "zadanie", "zadania", "zadań")} " +
            "i ${counted(habits.size, "nawyk", "nawyki", "nawyków")}"
    }

    /**
     * Zaleglosci sa osobna linia, a nie doklejka do tytulu: to jedyna liczba, ktora
     * ma znaczyc nieprzyjemnie, i ginelaby doklejona do reszty.
     */
    fun overdueLine(): String? = when {
        overdue == 0 -> null
        else -> "W tym ${counted(overdue, "zaległe", "zaległe", "zaległych")}"
    }

    /** Popoludniowy kopniak: co jeszcze zostalo otwarte. */
    fun afternoonTitle(): String = when {
        isEmpty -> "Wszystko odhaczone"
        tasks.isEmpty() -> "Zostało ${counted(habits.size, "nawyk", "nawyki", "nawyków")}"
        habits.isEmpty() -> "Zostało ${counted(tasks.size, "zadanie", "zadania", "zadań")}"
        else -> "Zostało ${counted(tasks.size, "zadanie", "zadania", "zadań")} " +
            "i ${counted(habits.size, "nawyk", "nawyki", "nawyków")}"
    }

    /** Lista tytulow pod naglowkiem, gdy powiadomienie jest rozwiniete. */
    fun lines(limit: Int = MAX_ITEMS): List<String> = items.take(limit).map { it.title }

    companion object {
        /** Ponad tyle rzeczy powiadomienie przestaje byc podsumowaniem, a staje sie lista. */
        const val MAX_ITEMS = 6
    }
}
