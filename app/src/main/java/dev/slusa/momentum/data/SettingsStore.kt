package dev.slusa.momentum.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ustawienia")

/**
 * Urlop obejmuje wszystkie nawyki naraz - to stan calej osoby, nie pojedynczego
 * nawyku. Data konca jest opcjonalna: wybieranie z gory dnia powrotu to zgadywanka,
 * a spozniony powrot oznaczalby kare za dni urlopu.
 */
data class Vacation(
    val from: LocalDate,
    val until: LocalDate?,
) {
    fun covers(date: LocalDate): Boolean =
        !date.isBefore(from) && (until == null || !date.isAfter(until))
}

data class Settings(
    val vacation: Vacation? = null,
)

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val from = prefs[VACATION_FROM]?.let(LocalDate::parse)
        Settings(
            vacation = from?.let { Vacation(it, prefs[VACATION_UNTIL]?.let(LocalDate::parse)) },
        )
    }

    suspend fun startVacation(from: LocalDate, until: LocalDate?) {
        context.dataStore.edit { prefs ->
            prefs[VACATION_FROM] = from.toString()
            if (until == null) prefs.remove(VACATION_UNTIL) else prefs[VACATION_UNTIL] = until.toString()
        }
    }

    suspend fun endVacation() {
        context.dataStore.edit { prefs ->
            prefs.remove(VACATION_FROM)
            prefs.remove(VACATION_UNTIL)
        }
    }

    private companion object {
        val VACATION_FROM = stringPreferencesKey("urlop_od")
        val VACATION_UNTIL = stringPreferencesKey("urlop_do")
    }
}
