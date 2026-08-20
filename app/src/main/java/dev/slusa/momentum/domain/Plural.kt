package dev.slusa.momentum.domain

/**
 * Polska odmiana przez liczby: 1 zadanie, 2 zadania, 5 zadan, 22 zadania, 12 zadan.
 *
 * Regula lapie tez wyjatek nastolatkow - 12, 13 i 14 ida z forma mnoga, mimo ze
 * koncza sie na 2, 3 i 4.
 */
fun plural(n: Int, one: String, few: String, many: String): String = when {
    n == 1 -> one
    n % 10 in 2..4 && n % 100 !in 12..14 -> few
    else -> many
}

/** To samo z liczba z przodu, np. "3 zadania". */
fun counted(n: Int, one: String, few: String, many: String): String =
    "$n ${plural(n, one, few, many)}"
