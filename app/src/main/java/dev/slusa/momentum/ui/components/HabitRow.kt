package dev.slusa.momentum.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.ui.HabitUi

/**
 * Nawyk na liscie ToDo. Momentum jest tu tylko liczba - pelna siatke pokazuje
 * zakladka Nawyki, zeby codzienna lista nie zamienila sie w kokpit statystyk.
 */
@Composable
fun HabitRow(
    item: HabitUi,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (item.doneToday) scheme.primary else Color.Transparent)
                    .border(
                        width = if (item.doneToday) 0.dp else 2.dp,
                        color = if (item.doneToday) Color.Transparent else scheme.outline,
                        shape = CircleShape,
                    )
                    .clickable(onClick = onToggleDone),
                contentAlignment = Alignment.Center,
            ) {
                if (item.doneToday) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Cofnij odhaczenie",
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = item.habit.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                )
                Text(
                    text = "momentum ${item.momentum}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }

            MomentumPips(item.momentum)
        }
    }
}

/** Dziesiec kropek zamiast paska - w jednej linii kafelka czyta sie lepiej. */
@Composable
private fun MomentumPips(value: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(10) { index ->
            Spacer(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(if (index < value) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(if (index < value) scheme.primary else scheme.surfaceVariant)
            )
        }
    }
}
