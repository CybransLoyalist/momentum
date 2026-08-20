package dev.slusa.momentum.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.ui.TodoUi
import dev.slusa.momentum.ui.components.AddBar
import dev.slusa.momentum.ui.components.DateChip
import dev.slusa.momentum.ui.components.EmptyState
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
 * Zadania z terminem w przyszlosci oraz wszystkie cykliczne. Grupowanie jest wzgledne,
 * nie po numerach tygodni - "w przyszlym tygodniu" niesie informacje, "tydzien 34" nie.
 */
@Composable
fun ScheduledScreen(
    items: List<TodoUi>,
    today: LocalDate,
    onOpenEditor: (String) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onItemClick: (TodoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }

    val groups = remember(items, today) { groupByHorizon(items, today) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "Zaplanowane",
                subtitle = if (items.isEmpty()) "Nic w kolejce" else "${items.size} w kolejce",
            )
        },
        bottomBar = {
            // Tu plusik nie dodaje, tylko otwiera edytor - bez terminu nie ma czego
            // dodac, a strzalka zamiast plusa zapowiada, ze cos sie otworzy.
            AddBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Co jest do zaplanowania?",
                onSubmit = {
                    onOpenEditor(draft)
                    draft = ""
                },
                submitIcon = Icons.AutoMirrored.Filled.ArrowForward,
                submitLabel = "Skonfiguruj zadanie",
                submitEnabled = true,
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
                        "Wpisz zadanie na dole i przejdź dalej, żeby ustawić termin " +
                            "albo powtarzanie.",
                    )
                }
            }
        }
    }
}

/**
 * Etykiety wzgledne wzgledem dzisiaj - blizsze terminy dostaja wiecej rozdzielczosci.
 * Zaleglosci i dzisiejsze maja wlasne sekcje, bo od kiedy trafiaja tu takze zadania
 * cykliczne, "Jutro" musi znaczyc jutro, a nie wszystko do jutra wlacznie.
 */
private fun groupByHorizon(
    items: List<TodoUi>,
    today: LocalDate,
): List<Pair<String, List<TodoUi>>> {
    val buckets = LinkedHashMap<String, MutableList<TodoUi>>()

    items.forEach { item ->
        val date = item.todo.plannedDate ?: return@forEach
        val days = ChronoUnit.DAYS.between(today, date)
        val label = when {
            days < 0L -> "Zaległe"
            days == 0L -> "Dziś"
            days == 1L -> "Jutro"
            days <= 7L -> "W ciągu tygodnia"
            days <= 30L -> "W ciągu miesiąca"
            else -> "Później"
        }
        buckets.getOrPut(label) { mutableListOf() }.add(item)
    }

    return buckets.map { it.key to it.value }
}
