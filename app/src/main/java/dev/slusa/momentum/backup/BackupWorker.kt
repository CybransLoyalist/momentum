package dev.slusa.momentum.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.slusa.momentum.MomentumApp
import dev.slusa.momentum.notifications.Reminders
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Codzienna kopia do wybranego folderu, w nocy.
 *
 * Ta sama mechanika co przy przypomnieniach: zadanie jednorazowe z opoznieniem, ktore
 * po wykonaniu planuje sie na jutro. Bez wybranego folderu nie ma co robic - zadanie
 * i tak sie przeplanowuje, zeby zaczelo dzialac samo, gdy folder w koncu wskazesz.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MomentumApp

        return try {
            val settings = app.settings.settings.first()

            // Zrzut do katalogu aplikacji idzie zawsze, takze bez wskazanego folderu -
            // to on jedzie na Dysk z Auto Backupem i ratuje sytuacje na nowym telefonie.
            app.backups.writeLocalSnapshot()

            val folder = settings.backupFolder
            if (folder != null) {
                val name = app.backups.writeTo(Uri.parse(folder))
                if (name != null) app.settings.setLastBackup(LocalDate.now())
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        } finally {
            schedule(applicationContext)
        }
    }

    companion object {
        private const val WORK = "kopia_dzienna"

        /** Trzecia w nocy: telefon lezy na szafce, a dzien jest juz zamkniety. */
        private val AT: LocalTime = LocalTime.of(3, 0)

        fun schedule(context: Context, now: LocalDateTime = LocalDateTime.now()) {
            val request = OneTimeWorkRequestBuilder<BackupWorker>()
                .setInitialDelay(Reminders.delayUntil(AT, now))
                .addTag(WORK)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
