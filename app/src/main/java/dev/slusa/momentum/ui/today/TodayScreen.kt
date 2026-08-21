package dev.slusa.momentum.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.BuildConfig
import dev.slusa.momentum.ui.HabitUi
import dev.slusa.momentum.ui.TodayUiState
import dev.slusa.momentum.ui.TodoUi
import dev.slusa.momentum.ui.components.AddBar
import dev.slusa.momentum.ui.components.Banner
import dev.slusa.momentum.ui.components.CollapsibleHeader
import dev.slusa.momentum.ui.components.EmptyState
import dev.slusa.momentum.ui.components.HabitRow
import dev.slusa.momentum.ui.components.ProgressBanner
import dev.slusa.momentum.ui.components.ScreenHeader
import dev.slusa.momentum.ui.components.SectionHeader
import dev.slusa.momentum.ui.components.TodayChip
import dev.slusa.momentum.ui.components.TodoRow
import dev.slusa.momentum.ui.theme.DoneRamp
import dev.slusa.momentum.ui.theme.LocalDoneRamp
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl"))

/**
 * Jeden wiersz ekranu ToDo.
 *
 * Lista jest budowana jako zwykla lista obiektow, a dopiero potem rysowana. Wczesniej
 * powstawala wprost w LazyColumn i nie dalo sie powiedziec, pod ktorym indeksem wyladuje
 * konkretne zadanie - a bez tego nie ma jak przewinac ekranu do wlasnie dopisanej rzeczy.
 */
private sealed interface TodayRow {
    val key: String

    data object Progress : TodayRow {
        override val key: String = "dorobek"
    }

    data object Vacation : TodayRow {
        override val key: String = "urlop"
    }

    data class Header(val title: String, val count: Int) : TodayRow {
        override val key: String = "naglowek-$title"
    }

    data class Task(val item: TodoUi, val section: String) : TodayRow {
        override val key: String = "$section-${item.todo.id}"
    }

    data class Habit(val item: HabitUi) : TodayRow {
        override val key: String = "nawyk-${item.habit.id}"
    }

    data class DoneHeader(val count: Int) : TodayRow {
        override val key: String = "naglowek-zrobione"
    }

    data class DoneHabit(val item: HabitUi) : TodayRow {
        override val key: String = "zrobiony-nawyk-${item.habit.id}"
    }

    data class DoneTask(val item: TodoUi) : TodayRow {
        override val key: String = "zrobione-${item.todo.id}"
    }

    data object Empty : TodayRow {
        override val key: String = "pusto"
    }

    data object Footer : TodayRow {
        override val key: String = "stopka"
    }
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    habits: List<HabitUi>,
    onAdd: (String, Boolean) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onToggleToday: (Long) -> Unit,
    onItemClick: (TodoUi) -> Unit,
    onToggleHabit: (Long, Boolean) -> Unit,
    onHabitClick: (HabitUi) -> Unit,
    vacationActive: Boolean,
    onOpenSettings: () -> Unit,
    lastAddedId: Long?,
    onAddedShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var draftForToday by remember { mutableStateOf(false) }
    var doneExpanded by remember { mutableStateOf(false) }

    // Odhaczony nawyk schodzi do "Zrobionych" tak samo jak odhaczony todos - sekcja
    // "Nawyki" ma pokazywac to, co jeszcze przed toba, a nie to, co juz za toba.
    val openHabits = habits.filter { !it.doneToday }
    val doneHabits = habits.filter { it.doneToday }
    val doneCount = state.done.size + doneHabits.size

    val rows = remember(state, openHabits, doneHabits, vacationActive, doneExpanded) {
        buildRows(state, openHabits, doneHabits, vacationActive, doneExpanded)
    }

    val listState = rememberLazyListState()

    /*
     * Przewijanie do dopisanej rzeczy, a nie na gore ekranu.
     *
     * Rzecz bez "dzis" laduje w sekcji "Ogolne", ktora jest przedostatnia - przewiniecie
     * na sama gore odsuwaloby ja od oka jeszcze bardziej niz brak przewijania. Celujemy
     * w naglowek sekcji, jesli jakis stoi tuz nad rzecza, zeby bylo widac, dokad wpadla.
     *
     * Efekt czeka na liste zawierajaca nowy wiersz: zapis do bazy jest asynchroniczny,
     * wiec w chwili dopisania tego wiersza jeszcze nie ma.
     */
    LaunchedEffect(lastAddedId, rows) {
        val id = lastAddedId ?: return@LaunchedEffect
        val index = rows.indexOfFirst { it is TodayRow.Task && it.item.todo.id == id }
        if (index < 0) return@LaunchedEffect

        val target = if (index > 0 && rows[index - 1] is TodayRow.Header) index - 1 else index
        listState.animateScrollToItem(target)
        onAddedShown()
    }

    val submit = {
        if (draft.isNotBlank()) {
            onAdd(draft, draftForToday)
            draft = ""
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "ToDo",
                subtitle = buildString {
                    append(state.day.format(DATE_FORMAT))
                    append(" · ")
                    // "rzecz" ma te sama forme dla 2 i dla 5, wiec wystarczy rozdzielic jedynke.
                    append(
                        when (state.todayCount) {
                            0 -> "nic na dziś"
                            1 -> "1 rzecz"
                            else -> "${state.todayCount} rzeczy"
                        }
                    )
                },
                action = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ustawienia",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            AddBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Co masz do zrobienia?",
                onSubmit = submit,
                trailing = {
                    TodayChip(active = draftForToday, onClick = { draftForToday = !draftForToday })
                },
            )
        },
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rows, key = { it.key }) { row ->
                when (row) {
                    TodayRow.Progress -> ProgressBanner(
                        done = state.doneToday + doneHabits.size,
                        target = DoneRamp.TARGET,
                        ramp = LocalDoneRamp.current,
                    )

                    TodayRow.Vacation ->
                        Banner("Tryb urlopowy — nawyki wstrzymane", onOpenSettings)

                    is TodayRow.Header -> SectionHeader(row.title, row.count)

                    is TodayRow.Task -> TodoRow(
                        item = row.item,
                        onToggleDone = { onToggleDone(row.item.todo.id, true) },
                        onClick = { onItemClick(row.item) },
                        trailing = {
                            TodayChip(
                                active = row.item.onTodayList,
                                onClick = { onToggleToday(row.item.todo.id) },
                            )
                        },
                    )

                    is TodayRow.Habit -> HabitRow(
                        item = row.item,
                        onToggleDone = { onToggleHabit(row.item.habit.id, true) },
                        onClick = { onHabitClick(row.item) },
                    )

                    is TodayRow.DoneHeader -> CollapsibleHeader(
                        label = "Zrobione (${row.count})",
                        expanded = doneExpanded,
                        onClick = { doneExpanded = !doneExpanded },
                    )

                    // Nawyk zostaje nawykiem takze po odhaczeniu - zwykly wiersz zabralby
                    // pasek momentum, czyli jedyna informacje o tym, co dalo klikniecie.
                    is TodayRow.DoneHabit -> HabitRow(
                        item = row.item,
                        onToggleDone = { onToggleHabit(row.item.habit.id, false) },
                        onClick = { onHabitClick(row.item) },
                    )

                    is TodayRow.DoneTask -> TodoRow(
                        item = row.item,
                        onToggleDone = { onToggleDone(row.item.todo.id, false) },
                        onClick = { onItemClick(row.item) },
                    )

                    TodayRow.Empty -> EmptyState("Czysto.", "Dopisz coś na dole ekranu.")

                    // Numer wersji na dole - dzieki temu od razu widac, czy aktualizacja doszla.
                    TodayRow.Footer -> Text(
                        text = "Momentum ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 32.dp),
                    )
                }
            }
        }
    }
}

private fun buildRows(
    state: TodayUiState,
    openHabits: List<HabitUi>,
    doneHabits: List<HabitUi>,
    vacationActive: Boolean,
    doneExpanded: Boolean,
): List<TodayRow> = buildList {
    add(TodayRow.Progress)
    if (vacationActive) add(TodayRow.Vacation)

    section("Z terminem na dziś", state.withDate)
    section("Na dziś", state.markedToday)

    if (openHabits.isNotEmpty()) {
        add(TodayRow.Header("Nawyki", openHabits.size))
        openHabits.forEach { add(TodayRow.Habit(it)) }
    }

    section("Ogólne", state.general)

    val doneCount = state.done.size + doneHabits.size
    if (doneCount > 0) {
        add(TodayRow.DoneHeader(doneCount))
        if (doneExpanded) {
            doneHabits.forEach { add(TodayRow.DoneHabit(it)) }
            state.done.forEach { add(TodayRow.DoneTask(it)) }
        }
    }

    if (state.isEmpty && openHabits.isEmpty()) add(TodayRow.Empty)

    add(TodayRow.Footer)
}

private fun MutableList<TodayRow>.section(title: String, items: List<TodoUi>) {
    if (items.isEmpty()) return
    add(TodayRow.Header(title, items.size))
    items.forEach { add(TodayRow.Task(it, title)) }
}
