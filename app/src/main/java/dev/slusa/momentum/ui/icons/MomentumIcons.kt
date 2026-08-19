package dev.slusa.momentum.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Wlasne ikony. Paczka material-icons-core ma bardzo waski zestaw, a pelna
 * material-icons-extended to kilkadziesiat megabajtow wektorow, ktore bez
 * minifikacji w calosci wladowalyby sie do APK. Kilka wlasnych ksztaltow
 * jest tansze i lepiej pasuje do reszty.
 */
object MomentumIcons {

    /** Klepsydra - "kiedys", czyli odsuniecie w czasie bez konkretnej daty. */
    val Hourglass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Hourglass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // gorna belka
                moveTo(5f, 3f); lineTo(19f, 3f); lineTo(19f, 5f); lineTo(5f, 5f); close()
                // dolna belka
                moveTo(5f, 19f); lineTo(19f, 19f); lineTo(19f, 21f); lineTo(5f, 21f); close()
                // gorny stozek - piasek jeszcze nie przesypany
                moveTo(6.5f, 5f); lineTo(17.5f, 5f); lineTo(12f, 11.4f); close()
                // dolny stozek
                moveTo(12f, 12.6f); lineTo(17.5f, 19f); lineTo(6.5f, 19f); close()
            }
        }.build()
    }

    /** Plomyk - nawyki i momentum. */
    val Flame: ImageVector by lazy {
        ImageVector.Builder(
            name = "Flame",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(12f, 2f, 6.5f, 6.5f, 6.5f, 13f)
                curveTo(6.5f, 17.1f, 9f, 20f, 12f, 20f)
                curveTo(15f, 20f, 17.5f, 17.1f, 17.5f, 13f)
                curveTo(17.5f, 9.5f, 15.5f, 7.5f, 14.5f, 6f)
                curveTo(14.5f, 8.5f, 13.5f, 9.5f, 12.5f, 10f)
                curveTo(13f, 7f, 12f, 4f, 12f, 2f)
                close()
            }
        }.build()
    }
}
