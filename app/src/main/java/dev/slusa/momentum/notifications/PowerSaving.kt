package dev.slusa.momentum.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/**
 * Pulapka Samsunga.
 *
 * Galaxy agresywnie usypia aplikacje i notorycznie zabija tym zaplanowane zadania.
 * Aplikacja nie ma jak sama sie z tego wypisac - moze tylko wykryc stan i zaprowadzic
 * we wlasciwe miejsce. Bez tego przypomnienia po prostu przestaja przychodzic i po
 * dwoch tygodniach wyglada to na zepsuta funkcje, a nie na ustawienie systemu.
 *
 * Lista "uspione aplikacje" u Samsunga nie ma publicznej intencji, wiec zostaje
 * ekran informacji o aplikacji jako najblizsze miejsce, do ktorego da sie zaprowadzic.
 */
object PowerSaving {

    fun isUnrestricted(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Systemowe okno "zezwolic na dzialanie w tle?" - jedno dotkniecie i gotowe. */
    fun requestUnrestricted(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { openAppSettings(context) }
    }

    /** Stad prowadzi droga do listy uspionych aplikacji, ktorej nie da sie otworzyc wprost. */
    fun openAppSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
