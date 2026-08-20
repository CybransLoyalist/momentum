package dev.slusa.momentum.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.data.Bucket
import dev.slusa.momentum.data.Recurrence
import dev.slusa.momentum.data.RecurrenceMode
import dev.slusa.momentum.data.RecurrenceUnit
import dev.slusa.momentum.domain.Recurring
import dev.slusa.momentum.ui.TodoUi
import dev.slusa.momentum.ui.components.Chip
import dev.slusa.momentum.ui.components.PickDateDialog
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
 * Co edytor obsluguje: nazwe, termin i powtarzanie. Celowo nie przenoszenie miedzy
 * listami - przestawienie listy na zakupy po cichu zabraloby zadaniu date i cykl,
 * a od przenoszenia jest arkusz akcji, gdzie widac, ze to osobna decyzja.
 */
data class EditorTarget(
    /** null oznacza nowe zadanie. */
    val item: TodoUi? = null,
    /** Tytul przepisany z paska dodawania, zeby nic nie przepadlo po drodze. */
    val draftTitle: String = "",
)

/**
 * Pelny ekran tworzenia i edycji zadania.
 *
 * Powstal, bo powtarzanie wcisniete pod kalendarz w okienku nachodzilo na rozwijana
 * liste miesiecy - a konfiguracja cyklu potrzebuje miejsca, ktorego okno dialogowe
 * nie ma. Sekcje odslaniaja sie kaskadowo, wiec zwykle zadanie z data to trzy rzeczy
 * na ekranie, a cala maszyneria cyklicznosci wychodzi dopiero, gdy jest do czego.
 */
@Composable
fun TaskEditorScreen(
    target: EditorTarget,
    today: LocalDate,
    onCancel: () -> Unit,
    onSave: (title: String, date: LocalDate?, rule: Recurrence?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val existing = target.item
    val isNew = existing == null

    // Termin i cykl istnieja tylko na liscie glownej. Przy edycji rzeczy z Kiedys
    // albo z zakupow zostaje samo pole nazwy - reszta nie ma tam znaczenia.
    val schedulable = existing == null || existing.todo.bucket == Bucket.GLOWNE

    var title by remember { mutableStateOf(existing?.todo?.title ?: target.draftTitle) }
    var date by remember {
        mutableStateOf(
            if (isNew) today.plusDays(1) else existing.todo.plannedDate
        )
    }

    var repeats by remember { mutableStateOf(existing?.rule != null) }
    var mode by remember { mutableStateOf(existing?.rule?.mode ?: RecurrenceMode.KALENDARZOWA) }
    var everyN by remember { mutableStateOf(existing?.rule?.everyN ?: 1) }
    var unit by remember { mutableStateOf(existing?.rule?.unit ?: RecurrenceUnit.MIESIAC) }
    var anchorDay by remember {
        mutableStateOf(existing?.rule?.anchorDay ?: (date ?: today).dayOfMonth)
    }

    // Kotwica dnia miesiaca ma sens tylko tam, gdzie miesiac w ogole wystepuje.
    val anchored = mode == RecurrenceMode.KALENDARZOWA &&
        (unit == RecurrenceUnit.MIESIAC || unit == RecurrenceUnit.ROK)

    // Data i kotwica chodza razem w obie strony. Wybrany dzien podstawia sie pod
    // kotwice, dopoki nie ruszysz jej recznie - a recznie wpisana kotwica przesuwa
    // pierwszy termin na najblizsze jej wystapienie. Bez tego drugiego kierunku
    // "co miesiac, 15." zapisane z domyslna data wypadalo jutro i dopiero kolejny
    // cykl trafial na pietnastego.
    var anchorTouched by remember { mutableStateOf(existing?.rule?.anchorDay != null) }
    LaunchedEffect(date) {
        if (!anchorTouched) date?.let { anchorDay = it.dayOfMonth }
    }

    // Powtarzanie bez daty nie ma od czego liczyc nastepnego razu.
    LaunchedEffect(date) {
        if (date == null) repeats = false
    }

    val rule = if (repeats && date != null) {
        Recurrence(
            id = existing?.rule?.id ?: 0,
            mode = mode,
            everyN = everyN,
            unit = unit,
            anchorDay = if (anchored) anchorDay else null,
        )
    } else {
        null
    }

    var pickingDate by remember { mutableStateOf(false) }
    if (pickingDate) {
        PickDateDialog(
            initial = date ?: today.plusDays(1),
            today = today,
            confirmLabel = "Ustaw termin",
            onDismiss = { pickingDate = false },
            onPicked = {
                date = it
                pickingDate = false
            },
        )
    }

    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (isNew) focus.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Anuluj",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = if (isNew) "Nowe zadanie" else "Zadanie",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { onSave(title.trim(), date, rule) },
                    enabled = title.isNotBlank(),
                ) { Text("Zapisz") }
            }
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nazwa zadania") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )

            if (!schedulable) return@Column

            Spacer(Modifier.height(24.dp))
            Label("TERMIN")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { pickingDate = true },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = date?.format(FULL_DATE) ?: "Bez terminu",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (date == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Skroty na najczestsze terminy - bez nich kazde zadanie wymagaloby
            // otwierania kalendarza, a to sa dwa przypadki na trzy.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("dziś", active = date == today) { date = today }
                Chip("jutro", active = date == today.plusDays(1)) { date = today.plusDays(1) }
                Chip("za tydzień", active = date == today.plusWeeks(1)) {
                    date = today.plusWeeks(1)
                }
                Chip("bez terminu", active = date == null) { date = null }
            }

            if (date != null) {
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { repeats = !repeats }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = repeats, onCheckedChange = { repeats = it })
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Powtarza się",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Kopia do lokalnej zmiennej: stan Compose jest wlasciwoscia delegowana,
            // wiec kompilator nie przepuszcza go przez sprawdzenie na null.
            val due = date
            if (rule != null && due != null) {
                Spacer(Modifier.height(12.dp))

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
                    Spacer(Modifier.height(20.dp))
                    Label("KTÓREGO")

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NumberField(value = anchorDay, range = 1..31) {
                            anchorDay = it
                            anchorTouched = true
                            date = firstOccurrence(it, unit, date, today)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "dnia miesiąca",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    if (anchorDay > 28) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "W krótszych miesiącach termin zejdzie na ostatni dzień, " +
                                "a w kolejnym wróci na $anchorDay.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Roznica miedzy trybami jest subtelna i latwo ja przegapic - jedno
                // zdanie z konkretna data mowi wiecej niz opis obu trybow.
                Spacer(Modifier.height(20.dp))
                Label("PO ODHACZENIU WRÓCI")
                Text(
                    text = Recurring.next(rule, due, due).format(FULL_DATE),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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

private fun onDay(base: java.time.LocalDate, day: Int): java.time.LocalDate =
    base.withDayOfMonth(day.coerceIn(1, base.lengthOfMonth()))

/**
 * Najblizszy termin pasujacy do kotwicy dnia miesiaca, liczac od dzisiaj.
 *
 * Dla powtarzania rocznego miesiac bierze sie z juz wybranej daty - "29 lutego co rok"
 * ma zostac lutym, a nie przeskoczyc na biezacy miesiac.
 */
private fun firstOccurrence(
    day: Int,
    unit: RecurrenceUnit,
    current: java.time.LocalDate?,
    today: java.time.LocalDate,
): java.time.LocalDate = when (unit) {
    RecurrenceUnit.ROK -> {
        val candidate = onDay(current ?: today, day)
        if (candidate.isBefore(today)) onDay(candidate.plusYears(1), day) else candidate
    }

    else -> {
        val candidate = onDay(today, day)
        if (candidate.isBefore(today)) onDay(today.plusMonths(1), day) else candidate
    }
}
