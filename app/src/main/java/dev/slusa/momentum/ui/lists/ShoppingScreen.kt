package dev.slusa.momentum.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
 * Jeden wiersz ekranu zakupow.
 *
 * Lista powstaje jako zwykla lista obiektow, a dopiero potem jest rysowana. Budowana
 * wprost w LazyColumn nie pozwalala powiedziec, pod ktorym indeksem wyladuje konkretna
 * rzecz - a bez tego nie ma jak przewinac ekranu do wlasnie dopisanej.
 */
private sealed interface ShoppingRow {
    val key: String

    data class Item(val item: TodoUi) : ShoppingRow {
        override val key: String = "rzecz-${item.todo.id}"
    }

    data object Empty : ShoppingRow {
        override val key: String = "pusto"
    }

    data class ListsHeader(val count: Int) : ShoppingRow {
        override val key: String = "naglowek-listy"
    }

    data class ListHead(val entry: ShoppingListUi, val expanded: Boolean) : ShoppingRow {
        override val key: String = "lista-${entry.list.id}"
    }

    data class InlineAddRow(val listId: Long, val name: String) : ShoppingRow {
        override val key: String = "dopisz-$listId"
    }

    data object NewList : ShoppingRow {
        override val key: String = "nowa-lista"
    }

    data class DoneHeader(val count: Int) : ShoppingRow {
        override val key: String = "naglowek-kupione"
    }

    data class DoneItem(val item: TodoUi) : ShoppingRow {
        override val key: String = "kupione-${item.todo.id}"
    }
}

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
    lastAddedId: Long?,
    onAddedShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    // Kupione domyslnie zwiniete - to archiwum na jedno klikniecie wstecz, a nie tresc,
    // ktora trzeba miec przed oczami.
    var doneExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedLists by rememberSaveable { mutableStateOf(setOf<Long>()) }

    var creatingList by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ShoppingList?>(null) }
    var deleting by remember { mutableStateOf<ShoppingListUi?>(null) }

    val listState = rememberLazyListState()

    val rows = remember(state, expandedLists, doneExpanded) {
        buildRows(state, expandedLists, doneExpanded)
    }

    /*
     * Przewijanie do dopisanej rzeczy, dopiero gdy juz jest na liscie.
     *
     * Skok wykonany od razu po dopisaniu nic nie daje: zapis do bazy jest asynchroniczny,
     * a lista z kluczami trzyma pozycje przy dotychczasowym pierwszym wierszu - rzecz
     * wstawiona nad nim laduje wtedy tuz ponad widokiem, czyli dokladnie tam, gdzie jej
     * nie widac.
     */
    LaunchedEffect(lastAddedId, rows) {
        val id = lastAddedId ?: return@LaunchedEffect
        val index = rows.indexOfFirst { it is ShoppingRow.Item && it.item.todo.id == id }
        if (index < 0) return@LaunchedEffect

        // Naglowek nad rzecza mowi, na ktora liste wpadla - warto go pokazac razem z nia.
        val above = rows.getOrNull(index - 1)
        val target = if (above is ShoppingRow.ListHead) index - 1 else index

        listState.animateScrollToItem(target)
        onAddedShown()
    }

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
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is ShoppingRow.Item -> TodoRow(
                        item = row.item,
                        onToggleDone = { onToggleDone(row.item.todo.id, true) },
                        onClick = { onItemClick(row.item) },
                    )

                    ShoppingRow.Empty -> EmptyState(
                        "Nic do kupienia.",
                        "Dopisz na dole, co ma się znaleźć w koszyku.",
                    )

                    is ShoppingRow.ListsHeader -> SectionHeader("Listy", row.count)

                    is ShoppingRow.ListHead -> ListHeader(
                        entry = row.entry,
                        expanded = row.expanded,
                        onToggle = {
                            expandedLists = if (row.expanded) {
                                expandedLists - row.entry.list.id
                            } else {
                                expandedLists + row.entry.list.id
                            }
                        },
                        onRename = { renaming = row.entry.list },
                        onDelete = { deleting = row.entry },
                    )

                    is ShoppingRow.InlineAddRow -> InlineAdd(
                        placeholder = "Dopisz do ${row.name}",
                        onSubmit = { onAdd(it, row.listId) },
                    )

                    ShoppingRow.NewList -> TextButton(
                        onClick = { creatingList = true },
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Nowa lista")
                    }

                    is ShoppingRow.DoneHeader -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f)) {
                            CollapsibleHeader(
                                label = "Kupione (${row.count})",
                                expanded = doneExpanded,
                                onClick = { doneExpanded = !doneExpanded },
                            )
                        }
                        TextButton(onClick = onClearDone) {
                            Text("Wyczyść", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    is ShoppingRow.DoneItem -> TodoRow(
                        item = row.item,
                        onToggleDone = { onToggleDone(row.item.todo.id, false) },
                        onClick = { onItemClick(row.item) },
                        // Skad rzecz pochodzi ma znaczenie wlasnie tutaj: po cofnieciu
                        // wroci na swoja podliste, a nie na te, ktora masz przed oczami.
                        trailing = row.item.listName?.let { name ->
                            { Text(name, style = MaterialTheme.typography.labelSmall) }
                        },
                    )
                }
            }
        }
    }
}

private fun buildRows(
    state: ShoppingUiState,
    expandedLists: Set<Long>,
    doneExpanded: Boolean,
): List<ShoppingRow> = buildList {
    state.open.forEach { add(ShoppingRow.Item(it)) }

    if (state.open.isEmpty() && state.lists.isEmpty() && state.done.isEmpty()) {
        add(ShoppingRow.Empty)
    }

    add(ShoppingRow.ListsHeader(state.lists.size))

    state.lists.forEach { entry ->
        val expanded = entry.list.id in expandedLists
        add(ShoppingRow.ListHead(entry, expanded))

        if (expanded) {
            entry.open.forEach { add(ShoppingRow.Item(it)) }
            add(ShoppingRow.InlineAddRow(entry.list.id, entry.list.name))
        }
    }

    add(ShoppingRow.NewList)

    if (state.done.isNotEmpty()) {
        add(ShoppingRow.DoneHeader(state.done.size))
        if (doneExpanded) state.done.forEach { add(ShoppingRow.DoneItem(it)) }
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
