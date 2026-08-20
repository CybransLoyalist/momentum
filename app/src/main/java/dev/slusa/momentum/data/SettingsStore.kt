package dev.slusa.momentum.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dev.slusa.momentum.domain.Routing
import java.time.LocalDate
import java.time.LocalTime

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

/** Jedno z dwoch dziennych przypomnien: wlacznik i godzina, osobno dla kazdego. */
data class Reminder(
    val enabled: Boolean,
    val time: LocalTime,
)

data class Settings(
    val vacation: Vacation? = null,
    /** Podsumowanie dnia: ile zadan, ile nawykow, co zalegle. */
    val morning: Reminder = Reminder(true, LocalTime.of(8, 0)),
    /** Kopniak: co zostalo otwarte. */
    val afternoon: Reminder = Reminder(true, LocalTime.of(16, 0)),
    /** Folder na automatyczne kopie, wybrany raz przez systemowy wybor katalogu. */
    val backupFolder: String? = null,
    /** Dzien ostatniej udanej kopii - jedyny sposob, zeby zobaczyc, ze dziala. */
    val lastBackup: LocalDate? = null,
    /** Slowa, po ktorych podyktowana rzecz trafia na liste zakupow zamiast na glowna. */
    val shoppingKeywords: List<String> = Routing.DEFAULT_KEYWORDS,
)

class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val from = prefs[VACATION_FROM]?.let(LocalDate::parse)
        Settings(
            vacation = from?.let { Vacation(it, prefs[VACATION_UNTIL]?.let(LocalDate::parse)) },
            morning = Reminder(
                enabled = prefs[MORNING_ON] ?: true,
                time = prefs[MORNING_AT]?.let(LocalTime::parse) ?: LocalTime.of(8, 0),
            ),
            afternoon = Reminder(
                enabled = prefs[AFTERNOON_ON] ?: true,
                time = prefs[AFTERNOON_AT]?.let(LocalTime::parse) ?: LocalTime.of(16, 0),
            ),
            backupFolder = prefs[BACKUP_FOLDER],
            lastBackup = prefs[LAST_BACKUP]?.let(LocalDate::parse),
            shoppingKeywords = Routing.parseKeywords(prefs[SHOPPING_KEYWORDS]),
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

    suspend fun setMorning(reminder: Reminder) {
        context.dataStore.edit { prefs ->
            prefs[MORNING_ON] = reminder.enabled
            prefs[MORNING_AT] = reminder.time.toString()
        }
    }

    suspend fun setAfternoon(reminder: Reminder) {
        context.dataStore.edit { prefs ->
            prefs[AFTERNOON_ON] = reminder.enabled
            prefs[AFTERNOON_AT] = reminder.time.toString()
        }
    }

    suspend fun setBackupFolder(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(BACKUP_FOLDER) else prefs[BACKUP_FOLDER] = uri
        }
    }

    suspend fun setLastBackup(date: LocalDate) {
        context.dataStore.edit { prefs -> prefs[LAST_BACKUP] = date.toString() }
    }

    /** Puste pole znaczy "domyslne", a nie "nie rozpoznawaj nic" - patrz [Routing]. */
    suspend fun setShoppingKeywords(text: String) {
        context.dataStore.edit { prefs -> prefs[SHOPPING_KEYWORDS] = text }
    }

    private companion object {
        val VACATION_FROM = stringPreferencesKey("urlop_od")
        val VACATION_UNTIL = stringPreferencesKey("urlop_do")
        val MORNING_ON = booleanPreferencesKey("rano_wlaczone")
        val MORNING_AT = stringPreferencesKey("rano_godzina")
        val AFTERNOON_ON = booleanPreferencesKey("popoludnie_wlaczone")
        val AFTERNOON_AT = stringPreferencesKey("popoludnie_godzina")
        val BACKUP_FOLDER = stringPreferencesKey("kopie_folder")
        val LAST_BACKUP = stringPreferencesKey("kopie_ostatnia")
        val SHOPPING_KEYWORDS = stringPreferencesKey("slowa_zakupy")
    }
}
