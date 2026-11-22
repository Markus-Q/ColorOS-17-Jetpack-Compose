package dev.irisui.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Rounded rectangle with "continuous" (squircle) corners, the corner style ColorOS and iOS use.
 * [smoothing] 0 gives plain circular corners, ~0.5 gives the soft ColorOS look.
 * The curve construction follows the well known corner-smoothing algorithm used by design tools.
 */
@Immutable
class SmoothRoundedCornerShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val smoothing: Float = 0.55f,
) : Shape {
    constructor(radius: Dp, smoothing: Float = 0.55f) : this(radius, radius, radius, radius, smoothing)

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val budget = min(w, h) / 2f
        val tl = with(density) { topStart.toPx() }.coerceIn(0f, budget)
        val tr = with(density) { topEnd.toPx() }.coerceIn(0f, budget)
        val br = with(density) { bottomEnd.toPx() }.coerceIn(0f, budget)
        val bl = with(density) { bottomStart.toPx() }.coerceIn(0f, budget)

        val path = Path()
        var first = true
        fun corner(r: Float, cx: Float, cy: Float, ux: Float, uy: Float, vx: Float, vy: Float) {
            if (r < 0.5f) {
                if (first) path.moveTo(cx, cy) else path.lineTo(cx, cy)
                first = false
                return
            }
            var s = smoothing
            var p = (1f + s) * r
            if (p > budget) {
                s = max(0f, budget / r - 1f)
                p = min(p, budget)
            }
            val arcMeasure = 90f * (1f - s)
            val arcLen = sin(Math.toRadians((arcMeasure / 2f).toDouble())).toFloat() * r * sqrt(2f)
            val angleAlpha = (90f - arcMeasure) / 2f
            val p3p4 = r * tan(Math.toRadians((angleAlpha / 2f).toDouble())).toFloat()
            val beta = Math.toRadians((45f * s).toDouble())
            val c = p3p4 * cos(beta).toFloat()
            val d = c * tan(beta).toFloat()
            val b = (p - arcLen - c - d) / 3f
            val a = 2f * b

            // local (x along incoming edge, y along outgoing edge) -> screen
            fun lx(x: Float, y: Float) = x * ux + y * vx
            fun ly(x: Float, y: Float) = x * uy + y * vy

            val startX = cx - ux * p
            val startY = cy - uy * p
            if (first) path.moveTo(startX, startY) else path.lineTo(startX, startY)
            first = false

            path.relativeCubicTo(lx(a, 0f), ly(a, 0f), lx(a + b, 0f), ly(a + b, 0f), lx(a + b + c, d), ly(a + b + c, d))

            val theta = Math.toRadians(arcMeasure.toDouble())
            val k = (4.0 / 3.0 * tan(theta / 4.0)).toFloat() * r
            val t0x = cos(beta).toFloat()
            val t0y = sin(beta).toFloat()
            val t3x = sin(beta).toFloat()
            val t3y = cos(beta).toFloat()
            val p3x = arcLen
            val p3y = arcLen
            path.relativeCubicTo(
                lx(k * t0x, k * t0y), ly(k * t0x, k * t0y),
                lx(p3x - k * t3x, p3y - k * t3y), ly(p3x - k * t3x, p3y - k * t3y),
                lx(p3x, p3y), ly(p3x, p3y),
            )

            path.relativeCubicTo(lx(d, c), ly(d, c), lx(d, b + c), ly(d, b + c), lx(d, a + b + c), ly(d, a + b + c))
        }

        corner(tl, 0f, 0f, 0f, -1f, 1f, 0f)
        corner(tr, w, 0f, 1f, 0f, 0f, 1f)
        corner(br, w, h, 0f, 1f, -1f, 0f)
        corner(bl, 0f, h, -1f, 0f, 0f, -1f)
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is SmoothRoundedCornerShape && topStart == other.topStart && topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd && bottomStart == other.bottomStart && smoothing == other.smoothing

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + smoothing.hashCode()
        return result
    }
}

/** Shape tokens. Radii were measured on a OnePlus 15 running ColorOS 17. */
@Immutable
class IrisShapes(
    val small: Shape = SmoothRoundedCornerShape(10.dp),
    val field: Shape = SmoothRoundedCornerShape(16.dp),
    val card: Shape = SmoothRoundedCornerShape(20.dp, smoothing = 0.55f),
    val popup: Shape = SmoothRoundedCornerShape(23.dp),
    val dialog: Shape = SmoothRoundedCornerShape(30.dp),
    val sheet: Shape = SmoothRoundedCornerShape(32.dp, 32.dp, 0.dp, 0.dp),
    val pill: Shape = RoundedCornerShape(percent = 50),
)
