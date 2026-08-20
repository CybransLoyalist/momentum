package dev.slusa.momentum

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.slusa.momentum.ui.MomentumShell
import dev.slusa.momentum.ui.MomentumViewModel
import dev.slusa.momentum.ui.theme.MomentumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as MomentumApp

        setContent {
            MomentumTheme {
                AskForNotifications()

                val vm: MomentumViewModel =
                    viewModel(factory = MomentumViewModel.factory(app.todos, app.habits, app.settings, app.backups))
                MomentumShell(vm)
            }
        }
    }
}

/**
 * Zgode na powiadomienia bierzemy przy starcie, a nie dopiero w ustawieniach - bez
 * niej przypomnienia po cichu nic nie robia, a nikt nie zaglada do ustawien po to,
 * zeby wlaczyc cos, o czym nie wie, ze jest wylaczone. Odmowa nie wraca natretnie:
 * system i tak pokazuje okno tylko raz, a potem zostaje karta w ustawieniach.
 */
@Composable
private fun AskForNotifications() {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
