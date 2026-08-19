package dev.slusa.momentum.ui.today

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.domain.Aging
import dev.slusa.momentum.ui.theme.LocalAgeRamp

/**
 * Kafelek zadania. Pasek starzenia jest waski i przy lewej krawedzi - kolor ma
 * byc sygnalem, ktory da sie zignorowac katem oka, a nie krzykiem na pol ekranu.
 */
@Composable
fun TodoRow(
    item: TodoUi,
    onToggleDone: () -> Unit,
    onToggleToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ramp = LocalAgeRamp.current
    val stripe by animateColorAsState(
        targetValue = if (item.onTodayList) Aging.color(item.ageDays, ramp) else Color.Transparent,
        label = "pasek starzenia",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(stripe)
            )

            Spacer(Modifier.width(12.dp))

            CheckCircle(done = item.todo.isDone, onClick = onToggleDone)

            Spacer(Modifier.width(12.dp))

            Text(
                text = item.todo.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.todo.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (item.todo.isDone) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
            )

            if (!item.todo.isDone) {
                Spacer(Modifier.width(8.dp))
                TodayChip(active = item.onTodayList, onClick = onToggleToday)
            }

            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun CheckCircle(done: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (done) scheme.primary else Color.Transparent)
            .border(
                width = if (done) 0.dp else 2.dp,
                color = if (done) Color.Transparent else scheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Cofnij odhaczenie",
                tint = scheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TodayChip(active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
