package dev.slusa.momentum.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.slusa.momentum.MainActivity
import dev.slusa.momentum.R
import dev.slusa.momentum.domain.Digest

/**
 * Jedno powiadomienie na porę, z podsumowaniem i lista pod spodem po rozwinieciu.
 *
 * Wczesniej byla to grupa: podsumowanie plus osobny kafelek na kazda rzecz, kazdy
 * z przyciskiem "Zrobione". Odhaczanie bez otwierania aplikacji dzialalo, ale przy
 * kilku zadaniach poranek zaczynal sie od szescioelementowego stosu powiadomien -
 * czyli od halasu, a nie od informacji. Podsumowanie ma sie przeczytac jednym
 * spojrzeniem, wiec jest jedno.
 */
object Notifications {

    private const val SUMMARY_ID = 1

    fun showDigest(context: Context, slot: Slot, digest: Digest) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val title = if (slot == Slot.RANO) digest.morningTitle() else digest.afternoonTitle()
        val lines = digest.lines()

        val style = NotificationCompat.InboxStyle()
        style.setBigContentTitle(title)
        digest.overdueLine()?.let { style.setSummaryText(it) }
        lines.forEach { style.addLine(it) }

        val notification = NotificationCompat.Builder(context, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(digest.overdueLine() ?: lines.joinToString(", "))
            .setStyle(style)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(SUMMARY_ID, notification) }
    }

    fun clearAll(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(SUMMARY_ID)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
