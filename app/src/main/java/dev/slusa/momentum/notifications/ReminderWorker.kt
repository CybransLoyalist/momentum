package dev.slusa.momentum.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.slusa.momentum.MomentumApp
import dev.slusa.momentum.data.Bucket
import dev.slusa.momentum.data.Settings
import dev.slusa.momentum.domain.Digest
import dev.slusa.momentum.domain.DigestItem
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Liczy podsumowanie dnia, wystawia powiadomienie i planuje sie na jutro.
 *
 * Przeplanowanie idzie na koncu i bezwarunkowo: gdyby zostalo pominiete przy jakims
 * wyjatku, lancuch przypomnien urwalby sie po cichu i po tygodniu wygladaloby to na
 * "powiadomienia przestaly dzialac".
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MomentumApp
        val slot = runCatching { Slot.valueOf(inputData.getString(KEY_SLOT).orEmpty()) }
            .getOrNull() ?: Slot.RANO

        val settings = app.settings.settings.first()
        val reminder = when (slot) {
            Slot.RANO -> settings.morning
            Slot.POPOLUDNIE -> settings.afternoon
        }

        return try {
            val digest = buildDigest(app, settings, LocalDate.now())

            // Popoludniowy kopniak przy pustej liscie bylby powiadomieniem o niczym.
            // Poranne podsumowanie ma sens takze puste - "dzisiaj czysto" to informacja.
            if (slot == Slot.RANO || !digest.isEmpty) {
                Notifications.showDigest(applicationContext, slot, digest)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        } finally {
            Reminders.schedule(applicationContext, slot, reminder)
        }
    }

    private suspend fun buildDigest(app: MomentumApp, settings: Settings, today: LocalDate): Digest {
        val open = app.todos.open(Bucket.GLOWNE).first()
            .filter { it.plannedDate != null && !it.plannedDate.isAfter(today) }

        val habits = app.habits.active().first()
        val doneToday = app.habits.completionsSince(today).first()
            .filter { it.date == today }
            .map { it.habitId }
            .toSet()

        val onVacation = settings.vacation?.covers(today) == true

        return Digest(
            tasks = open.map { DigestItem(it.id, it.title, habit = false) },
            habits = if (onVacation) {
                emptyList()
            } else {
                habits.filter { it.isScheduledOn(today) && it.id !in doneToday }
                    .map { DigestItem(it.id, it.name, habit = true) }
            },
            overdue = open.count { it.plannedDate!!.isBefore(today) },
        )
    }

    companion object {
        const val KEY_SLOT = "slot"
    }
}
