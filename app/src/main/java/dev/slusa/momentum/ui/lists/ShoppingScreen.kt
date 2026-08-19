package dev.slusa.momentum.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.ui.ShoppingUiState
import dev.slusa.momentum.ui.TodoUi
import dev.slusa.momentum.ui.components.AddBar
import dev.slusa.momentum.ui.components.CollapsibleHeader
import dev.slusa.momentum.ui.components.EmptyState
import dev.slusa.momentum.ui.components.ScreenHeader
import dev.slusa.momentum.ui.components.TodoRow

/**
 * Zero dat, zero starzenia, zero ceregieli. Wpisujesz rzecz, odhaczasz, ladauje
 * na dole wyszarzona. Jedyny dodatek to przycisk sprzatajacy.
 */
@Composable
fun ShoppingScreen(
    state: ShoppingUiState,
    onAdd: (String) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onClearDone: () -> Unit,
    onItemClick: (TodoUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    // Kupione domyslnie zwiniete, tak samo jak zrobione na liscie ToDo - to archiwum
    // na jedno klikniecie wstecz, a nie tresc, ktora trzeba miec przed oczami.
    var doneExpanded by remember { mutableStateOf(false) }

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
                title = "Zakupy",
                subtitle = if (state.open.isEmpty()) "Lista pusta" else "${state.open.size} do kupienia",
            )
        },
        bottomBar = {
            AddBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Co kupić?",
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
            items(state.open, key = { "otwarte-${it.todo.id}" }) { item ->
                TodoRow(
                    item = item,
                    onToggleDone = { onToggleDone(item.todo.id, true) },
                    onClick = { onItemClick(item) },
                )
            }

            if (state.open.isEmpty() && state.done.isEmpty()) {
                item {
                    EmptyState("Nic do kupienia.", "Dopisz na dole, co ma się znaleźć w koszyku.")
                }
            }

            if (state.done.isNotEmpty()) {
                item(key = "naglowek-kupione") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f)) {
                            CollapsibleHeader(
                                label = "Kupione (${state.done.size})",
                                expanded = doneExpanded,
                                onClick = { doneExpanded = !doneExpanded },
                            )
                        }
                        TextButton(onClick = onClearDone) {
                            Text(
                                text = "Wyczyść",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                if (doneExpanded) {
                    items(state.done, key = { "kupione-${it.todo.id}" }) { item ->
                        TodoRow(
                            item = item,
                            onToggleDone = { onToggleDone(item.todo.id, false) },
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }
    }
}
