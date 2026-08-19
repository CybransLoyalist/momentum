package dev.slusa.momentum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.ui.theme.LocalAgeRamp
import dev.slusa.momentum.ui.theme.MomentumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MomentumTheme {
                Scaffold { inner ->
                    SmokeTestScreen(Modifier.padding(inner))
                }
            }
        }
    }
}

/**
 * Etap 1 nie ma jeszcze zadnej funkcjonalnosci - ten ekran istnieje po to, zeby
 * potwierdzic, ze caly lancuch dziala: build -> podpis -> GitHub Release ->
 * Obtainium -> telefon. Pokazuje wersje, wiec od razu widac, czy aktualizacja doszla.
 */
@Composable
fun SmokeTestScreen(modifier: Modifier = Modifier) {
    val ramp = LocalAgeRamp.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "MOMENTUM",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Szkielet stoi",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Wersja ${BuildConfig.VERSION_NAME} (kod ${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Rampa starzenia",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ramp.forEach { color ->
                Spacer(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
        Text(
            text = "Dzien 0 po lewej, dzien 7 i dalej po prawej.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SmokeTestPreview() {
    MomentumTheme { SmokeTestScreen() }
}
