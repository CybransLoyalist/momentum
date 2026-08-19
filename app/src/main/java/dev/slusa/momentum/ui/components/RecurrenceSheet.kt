package dev.slusa.momentum.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.data.Recurrence
import dev.slusa.momentum.data.RecurrenceMode
import dev.slusa.momentum.data.RecurrenceUnit
import dev.slusa.momentum.domain.Recurring
import dev.slusa.momentum.ui.TodoUi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FULL_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag("pl"))

private val UNIT_LABELS = mapOf(
    RecurrenceUnit.DZIEN to "dni",
    RecurrenceUnit.TYDZIEN to "tygodnie",
    RecurrenceUnit.MIESIAC to "miesiące",
    RecurrenceUnit.ROK to "lata",
)

/**
 * Konfiguracja powtarzania. Roznica miedzy trybami jest subtelna i latwo ja przegapic,
 * wiec arkusz pokazuje na dole wyliczony termin nastepnego razu - jedno zdanie mowi
 * wiecej niz opis obu trybow razem wziety.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceSheet(
    item: TodoUi,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Recurrence) -> Unit,
    onClear: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dueDate = item.todo.plannedDate ?: today

    var mode by remember(item.todo.id) {
        mutableStateOf(item.rule?.mode ?: RecurrenceMode.KALENDARZOWA)
    }
    var everyN by remember(item.todo.id) { mutableStateOf(item.rule?.everyN ?: 1) }
    var unit by remember(item.todo.id) {
        mutableStateOf(item.rule?.unit ?: RecurrenceUnit.MIESIAC)
    }
    var anchorDay by remember(item.todo.id) {
        mutableStateOf(item.rule?.anchorDay ?: dueDate.dayOfMonth)
    }

    // Kotwica dnia miesiaca ma sens tylko tam, gdzie miesiac w ogole wystepuje.
    val anchored = mode == RecurrenceMode.KALENDARZOWA &&
        (unit == RecurrenceUnit.MIESIAC || unit == RecurrenceUnit.ROK)

    val draft = Recurrence(
        id = item.rule?.id ?: 0,
        mode = mode,
        everyN = everyN,
        unit = unit,
        anchorDay = if (anchored) anchorDay else null,
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {

            Text(
                text = item.todo.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(20.dp))

            Label("JAK LICZYMY")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Wg kalendarza", active = mode == RecurrenceMode.KALENDARZOWA) {
                    mode = RecurrenceMode.KALENDARZOWA
                }
                Chip("Od wykonania", active = mode == RecurrenceMode.OD_WYKONANIA) {
                    mode = RecurrenceMode.OD_WYKONANIA
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (mode == RecurrenceMode.KALENDARZOWA) {
                    "Termin nie zależy od tego, kiedy odhaczysz."
                } else {
                    "Następny raz liczy się od dnia odhaczenia."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            Label("CO ILE")
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberField(value = everyN, range = 1..99) { everyN = it }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = UNIT_LABELS.getValue(unit),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RecurrenceUnit.entries.forEach { entry ->
                    Chip(
                        label = UNIT_LABELS.getValue(entry),
                        active = unit == entry,
                        modifier = Modifier.weight(1f),
                    ) { unit = entry }
                }
            }

            if (anchored) {
                Spacer(Modifier.height(20.dp))
                Label("KTÓREGO")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberField(value = anchorDay, range = 1..31) { anchorDay = it }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "dnia miesiąca",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (anchorDay > 28) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "W krótszych miesiącach termin zejdzie na ostatni dzień " +
                            "i w kolejnym wróci na $anchorDay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Po odhaczeniu wróci: " +
                    Recurring.next(draft, dueDate, today).format(FULL_DATE),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (item.rule != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClear, contentPadding = PaddingValues(0.dp)) {
                    Text("Przestań powtarzać")
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onSave(draft) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Zapisz")
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
}

/**
 * Liczba wpisywana z klawiatury numerycznej. Przyciski plus-minus wygladaly czysciej,
 * ale dobicie nimi do trzydziestego pierwszego to trzydziesci dotkniec - przy tym
 * zakresie wpisanie dwoch cyfr wygrywa bezapelacyjnie.
 */
@Composable
private fun NumberField(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }

    // Zmiana z zewnatrz - np. inny tryb podstawil inna kotwice - ma byc widoczna w polu.
    LaunchedEffect(value) {
        if (text.toIntOrNull() != value) text = value.toString()
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(2)
            text = digits
            digits.toIntOrNull()?.takeIf { it in range }?.let(onChange)
        },
        modifier = Modifier
            .width(88.dp)
            // Po wyjsciu z pola wracamy do ostatniej sensownej wartosci, zeby na ekranie
            // nie zostalo puste okienko albo zero, ktorego regula i tak nie przyjela.
            .onFocusChanged { focus -> if (!focus.isFocused) text = value.toString() },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
    )
}
