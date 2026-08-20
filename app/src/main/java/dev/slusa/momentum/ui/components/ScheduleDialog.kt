package dev.slusa.momentum.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
 * Termin i powtarzanie w jednym oknie.
 *
 * Byly osobno i wychodzilo z tego szesc krokow na dodanie czynszu: zwykle zadanie,
 * data, otwarcie arkusza akcji, dopiero regula. Powtarzanie jest wlasnoscia terminu,
 * a nie osobnym bytem, wiec ustawia sie je tam, gdzie termin.
 *
 * @param initialRule regula, ktora zadanie juz ma - null oznacza jednorazowe.
 * @param startWithRecurrence rozwija sekcje powtarzania od razu, gdy wchodzisz tu
 *   po to, zeby ja ustawic, a nie zeby zmienic date.
 * @param onPicked data i regula; regula null oznacza "to sie nie powtarza".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    initial: LocalDate,
    today: LocalDate,
    initialRule: Recurrence? = null,
    startWithRecurrence: Boolean = false,
    confirmLabel: String = "Zaplanuj",
    onDismiss: () -> Unit,
    onPicked: (LocalDate, Recurrence?) -> Unit,
) {
    val state = rememberFutureDatePickerState(initial, today)
    val picked = state.pickedDate()

    var repeats by remember { mutableStateOf(initialRule != null || startWithRecurrence) }
    var mode by remember { mutableStateOf(initialRule?.mode ?: RecurrenceMode.KALENDARZOWA) }
    var everyN by remember { mutableStateOf(initialRule?.everyN ?: 1) }
    var unit by remember { mutableStateOf(initialRule?.unit ?: RecurrenceUnit.MIESIAC) }
    var anchorDay by remember {
        mutableStateOf(initialRule?.anchorDay ?: (picked ?: initial).dayOfMonth)
    }

    // Kotwica dnia miesiaca ma sens tylko tam, gdzie miesiac w ogole wystepuje.
    val anchored = mode == RecurrenceMode.KALENDARZOWA &&
        (unit == RecurrenceUnit.MIESIAC || unit == RecurrenceUnit.ROK)

    // Wybrany dzien podstawia sie pod kotwice, dopoki nie ruszysz jej recznie -
    // klikniecie dziesiatego wrzesnia prawie zawsze znaczy "dziesiatego kazdego".
    var anchorTouched by remember { mutableStateOf(initialRule?.anchorDay != null) }
    LaunchedEffect(picked) {
        if (!anchorTouched) picked?.let { anchorDay = it.dayOfMonth }
    }

    val rule = if (repeats) {
        Recurrence(
            id = initialRule?.id ?: 0,
            mode = mode,
            everyN = everyN,
            unit = unit,
            anchorDay = if (anchored) anchorDay else null,
        )
    } else {
        null
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { picked?.let { onPicked(it, rule) } },
                enabled = picked != null,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
    ) {
        // Naglowek i tytul kalendarza zdjete, zeby zmiescilo sie ponizej powtarzanie.
        DatePicker(
            state = state,
            title = null,
            headline = null,
            showModeToggle = false,
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.outline,
        )

        Column(
            modifier = Modifier
                .heightIn(max = 260.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Powtarzaj",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = repeats, onCheckedChange = { repeats = it })
            }

            if (repeats) {
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Wg kalendarza", active = mode == RecurrenceMode.KALENDARZOWA) {
                        mode = RecurrenceMode.KALENDARZOWA
                    }
                    Chip("Od wykonania", active = mode == RecurrenceMode.OD_WYKONANIA) {
                        mode = RecurrenceMode.OD_WYKONANIA
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "co",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    NumberField(value = everyN, range = 1..99) { everyN = it }
                }

                Spacer(Modifier.height(10.dp))

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
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberField(value = anchorDay, range = 1..31) {
                            anchorDay = it
                            anchorTouched = true
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "dnia miesiąca",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (anchorDay > 28) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "W krótszych miesiącach zejdzie na ostatni dzień, " +
                                "a w kolejnym wróci na $anchorDay.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Roznica miedzy trybami jest subtelna i latwo ja przegapic - jedno
                // zdanie z konkretna data mowi wiecej niz opis obu trybow.
                if (rule != null && picked != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Po odhaczeniu wróci: " +
                            Recurring.next(rule, picked, picked).format(FULL_DATE),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Liczba wpisywana z klawiatury numerycznej. Przyciski plus-minus wygladaly czysciej,
 * ale dobicie nimi do trzydziestego pierwszego to trzydziesci dotkniec - przy tym
 * zakresie wpisanie dwoch cyfr wygrywa bezapelacyjnie.
 */
@Composable
private fun NumberField(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }

    // Zmiana z zewnatrz - np. przestawiona data podstawila inna kotwice - ma byc
    // widoczna w polu.
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
            // Po wyjsciu z pola wracamy do ostatniej sensownej wartosci, zeby nie
            // zostalo puste okienko albo zero, ktorego regula i tak nie przyjela.
            .onFocusChanged { focus -> if (!focus.isFocused) text = value.toString() },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
    )
}

/** Sam wybor dnia, bez powtarzania - tam, gdzie regula nie mialaby sensu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDateDialog(
    initial: LocalDate,
    today: LocalDate,
    confirmLabel: String = "Wybierz",
    onDismiss: () -> Unit,
    onPicked: (LocalDate) -> Unit,
) {
    val state = rememberFutureDatePickerState(initial, today)
    val picked = state.pickedDate()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { picked?.let(onPicked) },
                enabled = picked != null,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        },
    ) {
        DatePicker(state = state)
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Kalendarz bez przeszlosci.
 *
 * Termin w przeszlosci i tak zniknalby z Planu prosto na liste zaleglosci, a data
 * powrotu z urlopu przed dzisiaj nie znaczy nic - w obu przypadkach lepiej, zeby
 * kalendarz w ogole tego nie proponowal. Zaleglosci powstaja same przez rollover
 * i to jest jedyna uczciwa droga do nich.
 *
 * Room trzyma daty jako ISO, picker jako milisekundy UTC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberFutureDatePickerState(
    initial: LocalDate,
    today: LocalDate,
): DatePickerState {
    val minMillis = remember(today) { today.toUtcMillis() }
    val selectable = remember(minMillis, today) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= minMillis
            override fun isSelectableYear(year: Int) = year >= today.year
        }
    }

    return rememberDatePickerState(
        initialSelectedDateMillis = maxOf(initial, today).toUtcMillis(),
        selectableDates = selectable,
    )
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
private fun DatePickerState.pickedDate(): LocalDate? = selectedDateMillis?.let {
    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
}
