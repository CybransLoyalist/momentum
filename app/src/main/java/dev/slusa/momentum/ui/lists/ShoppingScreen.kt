package dev.slusa.momentum.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.data.ShoppingList
import dev.slusa.momentum.ui.ShoppingListUi
import dev.slusa.momentum.ui.ShoppingUiState
import dev.slusa.momentum.ui.TodoUi
import dev.slusa.momentum.ui.components.AddBar
import dev.slusa.momentum.ui.components.CollapsibleHeader
import dev.slusa.momentum.ui.components.EmptyState
import dev.slusa.momentum.ui.components.ScreenHeader
import dev.slusa.momentum.ui.components.SectionHeader
import dev.slusa.momentum.ui.components.TodoRow

/**
 * Zero dat, zero starzenia, zero ceregieli.
 *
 * Lista glowna na codzienne rzeczy jest na wierzchu i nie da sie jej skasowac. Pod nia
 * nazwane podlisty - Ikea, Rossmann, przez internet - domyslnie zwiniete, bo to rzeczy
 * na kiedys tam bede, a nie na dzisiejsze zakupy.
 *
 * "Kupione" jest jedno, na samym dole, i zbiera odhaczone ze wszystkich list. Sluzy
 * wylacznie do cofniecia pomylki, wiec dzielenie go po listach dokladaloby podzialow
 * tam, gdzie szuka sie jednej konkretnej rzeczy.
 */
@Composable
fun ShoppingScreen(
    state: ShoppingUiState,
    onAdd: (String, Long?) -> Unit,
    onToggleDone: (Long, Boolean) -> Unit,
    onClearDone: () -> Unit,
    onItemClick: (TodoUi) -> Unit,
    onAddList: (String) -> Unit,
    onRenameList: (ShoppingList, String) -> Unit,
    onDeleteList: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }

    // Nowa rzecz laduje na gorze listy - patrz TodayScreen.
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Kupione domyslnie zwiniete - to archiwum na jedno klikniecie wstecz, a nie tresc,
    // ktora trzeba miec przed oczami.
    var doneExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedLists by rememberSaveable { mutableStateOf(setOf<Long>()) }

    var creatingList by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ShoppingList?>(null) }
    var deleting by remember { mutableStateOf<ShoppingListUi?>(null) }

    if (creatingList) {
        NameDialog(
            title = "Nowa lista",
            initial = "",
            confirm = "Utwórz",
            onDismiss = { creatingList = false },
            onConfirm = {
                onAddList(it)
                creatingList = false
            },
        )
    }

    renaming?.let { list ->
        NameDialog(
            title = "Zmień nazwę",
            initial = list.name,
            confirm = "Zapisz",
            onDismiss = { renaming = null },
            onConfirm = {
                onRenameList(list, it)
                renaming = null
            },
        )
    }

    deleting?.let { entry ->
        DeleteListDialog(
            entry = entry,
            onDismiss = { deleting = null },
            onConfirm = {
                onDeleteList(entry.list.id)
                deleting = null
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "Zakupy",
                // Naglowek liczy tylko liste glowna. Czterdziesci rzeczy z Ikei
                // doliczone tutaj zamienilyby te liczbe w szum.
                subtitle = if (state.open.isEmpty()) {
                    "Lista pusta"
                } else {
                    "${state.open.size} do kupienia"
                },
            )
        },
        bottomBar = {
            AddBar(
                value = draft,
                onValueChange = { draft = it },
                placeholder = "Co kupić?",
                onSubmit = {
                    if (draft.isNotBlank()) {
                        onAdd(draft, null)
                        draft = ""
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.open, key = { "glowne-${it.todo.id}" }) { item ->
                TodoRow(
                    item = item,
                    onToggleDone = { onToggleDone(item.todo.id, true) },
                    onClick = { onItemClick(item) },
                )
            }

            if (state.open.isEmpty() && state.lists.isEmpty() && state.done.isEmpty()) {
                item {
                    EmptyState("Nic do kupienia.", "Dopisz na dole, co ma się znaleźć w koszyku.")
                }
            }

            item(key = "naglowek-listy") { SectionHeader("Listy", state.lists.size) }

            state.lists.forEach { entry ->
                val expanded = entry.list.id in expandedLists

                item(key = "lista-${entry.list.id}") {
                    ListHeader(
                        entry = entry,
                        expanded = expanded,
                        onToggle = {
                            expandedLists = if (expanded) {
                                expandedLists - entry.list.id
                            } else {
                                expandedLists + entry.list.id
                            }
                        },
                        onRename = { renaming = entry.list },
                        onDelete = { deleting = entry },
                    )
                }

                if (expanded) {
                    items(entry.open, key = { "poz-${it.todo.id}" }) { item ->
                        TodoRow(
                            item = item,
                            onToggleDone = { onToggleDone(item.todo.id, true) },
                            onClick = { onItemClick(item) },
                        )
                    }

                    item(key = "dopisz-${entry.list.id}") {
                        InlineAdd(
                            placeholder = "Dopisz do ${entry.list.name}",
                            onSubmit = { onAdd(it, entry.list.id) },
                        )
                    }
                }
            }

            item(key = "nowa-lista") {
                TextButton(
                    onClick = { creatingList = true },
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Nowa lista")
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
                            // Skad rzecz pochodzi ma znaczenie wlasnie tutaj: po cofnieciu
                            // wroci na swoja podliste, a nie na te, ktora masz przed oczami.
                            trailing = item.listName?.let { name ->
                                { Text(name, style = MaterialTheme.typography.labelSmall) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(
    entry: ShoppingListUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f)) {
            CollapsibleHeader(
                label = "${entry.list.name} (${entry.open.size})",
                expanded = expanded,
                onClick = onToggle,
            )
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Text("⋯", style = MaterialTheme.typography.titleMedium)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Zmień nazwę") },
                    onClick = {
                        menuOpen = false
                        onRename()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Usuń listę") },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

/**
 * Dopisywanie wewnatrz rozwinietej podlisty.
 *
 * Pasek na dole ekranu zawsze celuje w liste glowna i nigdy tego nie zmienia. Chip
 * przelaczajacy cel bylby jednym polem mniej, ale pamietalby wybor miedzy wejsciami
 * i predzej czy pozniej mleko wyladowaloby w Ikei.
 */
@Composable
private fun InlineAdd(placeholder: String, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    val submit = {
        if (text.isNotBlank()) {
            onSubmit(text)
            text = ""
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(placeholder) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            IconButton(onClick = submit, enabled = text.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj",
                    tint = if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        },
    )
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nazwa listy") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(confirm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
private fun DeleteListDialog(
    entry: ShoppingListUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usunąć listę ${entry.list.name}?") },
        text = {
            Text(
                if (entry.open.isEmpty()) {
                    "Lista jest pusta, więc nic poza nią nie zniknie."
                } else {
                    "Ma ${entry.open.size} rzeczy do kupienia. Przeniosę je na listę główną " +
                        "— zniknie tylko sama lista."
                }
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Usuń listę") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}
