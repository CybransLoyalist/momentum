package dev.slusa.momentum.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slusa.momentum.BuildConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("pl"))

@Composable
fun TodayScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    var draftForToday by remember { mutableStateOf(true) }
    var doneExpanded by remember { mutableStateOf(false) }

    // Zmiana doby nie budzi sama przeplywow, wiec lapiemy ja przy powrocie do apki.
    LifecycleResumeEffect(Unit) {
        vm.refreshDay()
        onPauseOrDispose { }
    }

    val submit = {
        if (draft.isNotBlank()) {
            vm.add(draft, draftForToday)
            draft = ""
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { Header(state.day, state.todayCount) },
        bottomBar = {
            InputBar(
                value = draft,
                onValueChange = { draft = it },
                forToday = draftForToday,
                onToggleForToday = { draftForToday = !draftForToday },
                onSubmit = submit,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            todoSection("Z terminem na dziś", state.withDate, vm)
            todoSection("Na dziś", state.markedToday, vm)
            todoSection("Ogólne", state.general, vm)

            if (state.done.isNotEmpty()) {
                item(key = "naglowek-zrobione") {
                    DoneHeader(
                        count = state.done.size,
                        expanded = doneExpanded,
                        onClick = { doneExpanded = !doneExpanded },
                    )
                }
                if (doneExpanded) {
                    items(state.done, key = { "zrobione-${it.todo.id}" }) { item ->
                        TodoRow(
                            item = item,
                            onToggleDone = { vm.setDone(item.todo.id, false) },
                            onToggleToday = { vm.toggleToday(item.todo.id) },
                        )
                    }
                }
            }

            if (state.todayCount == 0 && state.general.isEmpty()) {
                item(key = "pusto") { EmptyState() }
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

private fun LazyListScope.todoSection(
    title: String,
    items: List<TodoUi>,
    vm: TodayViewModel,
) {
    if (items.isEmpty()) return

    item(key = "naglowek-$title") { SectionHeader(title, items.size) }

    items(items, key = { "${title}-${it.todo.id}" }) { item ->
        TodoRow(
            item = item,
            onToggleDone = { vm.setDone(item.todo.id, true) },
            onToggleToday = { vm.toggleToday(item.todo.id) },
        )
    }
}

@Composable
private fun Header(day: LocalDate, todayCount: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)
    ) {
        Text(
            text = "Dzisiaj",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = buildString {
                append(day.format(DATE_FORMAT))
                append(" · ")
                // "rzecz" ma te sama forme dla 2 i dla 5, wiec wystarczy rozdzielic jedynke.
                append(
                    when (todayCount) {
                        0 -> "nic na dziś"
                        1 -> "1 rzecz"
                        else -> "$todayCount rzeczy"
                    }
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(Locale.forLanguageTag("pl")),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun DoneHeader(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Zwiń" else "Rozwiń",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Zrobione ($count)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.padding(top = 48.dp)) {
        Text(
            text = "Czysto.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Dopisz coś na dole ekranu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    forToday: Boolean,
    onToggleForToday: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Co masz do zrobienia?") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            )

            Spacer(Modifier.width(8.dp))

            DraftTodayToggle(active = forToday, onClick = onToggleForToday)

            Spacer(Modifier.width(4.dp))

            IconButton(onClick = onSubmit, enabled = value.isNotBlank()) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj",
                    tint = if (value.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                )
            }
        }
    }
}

/** Decyduje, czy nowa rzecz ma od razu wpasc na dzisiejsza liste, czy do ogolnych. */
@Composable
private fun DraftTodayToggle(active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) scheme.primaryContainer else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (active) Color.Transparent else scheme.outline,
        ),
    ) {
        Text(
            text = "dziś",
            style = MaterialTheme.typography.labelSmall,
            color = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
