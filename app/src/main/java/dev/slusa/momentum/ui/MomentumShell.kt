package dev.slusa.momentum.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slusa.momentum.ui.components.PickDateDialog
import dev.slusa.momentum.ui.components.TodoActionsSheet
import dev.slusa.momentum.ui.lists.ScheduledScreen
import dev.slusa.momentum.ui.lists.ShoppingScreen
import dev.slusa.momentum.ui.lists.SomedayScreen
import dev.slusa.momentum.ui.today.TodayScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    DZISIAJ("Dzisiaj", Icons.Default.Home),
    KIEDYS("Kiedyś", Icons.Default.Star),
    ZAPLANOWANE("Plan", Icons.Default.DateRange),
    ZAKUPY("Zakupy", Icons.Default.ShoppingCart),
}

/**
 * Powloka z zakladkami. Arkusz akcji i wybor daty zyja tutaj, a nie w kazdym
 * ekranie osobno - dzialaja tak samo niezaleznie od tego, na ktorej liscie
 * jestes, i jest jedna kopia tej logiki zamiast czterech.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MomentumShell(vm: MomentumViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.DZISIAJ) }
    var sheetFor by remember { mutableStateOf<TodoUi?>(null) }
    var dateFor by remember { mutableStateOf<TodoUi?>(null) }

    // Zmiana doby nie budzi sama przeplywow, wiec lapiemy ja przy powrocie do apki.
    LifecycleResumeEffect(Unit) {
        vm.refreshDay()
        onPauseOrDispose { }
    }

    // Systemowy powrot wraca na liste glowna, zamiast zamykac aplikacje.
    BackHandler(enabled = tab != Tab.DZISIAJ) { tab = Tab.DZISIAJ }

    val todayState by vm.todayState.collectAsStateWithLifecycle()
    val somedayItems by vm.somedayState.collectAsStateWithLifecycle()
    val scheduledItems by vm.scheduledState.collectAsStateWithLifecycle()
    val shoppingState by vm.shoppingState.collectAsStateWithLifecycle()

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
                Tab.DZISIAJ -> TodayScreen(
                    state = todayState,
                    onAdd = { title, forToday -> vm.add(title, forToday = forToday) },
                    onToggleDone = vm::setDone,
                    onToggleToday = vm::toggleToday,
                    onItemClick = { sheetFor = it },
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
                    onAddScheduled = vm::addScheduled,
                    onToggleDone = vm::setDone,
                    onItemClick = { sheetFor = it },
                )

                Tab.ZAKUPY -> ShoppingScreen(
                    state = shoppingState,
                    onAdd = { vm.add(it, bucket = dev.slusa.momentum.data.Bucket.ZAKUPY) },
                    onToggleDone = vm::setDone,
                    onClearDone = vm::clearShoppingDone,
                    onItemClick = { sheetFor = it },
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

    dateFor?.let { item ->
        PickDateDialog(
            initial = item.todo.plannedDate ?: todayState.day.plusDays(1),
            onDismiss = { dateFor = null },
            onPicked = { date ->
                vm.schedule(item.todo.id, date)
                dateFor = null
            },
        )
    }
}
