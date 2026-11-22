package dev.irisui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.GlassFade
import dev.irisui.effects.ProgressiveBlur
import dev.irisui.effects.TouchDragAxes
import dev.irisui.effects.LocalBackdrop
import dev.irisui.effects.backdropSource
import dev.irisui.effects.glass
import dev.irisui.effects.rememberBackdrop
import dev.irisui.effects.touchMotion

/**
 * Page scaffold. The content fills the whole screen and scrolls under a progressive blur at the
 * top and a floating glass pill / round button at the bottom. Everything in the slots that uses
 * glass automatically blurs the page content through [LocalBackdrop].
 *
 * The content lambda receives padding that already accounts for the bars, so lists should use it
 * as `contentPadding`.
 */
@Composable
fun IrisScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    topBlurAlpha: () -> Float = { 1f },
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    /** Put the round button centered above the pill (Clock style) instead of beside it (Notes style). */
    fabAbovePill: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = IrisTheme.colors
    val backdrop = rememberBackdrop()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val topBarHeight = if (topBar != null) IrisTopBarDefaults.Height else 0.dp
    val hasBottom = bottomBar != null || floatingActionButton != null
    val fabAbove = fabAbovePill && floatingActionButton != null && bottomBar != null
    val bottomArea = if (hasBottom) 56.dp + 16.dp + 16.dp + (if (fabAbove) 64.dp else 0.dp) else 0.dp

    Box(modifier.fillMaxSize().background(colors.background)) {
        // The background lives inside the source so the blurred copy is opaque and fully covers
        // the sharp content underneath the glass.
        Box(Modifier.fillMaxSize().backdropSource(backdrop).background(colors.background)) {
            content(PaddingValues(top = statusTop + topBarHeight, bottom = navBottom + bottomArea))
        }

        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            // Progressive blur under the status bar and top bar (radius and page colour fade out towards the content).
            ProgressiveBlur(
                Modifier
                    .fillMaxWidth()
                    .height(statusTop + topBarHeight)
                    .graphicsLayer { alpha = topBlurAlpha() },
            )
            if (topBar != null) {
                Column(Modifier.fillMaxWidth().statusBarsPadding()) { topBar() }
            }

            if (hasBottom) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                ) {
                    if (fabAbove) {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            floatingActionButton?.invoke()
                            bottomBar?.invoke()
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)) { bottomBar?.invoke() }
                            floatingActionButton?.invoke()
                        }
                    }
                }
            }
        }
    }
}

/** Gives pages a [LargeTitle] to place as the first item of their scrolling content. */
@Stable
class LargeTitleScope internal constructor(
    private val title: String,
    private val subtitle: String?,
    private val scrollOffset: () -> Float = { 0f },
) {
    /**
     * The expanded title: 32 sp, 16 dp from the screen edge, with an optional 14 sp subtitle. Metrics are those of the
     * Clock and Files pages (title ink starts 25 px below the toolbar, first content 60 px under the subtitle or 80 px
     * under the title at 480 dpi).
     */
    @Composable
    fun LargeTitle(modifier: Modifier = Modifier) {
        val unit = with(LocalDensity.current) { 38.dp.toPx() }
        Column(
            modifier
                .graphicsLayer {
                    // Measured on the Settings home page: while the page scrolls the title shrinks by about 16 % and
                    // loses about 47 % of its brightness per 38 dp, fully gone after roughly twice that.
                    val p = (scrollOffset() / unit).coerceAtLeast(0f)
                    val s = (1f - 0.16f * p).coerceIn(0.6f, 1f)
                    scaleX = s
                    scaleY = s
                    alpha = (1f - 0.47f * p).coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0.04f, 0.3f)
                }
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 1.33.dp,
                    bottom = if (subtitle != null) 20.67.dp else 26.67.dp,
                ),
        ) {
            // The title and its subtitle follow a finger that drags them sideways; up and down drags still scroll the page.
            val titleDrag = Modifier.touchMotion(
                shape = IrisTheme.shapes.small,
                dragAxes = TouchDragAxes.Horizontal,
                claimAxes = TouchDragAxes.Horizontal,
                highlightAlpha = 0f,
            )
            IrisText(title, titleDrag, style = IrisTheme.typography.largeTitle)
            if (subtitle != null) {
                IrisText(
                    subtitle,
                    Modifier.padding(top = 6.dp).touchMotion(
                        shape = IrisTheme.shapes.small,
                        dragAxes = TouchDragAxes.Horizontal,
                        claimAxes = TouchDragAxes.Horizontal,
                        highlightAlpha = 0f,
                    ),
                    style = IrisTheme.typography.subtitleSmall,
                    color = IrisTheme.colors.contentSecondary,
                )
            }
        }
    }
}

/**
 * Collapsing large title. Place `LargeTitle()` as the first item of the scrolling content and
 * hand over how far the content is scrolled: the small title fades into the top bar once the large
 * one has scrolled away, and the blur under the bar fades in as soon as content passes below it.
 */
@Composable
fun IrisLargeTitleScaffold(
    title: String,
    scrollOffset: () -> Float,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    fabAbovePill: Boolean = false,
    content: @Composable LargeTitleScope.(PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    val blurStart = with(density) { 6.dp.toPx() }
    val titleStart = with(density) { 40.dp.toPx() }
    val titleEnd = with(density) { 72.dp.toPx() }
    val currentOffset by rememberUpdatedState(scrollOffset)
    val scope = remember(title, subtitle) { LargeTitleScope(title, subtitle) { currentOffset() } }
    IrisScaffold(
        modifier = modifier,
        topBar = {
            IrisTopBar(
                title = title,
                titleAlpha = { ((scrollOffset() - titleStart) / (titleEnd - titleStart)).coerceIn(0f, 1f) },
                navigationIcon = navigationIcon,
                actions = actions,
            )
        },
        topBlurAlpha = { (scrollOffset() / blurStart).coerceIn(0f, 1f) },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        fabAbovePill = fabAbovePill,
        content = { padding -> scope.content(padding) },
    )
}

/** Scroll offset (px) for a lazy list, for [IrisLargeTitleScaffold]. */
fun LazyListState.titleScrollOffset(): () -> Float = {
    if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset.toFloat() else Float.MAX_VALUE
}
