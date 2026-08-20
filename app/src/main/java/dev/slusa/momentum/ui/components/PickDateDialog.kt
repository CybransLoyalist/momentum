package dev.slusa.momentum.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
