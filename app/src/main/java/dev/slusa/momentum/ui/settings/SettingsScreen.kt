package dev.slusa.momentum.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.slusa.momentum.BuildConfig
import dev.slusa.momentum.data.Reminder
import dev.slusa.momentum.data.Vacation
import dev.slusa.momentum.notifications.PowerSaving
import dev.slusa.momentum.ui.components.PickDateDialog
import dev.slusa.momentum.ui.components.ScreenHeader
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("pl"))

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Ktore z dwoch przypomnien wlasnie ustawiamy - tylko na potrzeby okna z godzina. */
private enum class Slot { RANO, POPOLUDNIE }

@Composable
fun SettingsScreen(
    vacation: Vacation?,
    morning: Reminder,
    afternoon: Reminder,
    today: LocalDate,
    onBack: () -> Unit,
    onStartVacation: (LocalDate?) -> Unit,
    onEndVacation: () -> Unit,
    onMorningChange: (Reminder) -> Unit,
    onAfternoonChange: (Reminder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickingReturn by remember { mutableStateOf(false) }
    var pickingTimeFor by remember { mutableStateOf<Slot?>(null) }

    val context = LocalContext.current

    // Zgoda na powiadomienia i wylaczenie usypiania zmieniaja sie poza aplikacja,
    // wiec stan odswieza sie przy kazdym powrocie na ekran. Inaczej karta straszylaby
    // dalej po tym, jak wszystko juz zostalo ustawione.
    var notificationsAllowed by remember { mutableStateOf(canPostNotifications(context)) }
    var unrestricted by remember { mutableStateOf(PowerSaving.isUnrestricted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = canPostNotifications(context)
                unrestricted = PowerSaving.isUnrestricted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val askPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationsAllowed = granted }

    if (pickingReturn) {
        PickDateDialog(
            initial = vacation?.until ?: today.plusWeeks(1),
            today = today,
            confirmLabel = "Ustaw powrót",
            onDismiss = { pickingReturn = false },
            onPicked = {
                onStartVacation(it)
                pickingReturn = false
            },
        )
    }

    pickingTimeFor?.let { slot ->
        val current = if (slot == Slot.RANO) morning else afternoon
        TimeDialog(
            initial = current.time,
            onDismiss = { pickingTimeFor = null },
            onPicked = { time ->
                val updated = current.copy(time = time)
                if (slot == Slot.RANO) onMorningChange(updated) else onAfternoonChange(updated)
                pickingTimeFor = null
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenHeader(
                title = "Ustawienia",
                subtitle = "Momentum ${BuildConfig.VERSION_NAME}",
                action = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ReminderCard(
                    title = "Podsumowanie dnia",
                    description = "Ile zadań, ile nawyków i co jest zaległe.",
                    reminder = morning,
                    onToggle = { onMorningChange(morning.copy(enabled = it)) },
                    onPickTime = { pickingTimeFor = Slot.RANO },
                )
            }

            item {
                ReminderCard(
                    title = "Kopniak",
                    description = "Co zostało otwarte. Przy pustej liście nie przychodzi.",
                    reminder = afternoon,
                    onToggle = { onAfternoonChange(afternoon.copy(enabled = it)) },
                    onPickTime = { pickingTimeFor = Slot.POPOLUDNIE },
                )
            }

            if (!notificationsAllowed) {
                item {
                    WarningCard(
                        title = "Powiadomienia zablokowane",
                        text = "Bez zgody systemu przypomnienia nie mają jak się pokazać.",
                        action = "Pozwól",
                        onAction = { askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    )
                }
            }

            if (!unrestricted) {
                item {
                    WarningCard(
                        title = "Aplikacja może zostać uśpiona",
                        text = "Samsung agresywnie usypia aplikacje i to zabija zaplanowane " +
                            "przypomnienia. Ustaw baterię na „Nieograniczone”, a potem sprawdź " +
                            "w ustawieniach aplikacji, czy Momentum nie siedzi na liście uśpionych.",
                        action = "Zezwól na tło",
                        onAction = { PowerSaving.requestUnrestricted(context) },
                        secondary = "Ustawienia aplikacji",
                        onSecondary = { PowerSaving.openAppSettings(context) },
                    )
                }
            }

            item {
                VacationCard(
                    vacation = vacation,
                    today = today,
                    onToggle = { on -> if (on) onStartVacation(null) else onEndVacation() },
                    onPickReturn = { pickingReturn = true },
                    onClearReturn = { onStartVacation(null) },
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    title: String,
    description: String,
    reminder: Reminder,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Switch(checked = reminder.enabled, onCheckedChange = onToggle)
            }

            if (reminder.enabled) {
                TextButton(onClick = onPickTime, contentPadding = PaddingValues(0.dp)) {
                    Text("Godzina: ${reminder.time.format(TIME_FORMAT)}")
                }
            }
        }
    }
}

/**
 * Karta dla rzeczy ustawianych poza aplikacja. Pokazuje sie wylacznie wtedy, gdy cos
 * jest nie tak - stala checklista, ktora zwykle jest cala na zielono, przestaje byc
 * czytana po tygodniu i wtedy nie zauwazasz, gdy naprawde zrobi sie czerwona.
 */
@Composable
private fun WarningCard(
    title: String,
    text: String,
    action: String,
    onAction: () -> Unit,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = scheme.errorContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onErrorContainer,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onErrorContainer,
            )
            Row {
                TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                    Text(action, color = scheme.onErrorContainer)
                }
                if (secondary != null && onSecondary != null) {
                    Spacer(Modifier.padding(horizontal = 8.dp))
                    TextButton(onClick = onSecondary, contentPadding = PaddingValues(0.dp)) {
                        Text(secondary, color = scheme.onErrorContainer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onPicked: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPicked(LocalTime.of(state.hour, state.minute)) }) {
                Text("Ustaw")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
        text = { TimePicker(state = state) },
    )
}

@Composable
private fun VacationCard(
    vacation: Vacation?,
    today: LocalDate,
    onToggle: (Boolean) -> Unit,
    onPickReturn: () -> Unit,
    onClearReturn: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val active = vacation != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Tryb urlopowy",
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "Wstrzymuje wszystkie nawyki naraz. Dni urlopu nie liczą " +
                            "się ani jako zrobione, ani jako pominięte — momentum stoi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(0.dp))
                Switch(checked = active, onCheckedChange = onToggle)
            }

            if (vacation != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = buildString {
                        append("Trwa od ")
                        append(vacation.from.format(DATE_FORMAT))
                        append(
                            if (vacation.until == null) {
                                " · do odwołania"
                            } else {
                                " do ${vacation.until.format(DATE_FORMAT)}"
                            }
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                )

                Row {
                    TextButton(onClick = onPickReturn, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            if (vacation.until == null) {
                                "Ustaw datę powrotu"
                            } else {
                                "Zmień datę powrotu"
                            }
                        )
                    }
                    if (vacation.until != null) {
                        Spacer(Modifier.padding(horizontal = 8.dp))
                        TextButton(onClick = onClearReturn, contentPadding = PaddingValues(0.dp)) {
                            Text("Bez daty")
                        }
                    }
                }
            }
        }
    }
}

private fun canPostNotifications(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
