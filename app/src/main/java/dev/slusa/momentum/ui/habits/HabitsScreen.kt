package dev.slusa.momentum.ui.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.ui.HabitUi
import dev.slusa.momentum.ui.components.AddBar
import dev.slusa.momentum.ui.components.EmptyState
import dev.slusa.momentum.ui.components.GridLegend
import dev.slusa.momentum.ui.components.MomentumBadge
import dev.slusa.momentum.ui.components.MomentumGrid
import dev.slusa.momentum.ui.components.ScreenHeader
import java.time.DayOfWeek

@Composable
fun HabitsScreen(
    items: List<HabitUi>,
    onAdd: (String) -> Unit,
    onItemClick: (HabitUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }

    val submit = {
        if (draft.isNotBlank()) {
            onAdd(draft)
            draft = ""
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "Nawyki",
                subtitle = if (items.isEmpty()) {
                    "nic jeszcze nie ma"
                } else {
                    "${items.size} w obiegu · dotknij, żeby zmienić dni"
                },
            )
        },
        bottomBar = {
            AddBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Nowy nawyk (codzienny)",
                onSubmit = submit,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.habit.id }) { item ->
                HabitCard(item) { onItemClick(item) }
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        "Brak nawyków.",
                        "Dopisz nazwę na dole — nowy nawyk startuje jako codzienny, " +
                            "dni wybierzesz po dotknięciu kafelka.",
                    )
                }
            } else {
                item {
                    GridLegend(Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun HabitCard(item: HabitUi, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outline),
    ) {
        Column(
            Modifier
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = scheduleSummary(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                MomentumBadge(item.momentum)
            }

            Spacer(Modifier.height(14.dp))

            MomentumGrid(item.grid)
        }
    }
}

private val SHORT_DAYS = listOf("pon", "wt", "śr", "czw", "pt", "sob", "nd")

private fun scheduleSummary(item: HabitUi): String {
    val habit = item.habit
    val base = if (habit.daily) {
        "codziennie"
    } else {
        DayOfWeek.entries
            .filter { it in habit.weekdays }
            .joinToString(", ") { SHORT_DAYS[it.value - 1] }
            .ifEmpty { "brak dni" }
    }

    return if (habit.archived) "$base · zarchiwizowany" else base
}
