package dev.slusa.momentum.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slusa.momentum.backup.BackupData
import dev.slusa.momentum.ui.components.PickDateDialog
import dev.slusa.momentum.ui.components.TodoActionsSheet
import dev.slusa.momentum.ui.icons.MomentumIcons
import dev.slusa.momentum.ui.editor.EditorTarget
import dev.slusa.momentum.ui.editor.TaskEditorScreen
import dev.slusa.momentum.ui.habits.HabitEditorSheet
import dev.slusa.momentum.ui.habits.HabitsScreen
import dev.slusa.momentum.ui.lists.ScheduledScreen
import dev.slusa.momentum.ui.lists.ShoppingScreen
import dev.slusa.momentum.ui.lists.SomedayScreen
import dev.slusa.momentum.ui.settings.SettingsScreen
import dev.slusa.momentum.ui.today.TodayScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    TODO_("ToDo", Icons.AutoMirrored.Filled.List),
    KIEDYS("Kiedyś", MomentumIcons.Hourglass),
    ZAPLANOWANE("Plan", Icons.Default.DateRange),
    ZAKUPY("Zakupy", Icons.Default.ShoppingCart),
    NAWYKI("Nawyki", MomentumIcons.Flame),
}

/**
 * Powloka z zakladkami. Arkusz akcji i wybor daty zyja tutaj, a nie w kazdym
 * ekranie osobno - dzialaja tak samo niezaleznie od tego, na ktorej liscie
 * jestes, i jest jedna kopia tej logiki zamiast czterech.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MomentumShell(vm: MomentumViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.TODO_) }
    var sheetFor by remember { mutableStateOf<TodoUi?>(null) }
    var dateFor by remember { mutableStateOf<TodoUi?>(null) }
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var habitFor by remember { mutableStateOf<HabitUi?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // Zmiana doby nie budzi sama przeplywow, wiec lapiemy ja przy powrocie do apki.
    LifecycleResumeEffect(Unit) {
        vm.refreshDay()
        onPauseOrDispose { }
    }

    // Systemowy powrot zamyka po kolei: edytor, ustawienia, potem wraca na liste glowna.
    BackHandler(enabled = editorTarget != null) { editorTarget = null }
    BackHandler(enabled = editorTarget == null && showSettings) { showSettings = false }
    BackHandler(enabled = editorTarget == null && !showSettings && tab != Tab.TODO_) {
        tab = Tab.TODO_
    }

    val todayState by vm.todayState.collectAsStateWithLifecycle()
    val somedayItems by vm.somedayState.collectAsStateWithLifecycle()
    val scheduledItems by vm.scheduledState.collectAsStateWithLifecycle()
    val shoppingState by vm.shoppingState.collectAsStateWithLifecycle()
    val todayHabits by vm.todayHabits.collectAsStateWithLifecycle()
    val allHabits by vm.habitsState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val backupMessage by vm.backupMessage.collectAsStateWithLifecycle()
    val lastAddedId by vm.lastAddedId.collectAsStateWithLifecycle()
    val foundSnapshot by vm.foundSnapshot.collectAsStateWithLifecycle()
    val shareRequest by vm.shareRequest.collectAsStateWithLifecycle()

    // Wysylka pliku na zewnatrz idzie przez systemowe okno wyboru - dzieki temu kopia
    // laduje tam, gdzie chcesz, bez zadnego logowania po naszej stronie.
    val context = LocalContext.current
    LaunchedEffect(shareRequest) {
        val uri = shareRequest ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/json")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Kopia Momentum")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { context.startActivity(Intent.createChooser(send, "Wyślij kopię")) }
        vm.shareHandled()
    }

    // Pusta baza plus znaleziona kopia to praktycznie zawsze swiezo przeniesiony telefon.
    foundSnapshot?.let { data ->
        RestoreFoundDialog(
            data = data,
            onRestore = vm::restoreFoundSnapshot,
            onDismiss = vm::dismissFoundSnapshot,
        )
    }

    editorTarget?.let { target ->
        TaskEditorScreen(
            target = target,
            today = todayState.day,
            onCancel = { editorTarget = null },
            onSave = { title, date, rule ->
                val existing = target.item
                if (existing == null) {
                    vm.addTask(title, date, rule)
                } else {
                    vm.editTask(existing.todo.id, title, date, rule)
                }
                editorTarget = null
            },
        )
        return
    }

    if (showSettings) {
        SettingsScreen(
            vacation = settings.vacation,
            morning = settings.morning,
            afternoon = settings.afternoon,
            today = todayState.day,
            onBack = { showSettings = false },
            onStartVacation = vm::startVacation,
            onEndVacation = vm::endVacation,
            onMorningChange = vm::setMorningReminder,
            onAfternoonChange = vm::setAfternoonReminder,
            backupFolder = settings.backupFolder,
            lastBackup = settings.lastBackup,
            backupMessage = backupMessage,
            onPickBackupFolder = vm::setBackupFolder,
            onBackupNow = vm::backupNow,
            onShareBackup = vm::shareBackup,
            onRestore = vm::restoreFrom,
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Przy otwartej klawiaturze zakladki znikaja - pole dopisywania dostaje
            // wtedy caly dol ekranu i nie walczy o miejsce z paskiem nawigacji.
            if (!WindowInsets.isImeVisible) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(entry.icon, contentDescription = entry.label) },
                            label = { Text(entry.label) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            when (tab) {
                Tab.TODO_ -> TodayScreen(
                    state = todayState,
                    habits = todayHabits,
                    onAdd = { title, forToday -> vm.add(title, forToday = forToday) },
                    onToggleDone = vm::setDone,
                    onToggleToday = vm::toggleToday,
                    onItemClick = { sheetFor = it },
                    onToggleHabit = vm::setHabitDone,
                    onHabitClick = { habitFor = it },
                    vacationActive = settings.vacation != null,
                    onOpenSettings = { showSettings = true },
                    lastAddedId = lastAddedId,
                    onAddedShown = vm::consumeLastAdded,
                )

                Tab.KIEDYS -> SomedayScreen(
                    items = somedayItems,
                    onAdd = { vm.add(it, bucket = dev.slusa.momentum.data.Bucket.KIEDYS) },
                    onToggleDone = vm::setDone,
                    onMoveToMain = { vm.moveTo(it, dev.slusa.momentum.data.Bucket.GLOWNE) },
                    onItemClick = { sheetFor = it },
                )

                Tab.ZAPLANOWANE -> ScheduledScreen(
                    items = scheduledItems,
                    today = todayState.day,
                    onOpenEditor = { editorTarget = EditorTarget(draftTitle = it) },
                    onToggleDone = vm::setDone,
                    onItemClick = { sheetFor = it },
                )

                Tab.ZAKUPY -> ShoppingScreen(
                    state = shoppingState,
                    onAdd = vm::addShoppingItem,
                    onToggleDone = vm::setDone,
                    onClearDone = vm::clearShoppingDone,
                    onItemClick = { sheetFor = it },
                    onAddList = vm::addShoppingList,
                    onRenameList = vm::renameShoppingList,
                    onDeleteList = vm::deleteShoppingList,
                )

                Tab.NAWYKI -> HabitsScreen(
                    items = allHabits,
                    onAdd = { vm.addHabit(it, daily = true, days = emptySet()) },
                    onItemClick = { habitFor = it },
                )
            }
        }
    }

    sheetFor?.let { item ->
        TodoActionsSheet(
            item = item,
            onDismiss = { sheetFor = null },
            onToggleToday = {
                vm.toggleToday(item.todo.id)
                sheetFor = null
            },
            onPickDate = {
                sheetFor = null
                dateFor = item
            },
            onEdit = {
                sheetFor = null
                editorTarget = EditorTarget(item = item)
            },
            onMoveTo = { bucket ->
                vm.moveTo(item.todo.id, bucket)
                sheetFor = null
            },
            onDelete = {
                vm.delete(item.todo.id)
                sheetFor = null
            },
        )
    }

    habitFor?.let { item ->
        HabitEditorSheet(
            habit = item.habit,
            onDismiss = { habitFor = null },
            onSave = {
                vm.saveHabit(it)
                habitFor = null
            },
            onArchive = {
                vm.archiveHabit(item.habit.id, !item.habit.archived)
                habitFor = null
            },
            onDelete = {
                vm.deleteHabit(item.habit.id)
                habitFor = null
            },
        )
    }

    dateFor?.let { item ->
        PickDateDialog(
            initial = item.todo.plannedDate ?: todayState.day.plusDays(1),
            today = todayState.day,
            confirmLabel = "Zaplanuj",
            onDismiss = { dateFor = null },
            onPicked = { date ->
                vm.schedule(item.todo.id, date)
                dateFor = null
            },
        )
    }
}

/**
 * Propozycja odtworzenia z kopii znalezionej przy pustej bazie. Pytamy, zamiast robic
 * to po cichu: gdyby ktos swiadomie wyczyscil aplikacje, ciche przywrocenie wszystkiego
 * byloby dokladnie odwrotnoscia tego, czego chcial.
 */
@Composable
private fun RestoreFoundDialog(
    data: BackupData,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val stamp = data.createdAt
        ?.atZone(java.time.ZoneId.systemDefault())
        ?.toLocalDate()
        ?.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("pl")))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Znaleziono kopię") },
        text = {
            Text(
                buildString {
                    append("Aplikacja jest pusta, ale została kopia")
                    if (stamp != null) append(" z $stamp")
                    append(": ${data.todos.size} zadań i ${data.habits.size} nawyków ")
                    append("razem z historią. Przywrócić?")
                }
            )
        },
        confirmButton = { TextButton(onClick = onRestore) { Text("Przywróć") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zacznij od zera") } },
    )
}
