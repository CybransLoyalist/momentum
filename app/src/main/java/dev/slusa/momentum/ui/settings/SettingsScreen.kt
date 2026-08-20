package dev.slusa.momentum.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import dev.slusa.momentum.BuildConfig
import dev.slusa.momentum.data.Vacation
import dev.slusa.momentum.ui.components.PickDateDialog
import dev.slusa.momentum.ui.components.ScreenHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("pl"))

@Composable
fun SettingsScreen(
    vacation: Vacation?,
    today: LocalDate,
    onBack: () -> Unit,
    onStartVacation: (LocalDate?) -> Unit,
    onEndVacation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingReturn by remember { mutableStateOf(false) }

    if (pickingReturn) {
        PickDateDialog(
            initial = vacation?.until ?: today.plusWeeks(1),
            today = today,
            confirmLabel = "Ustaw powrót",
            onDismiss = { pickingReturn = false },
            onPicked = {
                onStartVacation(it)
                pickingReturn = false
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "Ustawienia",
                subtitle = "Momentum ${BuildConfig.VERSION_NAME}",
                action = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                VacationCard(
                    vacation = vacation,
                    today = today,
                    onToggle = { on -> if (on) onStartVacation(null) else onEndVacation() },
                    onPickReturn = { pickingReturn = true },
                    onClearReturn = { onStartVacation(null) },
                )
            }
        }
    }
}

@Composable
private fun VacationCard(
    vacation: Vacation?,
    today: LocalDate,
    onToggle: (Boolean) -> Unit,
    onPickReturn: () -> Unit,
    onClearReturn: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val active = vacation != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Tryb urlopowy",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "Wstrzymuje wszystkie nawyki naraz. Dni urlopu nie liczą " +
                            "się ani jako zrobione, ani jako pominięte — momentum stoi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(0.dp))
                Switch(checked = active, onCheckedChange = onToggle)
            }

            if (vacation != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = buildString {
                        append("Trwa od ")
                        append(vacation.from.format(DATE_FORMAT))
                        append(if (vacation.until == null) " · do odwołania" else " do ${vacation.until.format(DATE_FORMAT)}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                )

                Row {
                    TextButton(onClick = onPickReturn, contentPadding = PaddingValues(0.dp)) {
                        Text(if (vacation.until == null) "Ustaw datę powrotu" else "Zmień datę powrotu")
                    }
                    if (vacation.until != null) {
                        Spacer(Modifier.padding(horizontal = 8.dp))
                        TextButton(onClick = onClearReturn, contentPadding = PaddingValues(0.dp)) {
                            Text("Bez daty")
                        }
                    }
                }
            }
        }
    }
}
