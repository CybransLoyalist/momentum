package dev.slusa.momentum

import android.app.Application
import dev.slusa.momentum.data.HabitRepository
import dev.slusa.momentum.data.MomentumDatabase
import dev.slusa.momentum.data.SettingsStore
import dev.slusa.momentum.data.TodoRepository
import dev.slusa.momentum.notifications.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Aplikacja dla jednej osoby z jedna baza - pelnoprawny kontener wstrzykiwania
 * zaleznosci bylby tu kosztem bez zwrotu. Repozytoria wisza na Application.
 */
class MomentumApp : Application() {

    private val db by lazy { MomentumDatabase.get(this) }

    val todos: TodoRepository by lazy { TodoRepository(db.todoDao(), db.recurrenceDao()) }

    val habits: HabitRepository by lazy { HabitRepository(db.habitDao()) }

    val settings: SettingsStore by lazy { SettingsStore(this) }

    /**
     * Przypomnienia przeplanowuja sie same przy kazdej zmianie ustawien - dzieki temu
     * nie ma osobnej sciezki "zapisz i nie zapomnij przestawic zadania", ktora predzej
     * czy pozniej ktos by ominal.
     */
    override fun onCreate() {
        super.onCreate()
        Reminders.ensureChannel(this)

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            settings.settings
                .map { it.morning to it.afternoon }
                .distinctUntilChanged()
                .collect { (morning, afternoon) ->
                    Reminders.scheduleAll(this@MomentumApp, morning, afternoon)
                }
        }
    }
}
