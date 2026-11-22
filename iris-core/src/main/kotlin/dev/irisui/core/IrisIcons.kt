package dev.irisui.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private fun icon(
    name: String,
    filled: Boolean = false,
    strokeWidth: Float = 2f,
    block: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
    fill = if (filled) SolidColor(Color.Black) else null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = strokeWidth,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = block,
).build()

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    close()
}

private fun PathBuilder.polygon(vararg pts: Float) {
    moveTo(pts[0], pts[1])
    var i = 2
    while (i < pts.size) {
        lineTo(pts[i], pts[i + 1])
        i += 2
    }
    close()
}

/** A small stroke-based icon set so the kit has no dependency on an icon library. */
object IrisIcons {
    /** Back arrow with a long shaft, as in the ColorOS toolbar (ink 20 x 16 dp inside the 24 dp box). */
    val Back: ImageVector by lazy {
        icon("Back", strokeWidth = 1.2f) {
            moveTo(10.2f, 4.6f); lineTo(2.6f, 12f); lineTo(10.2f, 19.4f)
            moveTo(2.6f, 12f); lineTo(21.4f, 12f)
        }
    }
    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight", strokeWidth = 1.8f) { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
    }
    val Plus: ImageVector by lazy {
        icon("Plus") { moveTo(12f, 5f); lineTo(12f, 19f); moveTo(5f, 12f); lineTo(19f, 12f) }
    }
    val Close: ImageVector by lazy {
        icon("Close") { moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f) }
    }
    val Check: ImageVector by lazy {
        icon("Check") { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }
    }
    val Search: ImageVector by lazy {
        icon("Search") {
            circle(10.5f, 10.5f, 6.5f)
            moveTo(15.4f, 15.4f); lineTo(20f, 20f)
        }
    }
    val More: ImageVector by lazy {
        icon("More", strokeWidth = 3.6f) {
            moveTo(12f, 5.5f); lineTo(12f, 5.5f)
            moveTo(12f, 12f); lineTo(12f, 12f)
            moveTo(12f, 18.5f); lineTo(12f, 18.5f)
        }
    }
    val Sun: ImageVector by lazy {
        icon("Sun") {
            circle(12f, 12f, 4f)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                val c = cos(a).toFloat()
                val s = sin(a).toFloat()
                moveTo(12f + 7.2f * c, 12f + 7.2f * s)
                lineTo(12f + 9.4f * c, 12f + 9.4f * s)
            }
        }
    }
    val Home: ImageVector by lazy {
        icon("Home") {
            moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f)
            moveTo(6f, 9.5f); lineTo(6f, 19f); lineTo(18f, 19f); lineTo(18f, 9.5f)
        }
    }
    val HomeFilled: ImageVector by lazy {
        icon("HomeFilled", filled = true) { polygon(4f, 11f, 12f, 4f, 20f, 11f, 18f, 11f, 18f, 19f, 6f, 19f, 6f, 11f) }
    }
    val Heart: ImageVector by lazy {
        icon("Heart") { heartPath() }
    }
    val HeartFilled: ImageVector by lazy {
        icon("HeartFilled", filled = true) { heartPath() }
    }
    val Person: ImageVector by lazy {
        icon("Person") {
            circle(12f, 8f, 4f)
            moveTo(4.5f, 20f); arcTo(7.5f, 5.5f, 0f, false, true, 19.5f, 20f)
        }
    }
    val PersonFilled: ImageVector by lazy {
        icon("PersonFilled", filled = true) {
            circle(12f, 8f, 4f)
            moveTo(4.5f, 20f); arcTo(7.5f, 5.5f, 0f, false, true, 19.5f, 20f); close()
        }
    }
    val Bell: ImageVector by lazy {
        icon("Bell") {
            moveTo(6f, 17f); lineTo(6f, 11f); arcTo(6f, 6f, 0f, false, true, 18f, 11f); lineTo(18f, 17f)
            lineTo(19.5f, 18.5f); lineTo(4.5f, 18.5f); close()
            moveTo(10f, 21f); lineTo(14f, 21f)
        }
    }
    val BellFilled: ImageVector by lazy {
        icon("BellFilled", filled = true) {
            moveTo(6f, 17f); lineTo(6f, 11f); arcTo(6f, 6f, 0f, false, true, 18f, 11f); lineTo(18f, 17f)
            lineTo(19.5f, 18.5f); lineTo(4.5f, 18.5f); close()
            moveTo(10f, 21f); lineTo(14f, 21f)
        }
    }
    val Grid: ImageVector by lazy {
        icon("Grid") {
            polygon(4f, 4f, 10f, 4f, 10f, 10f, 4f, 10f)
            polygon(14f, 4f, 20f, 4f, 20f, 10f, 14f, 10f)
            polygon(4f, 14f, 10f, 14f, 10f, 20f, 4f, 20f)
            polygon(14f, 14f, 20f, 14f, 20f, 20f, 14f, 20f)
        }
    }
    val GridFilled: ImageVector by lazy {
        icon("GridFilled", filled = true) {
            polygon(4f, 4f, 10f, 4f, 10f, 10f, 4f, 10f)
            polygon(14f, 4f, 20f, 4f, 20f, 10f, 14f, 10f)
            polygon(4f, 14f, 10f, 14f, 10f, 20f, 4f, 20f)
            polygon(14f, 14f, 20f, 14f, 20f, 20f, 14f, 20f)
        }
    }
    val Tune: ImageVector by lazy {
        icon("Tune") {
            moveTo(4f, 8f); lineTo(20f, 8f)
            moveTo(4f, 16f); lineTo(20f, 16f)
            circle(9f, 8f, 2.4f)
            circle(15f, 16f, 2.4f)
        }
    }
    val Star: ImageVector by lazy {
        icon("Star") { starPath() }
    }
    val StarFilled: ImageVector by lazy {
        icon("StarFilled", filled = true) { starPath() }
    }
    val Alarm: ImageVector by lazy {
        icon("Alarm") {
            circle(12f, 13f, 7f)
            moveTo(12f, 9.5f); lineTo(12f, 13f); lineTo(14.5f, 14.8f)
            moveTo(4.5f, 5.5f); lineTo(7.5f, 3.5f)
            moveTo(19.5f, 5.5f); lineTo(16.5f, 3.5f)
        }
    }
    /** Alarm clock with a solid face, the selected state of the Alarm tab. */
    val AlarmFilled: ImageVector by lazy {
        icon("AlarmFilled", filled = true) {
            circle(12f, 13f, 7f)
            moveTo(4.5f, 5.5f); lineTo(7.5f, 3.5f)
            moveTo(19.5f, 5.5f); lineTo(16.5f, 3.5f)
        }
    }
    val Globe: ImageVector by lazy {
        icon("Globe", strokeWidth = 1.6f) {
            circle(12f, 12f, 9f)
            moveTo(3f, 12f); lineTo(21f, 12f)
            moveTo(12f, 3f); arcTo(4.5f, 9f, 0f, false, false, 12f, 21f); arcTo(4.5f, 9f, 0f, false, false, 12f, 3f)
            moveTo(4.2f, 7.5f); lineTo(19.8f, 7.5f)
            moveTo(4.2f, 16.5f); lineTo(19.8f, 16.5f)
        }
    }
    val Stopwatch: ImageVector by lazy {
        icon("Stopwatch", strokeWidth = 1.8f) {
            circle(12f, 13.5f, 7.5f)
            moveTo(12f, 13.5f); lineTo(12f, 9.5f)
            moveTo(9.5f, 3f); lineTo(14.5f, 3f)
            moveTo(12f, 3f); lineTo(12f, 6f)
        }
    }
    val Hourglass: ImageVector by lazy {
        icon("Hourglass", strokeWidth = 1.8f) {
            moveTo(6f, 3.5f); lineTo(18f, 3.5f)
            moveTo(6f, 20.5f); lineTo(18f, 20.5f)
            moveTo(7.5f, 3.5f); lineTo(7.5f, 7f); lineTo(12f, 12f); lineTo(7.5f, 17f); lineTo(7.5f, 20.5f)
            moveTo(16.5f, 3.5f); lineTo(16.5f, 7f); lineTo(12f, 12f); lineTo(16.5f, 17f); lineTo(16.5f, 20.5f)
        }
    }
    /** Speaker with a slash: media volume at zero. */
    val SpeakerMuted: ImageVector by lazy {
        icon("SpeakerMuted", filled = true, strokeWidth = 1.8f) {
            moveTo(4f, 9.5f); lineTo(8f, 9.5f); lineTo(12.5f, 5.5f); lineTo(12.5f, 18.5f); lineTo(8f, 14.5f); lineTo(4f, 14.5f); close()
            moveTo(16f, 9f); lineTo(21f, 15f)
            moveTo(21f, 9f); lineTo(16f, 15f)
        }
    }
    /** Music note with vibration waves, the ringtone row. */
    val MusicVibrate: ImageVector by lazy {
        icon("MusicVibrate", filled = true, strokeWidth = 1.8f) {
            moveTo(10f, 17f); lineTo(10f, 5f); lineTo(17f, 3.5f); lineTo(17f, 15.5f)
            circle(7.8f, 17f, 2.2f)
            circle(14.8f, 15.5f, 2.2f)
            moveTo(20f, 8f); lineTo(20f, 12f)
            moveTo(22.3f, 9f); lineTo(22.3f, 11f)
            moveTo(3.5f, 9f); lineTo(3.5f, 13f)
        }
    }
    val Trash: ImageVector by lazy {
        icon("Trash") {
            moveTo(5f, 7f); lineTo(19f, 7f)
            moveTo(10f, 4f); lineTo(14f, 4f)
            moveTo(7f, 7f); lineTo(7f, 18f); arcTo(2f, 2f, 0f, false, false, 9f, 20f)
            lineTo(15f, 20f); arcTo(2f, 2f, 0f, false, false, 17f, 18f); lineTo(17f, 7f)
            moveTo(10f, 11f); lineTo(10f, 16f)
            moveTo(14f, 11f); lineTo(14f, 16f)
        }
    }
}

private fun PathBuilder.heartPath() {
    moveTo(12f, 20.5f)
    curveTo(5f, 15.5f, 3f, 12f, 3f, 8.8f)
    curveTo(3f, 6.2f, 5f, 4.5f, 7.3f, 4.5f)
    curveTo(9.2f, 4.5f, 11f, 5.6f, 12f, 7.4f)
    curveTo(13f, 5.6f, 14.8f, 4.5f, 16.7f, 4.5f)
    curveTo(19f, 4.5f, 21f, 6.2f, 21f, 8.8f)
    curveTo(21f, 12f, 19f, 15.5f, 12f, 20.5f)
    close()
}

private fun PathBuilder.starPath() {
    val cx = 12f
    val cy = 12.6f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) 9f else 4f
        val a = Math.toRadians(-90.0 + i * 36.0)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
