package dev.slusa.momentum.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.slusa.momentum.MomentumApp
import dev.slusa.momentum.domain.DigestItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Odhaczanie prosto z powiadomienia, bez otwierania aplikacji.
 *
 * Zapis do bazy jest zawieszalny, a odbiornik nie moze na niego czekac w watku
 * glownym - stad [goAsync], ktore trzyma proces przy zyciu do momentu wywolania
 * finish(). Bez tego Android moglby ubic proces w polowie zapisu.
 */
class DoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DONE) return

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val habit = intent.getBooleanExtra(EXTRA_HABIT, false)

        val app = context.applicationContext as MomentumApp
        val pending = goAsync()

        // Kafelek znika od razu, jeszcze przed zapisem - dotkniecie ma byc natychmiast
        // widoczne, a zapis do lokalnej bazy nie ma realnej szansy sie nie udac.
        Notifications.dismiss(context, DigestItem(id, "", habit))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (habit) {
                    app.habits.setDone(id, LocalDate.now(), true)
                } else {
                    app.todos.setDone(id, true)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "dev.slusa.momentum.ODHACZ"
        const val EXTRA_ID = "id"
        const val EXTRA_HABIT = "nawyk"
    }
}
