package dev.slusa.momentum

import android.app.Application
import dev.slusa.momentum.data.MomentumDatabase
import dev.slusa.momentum.data.TodoRepository

/**
 * Aplikacja dla jednej osoby z jedna baza - pelnoprawny kontener wstrzykiwania
 * zaleznosci bylby tu kosztem bez zwrotu. Repozytorium wisi na Application.
 */
class MomentumApp : Application() {
    val repository: TodoRepository by lazy {
        TodoRepository(MomentumDatabase.get(this).todoDao())
    }
}
