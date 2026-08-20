package dev.slusa.momentum.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.slusa.momentum.data.Reminder
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** Ktore z dwoch dziennych przypomnien. */
enum class Slot(val key: String) {
    RANO("przypomnienie_rano"),
    POPOLUDNIE("przypomnienie_popoludnie"),
}

/**
 * Planowanie dwoch dziennych przypomnien.
 *
 * WorkManager nie ma zadania cyklicznego o konkretnej godzinie - okresowe potrafi
 * odpalic sie kiedykolwiek w oknie. Uzywamy wiec zadania jednorazowego z opoznieniem
 * do najblizszego wystapienia, a po wykonaniu planuje ono samo siebie na nastepny
 * dzien. Kolejka WorkManagera przezywa restart telefonu, wiec nie trzeba wlasnego
 * odbiornika na start systemu.
 *
 * Czego to nie przeskoczy: Samsung usypia aplikacje i wtedy zadanie po prostu nie
 * odpala. Ekran ustawien to wykrywa i o tym przypomina - patrz [PowerSaving].
 */
object Reminders {

    const val CHANNEL_ID = "momentum_przypomnienia"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Przypomnienia dnia",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Podsumowanie o poranku i kopniak po południu."
            }
        )
    }

    fun scheduleAll(context: Context, morning: Reminder, afternoon: Reminder) {
        schedule(context, Slot.RANO, morning)
        schedule(context, Slot.POPOLUDNIE, afternoon)
    }

    fun schedule(context: Context, slot: Slot, reminder: Reminder, now: LocalDateTime = LocalDateTime.now()) {
        val work = WorkManager.getInstance(context)
        if (!reminder.enabled) {
            work.cancelUniqueWork(slot.key)
            return
        }

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayUntil(reminder.time, now))
            .setInputData(Data.Builder().putString(ReminderWorker.KEY_SLOT, slot.name).build())
            .addTag(slot.key)
            .build()

        // REPLACE, a nie KEEP: zmiana godziny w ustawieniach ma przestawic czekajace
        // zadanie, a nie doczekac starej pory i dopiero potem sie poprawic.
        work.enqueueUniqueWork(slot.key, ExistingWorkPolicy.REPLACE, request)
    }

    /** Ile zostalo do najblizszego wystapienia godziny - dzisiaj albo jutro. */
    fun delayUntil(time: LocalTime, now: LocalDateTime): Duration {
        val todayAt = now.toLocalDate().atTime(time)
        val next = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
        return Duration.between(now, next)
    }
}
