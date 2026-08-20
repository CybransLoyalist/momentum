package dev.slusa.momentum.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.slusa.momentum.data.Habit
import dev.slusa.momentum.ui.components.Chip
import java.time.DayOfWeek

private val DAY_LABELS = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")

/**
 * Konfiguracja nawyku. Ekran odwiedzany raz na jakis czas, wiec moze byc gadatliwy -
 * wazniejsze, zeby wszystko dalo sie znalezc, niz zeby dalo sie to zrobic w dwa dotkniecia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorSheet(
    habit: Habit,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(habit.id) { mutableStateOf(habit.name) }
    var daily by remember(habit.id) { mutableStateOf(habit.daily) }
    var days by remember(habit.id) { mutableStateOf(habit.weekdays) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nazwa nawyku") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
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
