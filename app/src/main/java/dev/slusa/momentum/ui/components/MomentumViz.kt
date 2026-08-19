package dev.slusa.momentum.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.slusa.momentum.domain.DayState
import dev.slusa.momentum.domain.Momentum
import java.time.LocalDate

/**
 * Siatka ostatnich osmiu tygodni. Poza sama liczba pokazuje wzorce - "zawsze
 * sypie mi sie w weekendy" widac tu od razu, a z licznika nigdy.
 */
@Composable
fun MomentumGrid(
    cells: List<Pair<LocalDate, DayState>>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        cells.chunked(7).forEach { week ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { (_, dayState) ->
                    val shape = RoundedCornerShape(2.dp)
                    val base = Modifier.size(9.dp).clip(shape)

                    when (dayState) {
                        DayState.ZROBIONE ->
                            Spacer(base.background(scheme.primary))

                        DayState.POMINIETE ->
                            Spacer(base.background(scheme.error.copy(alpha = 0.55f)))

                        DayState.WOLNE ->
                            Spacer(base.background(scheme.surfaceVariant))

                        DayState.DZISIAJ ->
                            Spacer(base.border(1.5.dp, scheme.primary, shape))

                        DayState.PAUZA ->
                            Spacer(base.background(scheme.surfaceVariant.copy(alpha = 0.5f)))

                        DayState.PRZYSZLOSC, DayState.PRZED_STARTEM ->
                            Spacer(base.border(1.dp, scheme.outline.copy(alpha = 0.4f), shape))
                    }
                }
            }
        }
    }
}

/** Licznik 0-10 z paskiem. Liczba mowi ile, pasek mowi jak blisko maksa. */
@Composable
fun MomentumBadge(value: Int, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
            color = if (value > 0) scheme.primary else scheme.outline,
        )
        Text(
            text = "/${Momentum.MAX}",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.outline,
        )
        Spacer(Modifier.width(8.dp))
        MomentumBar(value)
    }
}

@Composable
private fun MomentumBar(value: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(Momentum.MAX) { index ->
            Spacer(
                Modifier
                    .width(5.dp)
                    .height(if (index < value) 12.dp else 6.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (index < value) scheme.primary else scheme.surfaceVariant)
            )
        }
    }
}

/** Legenda pod siatka - bez niej kolory sa zgadywanka. */
@Composable
fun GridLegend(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem("zrobione", scheme.primary)
        LegendItem("pominięte", scheme.error.copy(alpha = 0.55f))
        LegendItem("wolne", scheme.surfaceVariant)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
