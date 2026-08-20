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
import dev.slusa.momentum.domain.DigestItem

/**
 * Wystawianie i sprzatanie powiadomien.
 *
 * Uklad jest grupowy: jedno podsumowanie plus po jednym powiadomieniu na rzecz do
 * zrobienia, kazde z przyciskiem "Zrobione". Alternatywa - jedno powiadomienie z lista
 * w srodku - odpada, bo Android pozwala na najwyzej trzy przyciski na powiadomienie,
 * a odhaczanie prosto z rozwinietego powiadomienia jest tu calym sensem.
 */
object Notifications {

    private const val GROUP = "momentum_dzien"
    private const val SUMMARY_ID = 1
    private const val TODO_BASE = 10_000
    private const val HABIT_BASE = 20_000

    fun showDigest(context: Context, slot: Slot, digest: Digest) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        // Poprzednia porcja znika w calosci: o szesnastej ma byc widac stan z szesnastej,
        // a nie poranna liste z doklejonymi zmianami.
        clearAll(context)

        val title = if (slot == Slot.RANO) digest.morningTitle() else digest.afternoonTitle()

        val style = NotificationCompat.InboxStyle()
        style.setBigContentTitle(title)
        digest.overdueLine()?.let { style.setSummaryText(it) }
        digest.lines().forEach { style.addLine(it) }

        val summary = base(context)
            .setContentTitle(title)
            .setContentText(digest.overdueLine() ?: digest.lines().joinToString(", "))
            .setStyle(style)
            .setGroup(GROUP)
            .setGroupSummary(true)
            .build()

        val items = digest.items.take(Digest.MAX_ITEMS).map { item ->
            idOf(item) to base(context)
                .setContentTitle(item.title)
                .setContentText(if (item.habit) "Nawyk na dziś" else "Na dziś")
                .setGroup(GROUP)
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "Zrobione",
                    doneIntent(context, item),
                )
                .build()
        }

        runCatching {
            items.forEach { (id, notification) -> manager.notify(id, notification) }
            manager.notify(SUMMARY_ID, summary)
        }
    }

    /**
     * Po odhaczeniu z powiadomienia znika sam kafelek tej rzeczy, a gdy byla ostatnia -
     * takze podsumowanie. Bez tego zostawaloby wiszace "Dzisiaj 3 zadania" nad pusta grupa.
     */
    fun dismiss(context: Context, item: DigestItem) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(idOf(item))

        val remaining = manager.activeNotifications
            .filter { it.notification.group == GROUP && it.id != SUMMARY_ID }
        if (remaining.isEmpty()) manager.cancel(SUMMARY_ID)
    }

    fun clearAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.activeNotifications
            .filter { it.notification.group == GROUP }
            .forEach { manager.cancel(it.id) }
    }

    private fun base(context: Context) = NotificationCompat.Builder(context, Reminders.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentIntent(openApp(context))
        .setAutoCancel(true)

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun doneIntent(context: Context, item: DigestItem): PendingIntent = PendingIntent.getBroadcast(
        context,
        idOf(item),
        Intent(context, DoneReceiver::class.java)
            .setAction(DoneReceiver.ACTION_DONE)
            .putExtra(DoneReceiver.EXTRA_ID, item.id)
            .putExtra(DoneReceiver.EXTRA_HABIT, item.habit),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun idOf(item: DigestItem): Int =
        ((if (item.habit) HABIT_BASE else TODO_BASE) + item.id).toInt()
}
