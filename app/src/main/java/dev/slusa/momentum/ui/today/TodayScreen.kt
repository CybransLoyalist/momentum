package dev.slusa.momentum.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.BuildConfig
import dev.slusa.momentum.ui.HabitUi
import dev.slusa.momentum.ui.TodayUiState
import dev.slusa.momentum.ui.TodoUi
import dev.slusa.momentum.ui.components.AddBar
import dev.slusa.momentum.ui.components.CollapsibleHeader
import dev.slusa.momentum.ui.components.EmptyState
import dev.slusa.momentum.ui.components.HabitRow
import dev.slusa.momentum.ui.components.ScreenHeader
import dev.slusa.momentum.ui.components.SectionHeader
import dev.slusa.momentum.ui.components.TodayChip
import dev.slusa.momentum.ui.components.TodoRow
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl"))

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
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var draftForToday by remember { mutableStateOf(false) }
    var doneExpanded by remember { mutableStateOf(false) }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            section("Z terminem na dziś", state.withDate, onToggleDone, onToggleToday, onItemClick)
            section("Na dziś", state.markedToday, onToggleDone, onToggleToday, onItemClick)

            if (habits.isNotEmpty()) {
                item(key = "naglowek-nawyki") { SectionHeader("Nawyki", habits.size) }
                items(habits, key = { "nawyk-${it.habit.id}" }) { item ->
                    HabitRow(
                        item = item,
                        onToggleDone = { onToggleHabit(item.habit.id, !item.doneToday) },
                        onClick = { onHabitClick(item) },
                    )
                }
            }

            section("Ogólne", state.general, onToggleDone, onToggleToday, onItemClick)

            if (state.done.isNotEmpty()) {
                item(key = "naglowek-zrobione") {
                    CollapsibleHeader(
                        label = "Zrobione (${state.done.size})",
                        expanded = doneExpanded,
                        onClick = { doneExpanded = !doneExpanded },
                    )
                }
                if (doneExpanded) {
                    items(state.done, key = { "zrobione-${it.todo.id}" }) { item ->
                        TodoRow(
                            item = item,
                            onToggleDone = { onToggleDone(item.todo.id, false) },
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }

            if (state.isEmpty && habits.isEmpty()) {
                item(key = "pusto") {
                    EmptyState("Czysto.", "Dopisz coś na dole ekranu.")
                }
            }

            // Numer wersji na dole - dzieki temu od razu widac, czy aktualizacja doszla.
            item(key = "stopka") {
                Text(
                    text = "Momentum ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 32.dp),
                )
            }
        }
    }
}

private fun LazyListScope.section(
    title: String,
    items: List<TodoUi>,
    onToggleDone: (Long, Boolean) -> Unit,
    onToggleToday: (Long) -> Unit,
    onItemClick: (TodoUi) -> Unit,
) {
    if (items.isEmpty()) return

    item(key = "naglowek-$title") { SectionHeader(title, items.size) }

    items(items, key = { "$title-${it.todo.id}" }) { item ->
        TodoRow(
            item = item,
            onToggleDone = { onToggleDone(item.todo.id, true) },
            onClick = { onItemClick(item) },
            trailing = {
                TodayChip(active = item.onTodayList, onClick = { onToggleToday(item.todo.id) })
            },
        )
    }
}
