package dev.slusa.momentum.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.ui.CatchUpHabit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl"))

/**
 * Pytanie o wczorajsze nawyki przy porannym wejsciu do aplikacji.
 *
 * Zaznaczenia startuja **puste**, mimo ze wiekszosc pytan konczy sie zaznaczeniem
 * wszystkiego. Momentum jest jedyna liczba, ktora ta aplikacja obiecuje trzymac uczciwie,
 * a domyslnie zaznaczona lista zamienialaby to okno w przycisk "potwierdz" klikany bez
 * patrzenia - i po tygodniu licznik pokazywalby konsekwencje, ktorej nie bylo.
 */
@Composable
fun CatchUpDialog(
    habits: List<CatchUpHabit>,
    yesterday: LocalDate,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember(habits) { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zrobione wczoraj?") },
        text = {
            Column {
                Text(
                    text = "Te nawyki wypadały ${yesterday.format(DAY_FORMAT)} i nie zostały " +
                        "odhaczone. Zaznacz te, które jednak zrobiłaś — wpis trafi na wczoraj, " +
                        "żeby momentum się zgadzało. Dzisiejsze odhaczasz normalnie.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                habits.forEach { habit ->
                    val checked = habit.id in picked
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                picked = if (checked) picked - habit.id else picked + habit.id
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                picked = if (checked) picked - habit.id else picked + habit.id
                            },
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(picked) }, enabled = picked.isNotEmpty()) {
                Text("Odhacz zaznaczone")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Nie teraz") } },
    )
}
