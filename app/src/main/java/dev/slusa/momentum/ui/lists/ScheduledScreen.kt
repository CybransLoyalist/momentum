package dev.slusa.momentum.ui.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.ui.TodoUi
import dev.slusa.momentum.ui.components.AddBar
import dev.slusa.momentum.ui.components.DateChip
import dev.slusa.momentum.ui.components.EmptyState
import dev.slusa.momentum.ui.components.PickDateDialog
import dev.slusa.momentum.ui.components.ScreenHeader
import dev.slusa.momentum.ui.components.SectionHeader
import dev.slusa.momentum.ui.components.TodoRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val SHORT_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale.forLanguageTag("pl"))

/**
 * Zadania z terminem w przyszlosci. Grupowanie jest wzgledne, nie po numerach
 * tygodni - "w przyszlym tygodniu" niesie informacje, "tydzien 34" nie.
 */
@Composable
fun ScheduledScreen(
    items: List<TodoUi>,
    today: LocalDate,
    onAddScheduled: (String, LocalDate) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onItemClick: (TodoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var pickingDate by remember { mutableStateOf(false) }

    if (pickingDate) {
        PickDateDialog(
            initial = today.plusDays(1),
            onDismiss = { pickingDate = false },
            onPicked = { date ->
                onAddScheduled(draft, date)
                draft = ""
                pickingDate = false
            },
        )
    }

    val groups = remember(items, today) { groupByHorizon(items, today) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "Zaplanowane",
                subtitle = if (items.isEmpty()) "nic w kolejce" else "${items.size} w kolejce",
            )
        },
        bottomBar = {
            AddBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Co i na kiedy?",
                onSubmit = { if (draft.isNotBlank()) pickingDate = true },
                trailing = {
                    PickDateChip(enabled = draft.isNotBlank()) { pickingDate = true }
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
            groups.forEach { (label, entries) ->
                item(key = "naglowek-$label") { SectionHeader(label, entries.size) }

                items(entries, key = { it.todo.id }) { item ->
                    TodoRow(
                        item = item,
                        onToggleDone = { onToggleDone(item.todo.id, true) },
                        onClick = { onItemClick(item) },
                        trailing = { DateChip(item.todo.plannedDate?.format(SHORT_DATE) ?: "") },
                    )
                }
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        "Kalendarz pusty.",
                        "Wpisz zadanie na dole i wybierz dzień, w którym ma wrócić.",
                    )
                }
            }
        }
    }
}

/** Etykiety wzgledne wzgledem dzisiaj - blizsze terminy dostaja wiecej rozdzielczosci. */
private fun groupByHorizon(
    items: List<TodoUi>,
    today: LocalDate,
): List<Pair<String, List<TodoUi>>> {
    val buckets = LinkedHashMap<String, MutableList<TodoUi>>()

    items.forEach { item ->
        val date = item.todo.plannedDate ?: return@forEach
        val days = ChronoUnit.DAYS.between(today, date)
        val label = when {
            days <= 1L -> "Jutro"
            days <= 7L -> "W ciągu tygodnia"
            days <= 30L -> "W ciągu miesiąca"
            else -> "Później"
        }
        buckets.getOrPut(label) { mutableListOf() }.add(item)
    }

    return buckets.map { it.key to it.value }
}

@Composable
private fun PickDateChip(enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, scheme.outline),
    ) {
        Text(
            text = "kiedy?",
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) scheme.onSurfaceVariant else scheme.outline,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
