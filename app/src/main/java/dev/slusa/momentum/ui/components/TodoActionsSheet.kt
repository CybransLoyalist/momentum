package dev.slusa.momentum.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.data.Bucket
import dev.slusa.momentum.ui.TodoUi

/**
 * Jedno miejsce na wszystkie akcje, ktore da sie zrobic z zadaniem. Alternatywa,
 * czyli gesty przesuniecia, jest szybsza, ale niewidoczna - a tu wiekszosc akcji
 * wykonuje sie rzadko i wazniejsze jest, zeby dalo sie je znalezc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoActionsSheet(
    item: TodoUi,
    onDismiss: () -> Unit,
    onToggleToday: () -> Unit,
    onPickDate: () -> Unit,
    onEdit: () -> Unit,
    onMoveTo: (Bucket) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text(
                text = item.todo.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline,
            )

            when (item.todo.bucket) {
                Bucket.GLOWNE -> {
                    SheetAction(
                        label = if (item.onTodayList) "Zdejmij z dzisiaj" else "Zrób to dzisiaj",
                        onClick = onToggleToday,
                    )
                    // Sam kalendarz zostaje osobna pozycja, bo przelozenie na jutro
                    // to dwa dotkniecia, a przez edytor byloby piec.
                    SheetAction("Zaplanuj na inny dzień", onClick = onPickDate)
                    SheetAction("Edytuj…", onClick = onEdit)
                    SheetAction("Odłóż na kiedyś") { onMoveTo(Bucket.KIEDYS) }
                    SheetAction("Przenieś do zakupów") { onMoveTo(Bucket.ZAKUPY) }
                }

                Bucket.KIEDYS -> {
                    SheetAction("Przenieś na listę główną") { onMoveTo(Bucket.GLOWNE) }
                    SheetAction("Zaplanuj na konkretny dzień", onClick = onPickDate)
                    SheetAction("Zmień nazwę…", onClick = onEdit)
                }

                Bucket.ZAKUPY -> {
                    SheetAction("Przenieś na listę główną") { onMoveTo(Bucket.GLOWNE) }
                    SheetAction("Zmień nazwę…", onClick = onEdit)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline,
            )

            SheetAction("Usuń", destructive = true, onClick = onDelete)
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
