package dev.irisui.effects

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisTheme
import kotlin.math.ceil

/**
 * The content that glass surfaces blur. Mark the page content with [backdropSource] and put the
 * glass surfaces next to it (as siblings that are drawn later), never inside it.
 */
@Stable
class Backdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var sourcePosition: Offset by mutableStateOf(Offset.Zero)

    /** Bumped whenever the source was drawn, so glass nodes that live in their own layer redraw. */
    internal var version: Int by mutableIntStateOf(0)
    private var counter = 0

    internal fun markDirty() {
        counter += 1
        version = counter
    }
}

/**
 * Creates and remembers a [Backdrop]: mark the content to blur with [backdropSource] and draw glass surfaces above it with [glass].
 * `IrisApp` and the scaffolds do this for you.
 */
@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Backdrop(layer) }
}

/** Backdrop that glass surfaces use when none is passed explicitly. Scaffolds provide it. */
val LocalBackdrop = compositionLocalOf<Backdrop?> { null }

/** Records this content into [backdrop] (and still draws it normally). */
fun Modifier.backdropSource(backdrop: Backdrop): Modifier = this.then(BackdropSourceElement(backdrop))

private data class BackdropSourceElement(val backdrop: Backdrop) : ModifierNodeElement<BackdropSourceNode>() {
    override fun create() = BackdropSourceNode(backdrop)
    override fun update(node: BackdropSourceNode) {
        node.backdrop = backdrop
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "backdropSource"
    }
}

private class BackdropSourceNode(var backdrop: Backdrop) :
    Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        backdrop.sourcePosition = coordinates.positionInRoot()
    }

    override fun ContentDrawScope.draw() {
        // Draw the content normally and record a second copy into the backdrop layer. The layer is
        // only ever drawn by glass surfaces; drawing it here as well makes it undrawable elsewhere.
        drawContent()
        // DrawScope.record retargets this scope's canvas to the layer, so drawContent() lands in it.
        backdrop.layer.record(IntSize(size.width.toInt(), size.height.toInt())) {
            this@draw.drawContent()
        }
        backdrop.markDirty()
    }
}

/** Direction in which a surface fades out (progressive blur for top and bottom bars). */
enum class GlassFade { TowardsBottom, TowardsTop }

/**
 * Fades the content of this element out along the vertical axis. Put it before [glass] in the
 * modifier chain to get a progressive blur: full strength at the bar, gone at the far edge.
 */
fun Modifier.verticalFadeMask(fade: GlassFade): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val mask = when (fade) {
            GlassFade.TowardsBottom -> Brush.verticalGradient(
                0f to Color.Black, 0.62f to Color.Black.copy(alpha = 0.92f), 1f to Color.Transparent,
            )
            GlassFade.TowardsTop -> Brush.verticalGradient(
                0f to Color.Transparent, 0.38f to Color.Black.copy(alpha = 0.92f), 1f to Color.Black,
            )
        }
        drawRect(brush = mask, blendMode = BlendMode.DstIn)
    }

/**
 * Everything that makes up a glass surface: the [material] (blur plus colour layers, see [GlassMaterial]) and the edge
 * decoration (rim light and the soft light along the inner bottom edge). Without a [material] the older recipe applies:
 * a plain Gaussian [blur] covered by a flat [tint].
 */
@Immutable
data class GlassStyle(
    val blur: Dp = 24.dp,
    /** Flat colour drawn over the blurred backdrop, only used when there is no [material]. */
    val tint: Color = Color.Transparent,
    val rimHighlight: Color = Color.Transparent,
    val rimShade: Color = Color.Transparent,
    val rimWidth: Dp = 1.dp,
    /**
     * Soft light that spills inwards from the edge of the glass (white with a small alpha). It is strongest at the
     * bottom edge ([edgeGlowBottom]) and weaker at the top ([edgeGlowTop]); the two colours are blended in between and
     * the light fades out with a Gaussian of [edgeGlowRadius].
     */
    val edgeGlowTop: Color = Color.Transparent,
    val edgeGlowBottom: Color = Color.Transparent,
    val edgeGlowRadius: Dp = 8.dp,
    /** Extra light along the bottom edge that reaches further inwards than the rest ([bottomGlowRadius]). */
    val bottomGlow: Color = Color.Transparent,
    val bottomGlowRadius: Dp = 12.dp,
    /**
     * Light that spills out below the glass (the pill and the round toolbar buttons have it): the outline of the shape,
     * moved down by [outerGlowOffset] and blurred with [outerGlowRadius], drawn behind the glass.
     */
    val outerGlow: Color = Color.Transparent,
    val outerGlowOffset: Dp = 2.dp,
    val outerGlowRadius: Dp = 1.2.dp,
    val material: GlassMaterial? = null,
    /** What the page behind the glass looks like where no real backdrop can be blurred (no backdrop, API < 31). */
    val fallbackBackdrop: Color = Color.Black,
)

/**
 * Ready-made [GlassStyle]s for the glass surfaces of ColorOS 17 (bottom pill, toolbar buttons, popup menu, dialog, bottom sheet, search bar)
 * and builders for your own.
 */
object GlassDefaults {
    private fun surfaceRim(dark: Boolean, highlight: Color, shade: Color) = GlassStyle(
        rimHighlight = highlight,
        rimShade = shade,
        rimWidth = 0.7.dp,
        // Measured on the Clock pill over black: 2 levels of light along the top and the sides (fading over ~10 dp) and about
        // 12 levels along the bottom edge that reach ~17 dp inwards.
        edgeGlowTop = Color.White.copy(alpha = if (dark) 0.02f else 0.12f),
        edgeGlowBottom = Color.White.copy(alpha = if (dark) 0.02f else 0.12f),
        edgeGlowRadius = 8.dp,
        bottomGlow = Color.White.copy(alpha = if (dark) 0.10f else 0.3f),
        bottomGlowRadius = 13.3.dp,
        // Measured under the Clock pill over black: 27 levels at the edge, falling to 0 within 14 px, only below the straight part.
        outerGlow = Color.White.copy(alpha = if (dark) 0.112f else 0.3f),
        outerGlowOffset = 2.1.dp,
        outerGlowRadius = 1.4.dp,
    )

    /** Floating surfaces with a flat tint: round glass buttons in custom layouts, demo panels. */
    @Composable
    fun surface(blur: Dp = 22.dp, tintAlpha: Float = 0.78f): GlassStyle {
        val c = IrisTheme.colors
        return surfaceRim(c.isDark, c.rimHighlight, c.rimShade).copy(blur = blur, tint = c.glass.copy(alpha = tintAlpha))
    }

    /** Any glass surface built from a [GlassMaterial], with the rim of the floating surfaces. */
    @Composable
    fun surface(material: GlassMaterial): GlassStyle {
        val c = IrisTheme.colors
        return surfaceRim(c.isDark, c.rimHighlight, c.rimShade).copy(material = material, fallbackBackdrop = c.background)
    }

    /** The floating bottom navigation pill. */
    @Composable
    fun bottomBar(): GlassStyle = surface(GlassMaterials.bottomBar(IrisTheme.colors.isDark))

    /** Round glass buttons in toolbars. Their rim is fainter than the pill's. */
    @Composable
    fun toolbarButton(): GlassStyle {
        val c = IrisTheme.colors
        return surface(GlassMaterials.topBar(c.isDark)).copy(
            rimHighlight = c.content.copy(alpha = 0.05f),
            rimShade = c.content.copy(alpha = 0.045f),
            // The round toolbar buttons: 20 levels under the disc, falling to 0 within 16 px.
            outerGlow = Color.White.copy(alpha = if (c.isDark) 0.078f else 0.25f),
            outerGlowOffset = 3.dp,
            outerGlowRadius = 1.83.dp,
        )
    }

    /** Popup menus: a brighter rim, strongest at the bottom edge, and no light along the inner edge. */
    @Composable
    fun popup(): GlassStyle {
        val c = IrisTheme.colors
        return GlassStyle(
            material = GlassMaterials.popup(c.isDark),
            fallbackBackdrop = c.background,
            rimHighlight = Color.White.copy(alpha = if (c.isDark) 0.24f else 0.5f),
            rimShade = Color.White.copy(alpha = if (c.isDark) 0.31f else 0.5f),
            rimWidth = 1.dp,
            // Measured on the Clock menu over a black page: about 5 levels of light along the top and the sides that
            // fades out over ~13 dp, plus another 6 levels along the bottom edge that reach ~17 dp inwards.
            edgeGlowTop = Color.White.copy(alpha = 0.042f),
            edgeGlowBottom = Color.White.copy(alpha = 0.052f),
            edgeGlowRadius = 8.dp,
            bottomGlow = Color.White.copy(alpha = 0.058f),
            bottomGlowRadius = 12.dp,
        )
    }

    /** Dialog panel (strong blur, translucent colour layers). */
    @Composable
    fun dialog(): GlassStyle = surface(GlassMaterials.dialog(IrisTheme.colors.isDark)).copy(outerGlow = Color.Transparent)

    /** Bottom sheet panel: the strongest blur of the system. Light along the top edge as measured on the Clock sheet. */
    @Composable
    fun bottomSheet(): GlassStyle {
        val c = IrisTheme.colors
        return GlassStyle(
            material = GlassMaterials.bottomSheet(c.isDark),
            fallbackBackdrop = c.background,
            rimHighlight = Color.White.copy(alpha = if (c.isDark) 0.11f else 0.3f),
            rimShade = Color.Transparent,
            rimWidth = 0.7.dp,
            edgeGlowTop = Color.White.copy(alpha = 0.047f),
            edgeGlowBottom = Color.White.copy(alpha = 0.047f),
            edgeGlowRadius = 8.dp,
        )
    }

    /** Search field: the top bar colours without any blur. */
    @Composable
    fun searchBar(): GlassStyle {
        val c = IrisTheme.colors
        return GlassStyle(material = GlassMaterials.searchBar(c.isDark), fallbackBackdrop = c.background, rimWidth = 0.dp)
    }
}

/**
 * Turns this element into a glass surface: it draws a blurred copy of [backdrop] clipped to
 * [shape], runs it through the colour layers of the material and adds a light rim. Without a backdrop (or below
 * API 31) the material is drawn as the flat colour it has over [GlassStyle.fallbackBackdrop].
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    style: GlassStyle = GlassDefaults.bottomBar(),
    backdrop: Backdrop? = LocalBackdrop.current,
): Modifier = this.then(GlassElement(backdrop, shape, style))

private data class GlassElement(
    val backdrop: Backdrop?,
    val shape: Shape,
    val style: GlassStyle,
) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(backdrop, shape, style)
    override fun update(node: GlassNode) {
        node.backdrop = backdrop
        node.shape = shape
        node.style = style
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "glass"
    }
}

/**
 * Light that spills inwards from the edge of a glass shape: the area outside the shape is filled with a vertical
 * gradient, blurred with a Gaussian and clipped to the shape (a half plane blurred this way fades out like
 * `1 - Φ(d / sigma)`, which is the profile measured on the real popups). The layer is only recorded again when the
 * size, shape or colours change.
 */
private class EdgeLight {
    private var layer: GraphicsLayer? = null
    private var lastSigma = -1f
    private var lastSize = IntSize.Zero
    private var lastKey: Any? = null
    private var lastShape: Shape? = null

    fun attach(context: GraphicsContext) {
        layer = context.createGraphicsLayer()
    }

    fun detach(context: GraphicsContext) {
        layer?.let { context.releaseGraphicsLayer(it) }
        layer = null
        lastSigma = -1f
        lastKey = null
    }

    /** [colors] are the gradient colours from the top to the bottom edge, [positions] where they sit (0..1). */
    fun draw(scope: ContentDrawScope, path: Path, shape: Shape, sigmaPx: Float, colors: List<Color>, positions: List<Float>) {
        val layer = layer ?: return
        with(scope) {
            val w = ceil(this.size.width).toInt()
            val h = ceil(this.size.height).toInt()
            if (w <= 0 || h <= 0 || sigmaPx <= 0f) return
            if (Build.VERSION.SDK_INT < 31) {
                // No blur effects before API 31: a plain gradient along the bottom edge stands in for the light.
                val last = colors.last()
                clipPath(path) {
                    drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.6f to Color.Transparent, 1f to last.copy(alpha = last.alpha * 0.8f)))
                }
                return
            }
            val pad = ceil(sigmaPx * 3f).toInt() + 2
            val newSize = IntSize(w, h)
            val newKey = colors to positions
            if (sigmaPx != lastSigma || newSize != lastSize || newKey != lastKey || shape != lastShape) {
                if (sigmaPx != lastSigma) {
                    layer.renderEffect = androidx.compose.ui.graphics.BlurEffect(sigmaPx, sigmaPx, androidx.compose.ui.graphics.TileMode.Decal)
                }
                layer.record(IntSize(w + 2 * pad, h + 2 * pad)) {
                    translate(pad.toFloat(), pad.toFloat()) {
                        val outside = Path().apply {
                            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                            addRect(androidx.compose.ui.geometry.Rect(-pad * 2f, -pad * 2f, w + pad * 2f, h + pad * 2f))
                            addPath(path)
                        }
                        drawPath(
                            outside,
                            Brush.verticalGradient(
                                colorStops = positions.indices.map { positions[it] to colors[it] }.toTypedArray(),
                                startY = 0f,
                                endY = h.toFloat(),
                            ),
                        )
                    }
                }
                lastSigma = sigmaPx
                lastSize = newSize
                lastKey = newKey
                lastShape = shape
            }
            clipPath(path) {
                translate(-pad.toFloat(), -pad.toFloat()) { drawLayer(layer) }
            }
        }
    }
}

/**
 * Light below a glass shape: its outline moved down and blurred, drawn behind the glass (which covers the part that lies
 * under it). Only recorded again when something it depends on changes.
 */
private class OuterLight {
    private var layer: GraphicsLayer? = null
    private var lastKey: Any? = null
    private var lastSigma = -1f

    fun attach(context: GraphicsContext) {
        layer = context.createGraphicsLayer()
    }

    fun detach(context: GraphicsContext) {
        layer?.let { context.releaseGraphicsLayer(it) }
        layer = null
        lastKey = null
        lastSigma = -1f
    }

    fun draw(scope: ContentDrawScope, path: Path, shape: Shape, color: Color, offsetPx: Float, sigmaPx: Float) {
        val layer = layer ?: return
        with(scope) {
            val w = ceil(this.size.width).toInt()
            val h = ceil(this.size.height).toInt()
            if (w <= 0 || h <= 0) return
            if (Build.VERSION.SDK_INT < 31) {
                translate(top = offsetPx) { drawPath(path, color.copy(alpha = color.alpha * 0.6f)) }
                return
            }
            val pad = ceil(sigmaPx * 3f).toInt() + 2
            val extra = ceil(offsetPx).toInt()
            val key = listOf(IntSize(w, h), color, offsetPx, shape)
            if (key != lastKey || sigmaPx != lastSigma) {
                if (sigmaPx != lastSigma) {
                    layer.renderEffect = androidx.compose.ui.graphics.BlurEffect(sigmaPx, sigmaPx, androidx.compose.ui.graphics.TileMode.Decal)
                }
                layer.record(IntSize(w + 2 * pad, h + extra + 2 * pad)) {
                    translate(pad.toFloat(), pad + offsetPx) { drawPath(path, color) }
                }
                lastKey = key
                lastSigma = sigmaPx
            }
            translate(-pad.toFloat(), -pad.toFloat()) { drawLayer(layer) }
        }
    }
}

private class GlassNode(
    var backdrop: Backdrop?,
    var shape: Shape,
    var style: GlassStyle,
) : Modifier.Node(), DrawModifierNode, GlobalPositionAwareModifierNode {

    private var blurLayer: GraphicsLayer? = null
    private val edgeLight = EdgeLight()
    private val bottomLight = EdgeLight()
    private val outerLight = OuterLight()
    private var appliedKey = Long.MIN_VALUE
    private var position = Offset.Zero

    override fun onAttach() {
        val context = requireGraphicsContext()
        blurLayer = context.createGraphicsLayer()
        edgeLight.attach(context)
        bottomLight.attach(context)
        outerLight.attach(context)
    }

    override fun onDetach() {
        val context = requireGraphicsContext()
        blurLayer?.let { context.releaseGraphicsLayer(it) }
        edgeLight.detach(context)
        bottomLight.detach(context)
        outerLight.detach(context)
        blurLayer = null
        appliedKey = Long.MIN_VALUE
    }

    private fun ContentDrawScope.drawEdgeGlow(path: Path, st: GlassStyle) {
        val top = st.edgeGlowTop
        val bottom = st.edgeGlowBottom
        if (top.alpha > 0f || bottom.alpha > 0f) {
            // Mostly the top colour along the sides, the bottom colour only close to the bottom edge.
            val sides = androidx.compose.ui.graphics.lerp(top, bottom, 0.15f)
            edgeLight.draw(this, path, shape, st.edgeGlowRadius.toPx(), listOf(top, sides, bottom), listOf(0f, 0.55f, 1f))
        }
        if (st.bottomGlow.alpha > 0f) {
            val clear = st.bottomGlow.copy(alpha = 0f)
            bottomLight.draw(this, path, shape, st.bottomGlowRadius.toPx(), listOf(clear, clear, st.bottomGlow), listOf(0f, 0.6f, 1f))
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        val p = coordinates.positionInRoot()
        if (p != position) {
            position = p
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = outline.toPath()
        val st = style
        val material = st.material

        if (st.outerGlow.alpha > 0f) {
            outerLight.draw(this, path, shape, st.outerGlow, st.outerGlowOffset.toPx(), st.outerGlowRadius.toPx())
        }

        val b = backdrop
        val layer = blurLayer
        val wantsBlur = material != null || st.blur > 0.dp
        if (b != null && layer != null && Build.VERSION.SDK_INT >= 31 && wantsBlur) {
            // Reading the version registers a draw dependency on the source.
            @Suppress("UNUSED_VARIABLE") val version = b.version
            val effective = material ?: GlassMaterial(st.blur, emptyList())
            val blurPx = effective.blur.toPx()
            val key = effective.effectKey(requireDensity().density)
            if (key != appliedKey) {
                layer.renderEffect = effective.buildRenderEffect(blurPx)
                appliedKey = key
            }
            val w = ceil(size.width).toInt()
            val h = ceil(size.height).toInt()
            val sourceOrigin = position - b.sourcePosition
            // View level glass blurs exactly the content behind the view and mirrors at its edges. Window level glass
            // (popup, dialog, sheet) is blurred by the compositor, which also sees the content around the panel, so
            // the layer holds a margin of three sigmas of it.
            val pad = if (effective.sampleOutside) ceil(blurPx * 3f).toInt() else 0
            layer.record(IntSize(w + 2 * pad, h + 2 * pad)) {
                translate(left = pad - sourceOrigin.x, top = pad - sourceOrigin.y) {
                    drawLayer(b.layer)
                }
            }
            clipPath(path) {
                translate(left = -pad.toFloat(), top = -pad.toFloat()) {
                    drawLayer(layer)
                }
            }
        } else if (material != null) {
            drawOutline(outline, color = material.over(st.fallbackBackdrop))
        }

        if (st.tint != Color.Transparent) {
            drawOutline(outline, color = st.tint)
        }
        if (st.edgeGlowTop.alpha > 0f || st.edgeGlowBottom.alpha > 0f || st.bottomGlow.alpha > 0f) {
            drawEdgeGlow(path, st)
        }

        val rimW = st.rimWidth.toPx()
        if (rimW > 0f && (st.rimHighlight.alpha > 0f || st.rimShade.alpha > 0f)) {
            clipPath(path) {
                drawOutline(
                    outline,
                    brush = Brush.verticalGradient(
                        0f to st.rimHighlight,
                        0.3f to st.rimHighlight.copy(alpha = 0f),
                        0.7f to st.rimShade.copy(alpha = 0f),
                        1f to st.rimShade,
                    ),
                    style = Stroke(rimW * 2f),
                )
            }
        }

        drawContent()
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
}
