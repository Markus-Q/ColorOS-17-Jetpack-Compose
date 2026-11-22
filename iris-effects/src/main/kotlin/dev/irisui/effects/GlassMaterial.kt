package dev.irisui.effects

import android.graphics.BlendModeColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One colour layer of a glass material. [color] (with its alpha) is blended onto what is below it with [blendMode].
 * ColorOS only uses [BlendMode.SrcOver], [BlendMode.Overlay], [BlendMode.ColorDodge] and [BlendMode.Luminosity].
 */
@Immutable
data class GlassLayer(val color: Color, val blendMode: BlendMode)

/**
 * The recipe of every glass surface of ColorOS 17, matched on a OnePlus 15: the backdrop is blurred with a Gaussian of
 * [blur], then passed through [layers], each one a
 * colour blended with its own blend mode. Nothing else is involved, in particular there is no flat translucent tint:
 * Overlay / ColorDodge keep the colours of what is behind the glass, Luminosity keeps its hue but takes the colour's
 * brightness.
 */
@Immutable
data class GlassMaterial(
    /** Standard deviation of the Gaussian. The stock values are pixels on a 480 dpi screen, so 3 px = 1 dp. */
    val blur: Dp,
    val layers: List<GlassLayer>,
    /**
     * Window level materials (popup, dialog, sheet) are blurred by the compositor, which also sees the content just
     * outside the glass. View level materials (bars, buttons) only see what is behind the view and mirror at its edges.
     */
    val sampleOutside: Boolean = false,
) {
    /**
     * The colour this material produces over a uniform, opaque background. Used as the stand-in where blur effects
     * are not available (API < 31) and by the catalog's blur lab.
     */
    fun over(background: Color): Color {
        var r = background.red
        var g = background.green
        var b = background.blue
        for (layer in layers) {
            val out = blend(layer.color, layer.blendMode, r, g, b)
            r = out[0]; g = out[1]; b = out[2]
        }
        return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
    }

    /** Identity of the effect chain, so the node only rebuilds it when something really changed. */
    internal fun effectKey(density: Float): Long {
        var h = (blur.value * density).toBits().toLong()
        for (l in layers) h = h * 31 + l.color.toArgb() * 7L + l.blendMode.hashCode()
        return h
    }
}

private fun blend(src: Color, mode: BlendMode, dr: Float, dg: Float, db: Float): FloatArray {
    val a = src.alpha
    val s = floatArrayOf(src.red, src.green, src.blue)
    val d = floatArrayOf(dr, dg, db)
    val x = FloatArray(3)
    when (mode) {
        BlendMode.Overlay -> for (i in 0..2) x[i] = if (d[i] < 0.5f) 2f * s[i] * d[i] else 1f - 2f * (1f - s[i]) * (1f - d[i])
        BlendMode.ColorDodge -> for (i in 0..2) x[i] = if (d[i] <= 0f) 0f else if (s[i] >= 1f) 1f else minOf(1f, d[i] / (1f - s[i]))
        BlendMode.Luminosity -> {
            // W3C non-separable blend (SetLum / ClipColor, weights 0.30 / 0.59 / 0.11).
            val sl = 0.3f * s[0] + 0.59f * s[1] + 0.11f * s[2]
            val dl = 0.3f * d[0] + 0.59f * d[1] + 0.11f * d[2]
            val c = floatArrayOf(d[0] + sl - dl, d[1] + sl - dl, d[2] + sl - dl)
            val mn = minOf(c[0], c[1], c[2])
            val mx = maxOf(c[0], c[1], c[2])
            for (i in 0..2) {
                x[i] = when {
                    mn < 0f -> sl + (c[i] - sl) * sl / (sl - mn)
                    mx > 1f -> sl + (c[i] - sl) * (1f - sl) / (mx - sl)
                    else -> c[i]
                }
            }
        }
        else -> for (i in 0..2) x[i] = s[i] // SrcOver
    }
    return floatArrayOf(
        d[0] * (1f - a) + x[0] * a,
        d[1] * (1f - a) + x[1] * a,
        d[2] * (1f - a) + x[2] * a,
    )
}

@RequiresApi(29)
private fun BlendMode.toAndroid(): android.graphics.BlendMode = when (this) {
    BlendMode.Overlay -> android.graphics.BlendMode.OVERLAY
    BlendMode.ColorDodge -> android.graphics.BlendMode.COLOR_DODGE
    BlendMode.Luminosity -> android.graphics.BlendMode.LUMINOSITY
    BlendMode.Multiply -> android.graphics.BlendMode.MULTIPLY
    BlendMode.Screen -> android.graphics.BlendMode.SCREEN
    else -> android.graphics.BlendMode.SRC_OVER
}

/**
 * Builds the render effect chain of the material (`RenderEffect.createBlurEffect(r, r, MIRROR)` followed by one
 * `BlendModeColorFilter` per layer). The blur mirrors at the edges of the glass, so only the content behind the glass
 * itself shows up in it, exactly like the real thing.
 */
@RequiresApi(31)
internal fun GlassMaterial.buildRenderEffect(blurPx: Float): androidx.compose.ui.graphics.RenderEffect {
    var effect: RenderEffect? = if (blurPx > 0.01f) {
        RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.MIRROR)
    } else {
        null
    }
    for (layer in layers) {
        val filter = BlendModeColorFilter(layer.color.toArgb(), layer.blendMode.toAndroid())
        effect = if (effect == null) RenderEffect.createColorFilterEffect(filter) else RenderEffect.createColorFilterEffect(filter, effect)
    }
    return (effect ?: RenderEffect.createBlurEffect(0.01f, 0.01f, Shader.TileMode.MIRROR)).asComposeRenderEffect()
}

/**
 * The materials of ColorOS 17, matched on a OnePlus 15: view level materials (bars, buttons) and window level
 * materials (popup menu, dialog, bottom sheet).
 *
 * All blur values are pixels at 480 dpi in the system, converted to dp here (3 px = 1 dp). The window level radius
 * units of the system (160 popup, 200 dialog, 400 sheet) are 6 units per pixel of Gaussian sigma.
 */
object GlassMaterials {
    private fun px(v: Float): Dp = (v / 3f).dp
    private fun c(argb: Long) = Color(argb)

    /** Floating bottom navigation pill. Over black it is #1B1B1B (dark) and keeps the hue of the page behind it. */
    fun bottomBar(dark: Boolean) = if (dark) {
        GlassMaterial(px(25f), listOf(GlassLayer(c(0x80666666), BlendMode.Overlay), GlassLayer(c(0xB3262626), BlendMode.Luminosity)))
    } else {
        GlassMaterial(px(25f), listOf(GlassLayer(c(0xB3666666), BlendMode.ColorDodge), GlassLayer(c(0x99F5F5F5), BlendMode.Luminosity)))
    }

    /** Top bar, toolbar buttons and the search bar (which uses it without blur). Over black it is #1E1E1E (dark). */
    fun topBar(dark: Boolean, blur: Boolean = true) = if (dark) {
        GlassMaterial(if (blur) px(20f) else 0.dp, listOf(GlassLayer(c(0xCC262626), BlendMode.SrcOver), GlassLayer(c(0xB37F7F7F), BlendMode.Overlay)))
    } else {
        GlassMaterial(if (blur) px(20f) else 0.dp, listOf(GlassLayer(c(0x66333333), BlendMode.ColorDodge), GlassLayer(c(0x99F5F5F5), BlendMode.Luminosity)))
    }

    /** Search bar: the top bar colours without blur (light uses slightly stronger layers). */
    fun searchBar(dark: Boolean) = if (dark) {
        topBar(true, blur = false)
    } else {
        GlassMaterial(0.dp, listOf(GlassLayer(c(0xD6333333), BlendMode.ColorDodge), GlassLayer(c(0x66F5F5F5), BlendMode.Luminosity)))
    }

    /** Secondary button on top of blurred content: a plain white veil. */
    fun secondaryButton(dark: Boolean) = if (dark) {
        GlassMaterial(px(25f), listOf(GlassLayer(c(0x33FFFFFF), BlendMode.SrcOver), GlassLayer(c(0x00FFFFFF), BlendMode.SrcOver)))
    } else {
        GlassMaterial(px(25f), listOf(GlassLayer(c(0xCCFFFFFF), BlendMode.SrcOver), GlassLayer(c(0x00FFFFFF), BlendMode.SrcOver)))
    }

    fun transparentButton() =
        GlassMaterial(px(25f), listOf(GlassLayer(c(0x3F333333), BlendMode.Overlay), GlassLayer(c(0x3FFFFFFF), BlendMode.Luminosity)))

    fun segmentButton(dark: Boolean) = if (dark) {
        GlassMaterial(px(25f), listOf(GlassLayer(c(0x7F7F7F7F), BlendMode.Overlay), GlassLayer(c(0x99262626), BlendMode.Luminosity)))
    } else {
        GlassMaterial(px(25f), listOf(GlassLayer(c(0x7F333333), BlendMode.ColorDodge), GlassLayer(c(0x99EFEFEF), BlendMode.Luminosity)))
    }

    fun chipTab(dark: Boolean, selected: Boolean) = when {
        dark && selected -> GlassMaterial(px(40f), listOf(GlassLayer(c(0xA6505050), BlendMode.Luminosity), GlassLayer(c(0x4CA7A7A7), BlendMode.Overlay)))
        dark -> GlassMaterial(px(40f), listOf(GlassLayer(c(0x33B2B2B2), BlendMode.Luminosity), GlassLayer(c(0x33303030), BlendMode.Overlay)))
        selected -> GlassMaterial(px(40f), listOf(GlassLayer(c(0x66A3A1A1), BlendMode.ColorDodge), GlassLayer(c(0x99F5F5F5), BlendMode.Luminosity)))
        else -> GlassMaterial(px(40f), listOf(GlassLayer(c(0x144E4E4E), BlendMode.ColorDodge), GlassLayer(c(0x14606060), BlendMode.Luminosity)))
    }

    /**
     * Popup menu (system blur radius 160, blend mode "glow overlay"): the mix colour is blended with Luminosity, so the
     * colours behind the popup stay vivid, then the second colour is laid over it with Overlay.
     */
    fun popup(dark: Boolean) = if (dark) {
        GlassMaterial(px(160f / 6f), listOf(GlassLayer(c(0x99262626), BlendMode.Luminosity), GlassLayer(c(0x99262626), BlendMode.Overlay)), sampleOutside = true)
    } else {
        GlassMaterial(px(160f / 6f), listOf(GlassLayer(c(0x99CDCDCD), BlendMode.Luminosity), GlassLayer(c(0xBFEEEEEE), BlendMode.ColorDodge)), sampleOutside = true)
    }

    /** Dialog (system blur radius 200): plain mix colour, then Overlay (dark) / ColorDodge (light). */
    fun dialog(dark: Boolean) = if (dark) {
        GlassMaterial(px(200f / 6f), listOf(GlassLayer(c(0x8C777777), BlendMode.SrcOver), GlassLayer(c(0xB2292929), BlendMode.Overlay)), sampleOutside = true)
    } else {
        GlassMaterial(px(200f / 6f), listOf(GlassLayer(c(0xDC909090), BlendMode.SrcOver), GlassLayer(c(0x509F9F9F), BlendMode.ColorDodge)), sampleOutside = true)
    }

    /** Bottom sheet (system blur radius 400, the strongest blur in the system). */
    fun bottomSheet(dark: Boolean) = if (dark) {
        GlassMaterial(px(400f / 6f), listOf(GlassLayer(c(0xA4515151), BlendMode.SrcOver), GlassLayer(c(0xB30F0F0F), BlendMode.Overlay)), sampleOutside = true)
    } else {
        GlassMaterial(px(400f / 6f), listOf(GlassLayer(c(0xDCC8C8C8), BlendMode.SrcOver), GlassLayer(c(0x376E6E6E), BlendMode.ColorDodge)), sampleOutside = true)
    }
}

internal val ApiForRenderEffect: Boolean get() = Build.VERSION.SDK_INT >= 31
