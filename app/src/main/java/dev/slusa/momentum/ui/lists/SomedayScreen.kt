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
import dev.slusa.momentum.ui.components.EmptyState
import dev.slusa.momentum.ui.components.ScreenHeader
import dev.slusa.momentum.ui.components.TodoRow

/**
 * Rzeczy, o ktorych nie chcesz zapomniec, ale nie chcesz, zeby sie na ciebie
 * patrzyly. Chip po prawej przenosi pozycje na liste glowna jednym dotknieciem.
 */
@Composable
fun SomedayScreen(
    items: List<TodoUi>,
    onAdd: (String) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onMoveToMain: (Long) -> Unit,
    onItemClick: (TodoUi) -> Unit,
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
                title = "Kiedyś",
                subtitle = if (items.isEmpty()) "pusto" else "${items.size} odłożonych",
            )
        },
        bottomBar = {
            AddBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Coś, co zrobisz kiedyś…",
                onSubmit = submit,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.todo.id }) { item ->
                TodoRow(
                    item = item,
                    onToggleDone = { onToggleDone(item.todo.id, true) },
                    onClick = { onItemClick(item) },
                    trailing = { MoveToMainChip { onMoveToMain(item.todo.id) } },
                )
            }

            if (items.isEmpty()) {
                item {
                    EmptyState(
                        "Nic odłożonego.",
                        "Tu trafia to, o czym nie chcesz zapomnieć, ale nie dziś.",
                    )
                }
            }
        }
    }
}

@Composable
private fun MoveToMainChip(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, scheme.outline),
    ) {
        Text(
            text = "→ główna",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
