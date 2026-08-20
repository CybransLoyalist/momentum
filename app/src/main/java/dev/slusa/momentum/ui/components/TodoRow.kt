package dev.slusa.momentum.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.domain.Aging
import dev.slusa.momentum.domain.contrastOn
import dev.slusa.momentum.ui.TodoUi
import dev.slusa.momentum.ui.theme.LocalAgeRamp

// Odstepy wiersza trzymane jako stale, bo plama starzenia jest z nimi zwiazana:
// ma przykryc kolko odhaczania i zgasnac, zanim zacznie sie tytul.
private val ROW_START = 14.dp
private val CIRCLE_SIZE = 24.dp
private val CIRCLE_GAP = 14.dp

/** Tytul zaczyna sie tutaj - plama musi byc wczesniej przezroczysta. */
private val TEXT_START = ROW_START + CIRCLE_SIZE + CIRCLE_GAP

private val STAIN_WIDTH = TEXT_START - 2.dp

/** Do konca kolka pelny kolor, dalej gasniecie - inaczej cel zniknalby razem z plama. */
private val STAIN_SOLID = (ROW_START + CIRCLE_SIZE).value / STAIN_WIDTH.value

/**
 * Kafelek zadania, wspolny dla wszystkich list.
 *
 * Kolor starzenia idzie od lewej krawedzi jako plama gasnaca w prawo, przechodzac pod
 * kolkiem odhaczania. Waski pasek przy samej krawedzi byl zbyt dyskretny - dawal sie
 * zignorowac katem oka az do skutku. Gradient zamiast rownego bloku, bo pelny prostokat
 * na kazdym starym zadaniu robilby z listy sciane czerni, przed czym broni sie spec.
 */
@Composable
fun TodoRow(
    item: TodoUi,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val ramp = LocalAgeRamp.current
    val stain by animateColorAsState(
        targetValue = if (item.ages && !item.todo.isDone) {
            Aging.color(item.ageDays, ramp)
        } else {
            Color.Transparent
        },
        label = "plama starzenia",
    )

    val stainWidthPx = with(LocalDensity.current) { STAIN_WIDTH.toPx() }
    val brush = Brush.horizontalGradient(
        colorStops = arrayOf(
            0f to stain,
            STAIN_SOLID to stain,
            1f to stain.copy(alpha = 0f),
        ),
        startX = 0f,
        endX = stainWidthPx,
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .background(brush),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(ROW_START))

            CheckCircle(
                size = CIRCLE_SIZE,
                done = item.todo.isDone,
                onBackground = stain,
                onClick = onToggleDone,
            )

            Spacer(Modifier.width(CIRCLE_GAP))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(vertical = 14.dp),
            ) {
                Text(
                    text = item.todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.todo.isDone) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (item.todo.isDone) TextDecoration.LineThrough else null,
                )

                // Etykieta powtarzania idzie pod tytulem, a nie w trailing - tam siedza
                // juz przelacznik "dzis" i data terminu, zaleznie od listy.
                item.rule?.let { rule ->
                    Text(
                        text = rule.describe(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (trailing != null && !item.todo.isDone) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }

            Spacer(Modifier.width(12.dp))
        }
    }
}

/**
 * Kolko odhaczania. Obwodka dostosowuje sie do tego, na czym stoi - na ciemnej plamie
 * starzenia zwykly szary kontur znikal razem z celem, w ktory trzeba trafic.
 */
@Composable
private fun CheckCircle(
    size: androidx.compose.ui.unit.Dp,
    done: Boolean,
    onBackground: Color,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ring = if (onBackground.alpha > 0.15f) {
        contrastOn(onBackground)
    } else {
        scheme.outline
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (done) scheme.primary else Color.Transparent)
            .border(
                width = if (done) 0.dp else 2.dp,
                color = if (done) Color.Transparent else ring,
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

/** Maly przelacznik "na dzisiaj" po prawej stronie kafelka. */
@Composable
fun TodayChip(active: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (active) scheme.primaryContainer else Color.Transparent,
        border = BorderStroke(
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

/** Etykieta terminu, uzywana na liscie zaplanowanych. */
@Composable
fun DateChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
