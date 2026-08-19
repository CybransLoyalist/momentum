package dev.slusa.momentum.ui.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.data.Habit
import dev.slusa.momentum.ui.components.PickDateDialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_LABELS = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("pl"))

/**
 * Konfiguracja nawyku. Ekran odwiedzany raz na jakis czas, wiec moze byc gadatliwy -
 * wazniejsze, zeby wszystko dalo sie znalezc, niz zeby dalo sie to zrobic w dwa dotkniecia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorSheet(
    habit: Habit,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onPause: (LocalDate?) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(habit.id) { mutableStateOf(habit.name) }
    var daily by remember(habit.id) { mutableStateOf(habit.daily) }
    var days by remember(habit.id) { mutableStateOf(habit.weekdays) }
    var pickingPause by remember { mutableStateOf(false) }

    if (pickingPause) {
        PickDateDialog(
            initial = habit.pausedTo ?: today.plusWeeks(1),
            onDismiss = { pickingPause = false },
            onPicked = {
                onPause(it)
                pickingPause = false
            },
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nazwa nawyku") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "KIEDY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Codziennie", active = daily) { daily = true }
                Chip("Wybrane dni", active = !daily) { daily = false }
            }

            if (!daily) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DayOfWeek.entries.forEachIndexed { index, day ->
                        Chip(
                            label = DAY_LABELS[index],
                            active = day in days,
                            modifier = Modifier.weight(1f),
                        ) {
                            days = if (day in days) days - day else days + day
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))

            if (habit.pausedTo != null && !habit.pausedTo.isBefore(today)) {
                Text(
                    text = "Pauza do ${habit.pausedTo.format(DATE_FORMAT)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { onPause(null) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("Zakończ pauzę")
                }
            } else {
                TextButton(onClick = { pickingPause = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("Tryb urlopowy — wstrzymaj do dnia…")
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onArchive, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text(if (habit.archived) "Przywróć do obiegu" else "Zarchiwizuj (historia zostaje)")
            }

            TextButton(onClick = onDelete, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("Usuń razem z historią", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    onSave(
                        habit.copy(
                            name = name.trim().ifEmpty { habit.name },
                            daily = daily,
                            weekdaysMask = Habit.maskOf(days),
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && (daily || days.isNotEmpty()),
            ) {
                Text("Zapisz")
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (active) scheme.primaryContainer else Color.Transparent,
        border = BorderStroke(1.dp, if (active) Color.Transparent else scheme.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
