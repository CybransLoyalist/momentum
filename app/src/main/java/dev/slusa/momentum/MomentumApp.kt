package dev.slusa.momentum

import android.app.Application
import dev.slusa.momentum.data.HabitRepository
import dev.slusa.momentum.data.MomentumDatabase
import dev.slusa.momentum.data.TodoRepository

/**
 * Aplikacja dla jednej osoby z jedna baza - pelnoprawny kontener wstrzykiwania
 * zaleznosci bylby tu kosztem bez zwrotu. Repozytoria wisza na Application.
 */
class MomentumApp : Application() {

    private val db by lazy { MomentumDatabase.get(this) }

    val todos: TodoRepository by lazy { TodoRepository(db.todoDao()) }

    val habits: HabitRepository by lazy { HabitRepository(db.habitDao()) }
}
