package dev.irisui.inputs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.IrisHaptic
import dev.irisui.effects.LocalColorOSHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Wheel for numbers (hours, minutes, durations), three rows high: the centered value in the label color at 22 sp, the
 * neighbours at 17 sp and 30 % brightness, as in the Clock sheets. It snaps to a row when released and every step ticks
 * the haptic motor with the picker constant of the stock apps. With [wrap] the wheel turns around at the ends.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IrisNumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    wrap: Boolean = true,
    format: (Int) -> String = { "%02d".format(it) },
    centerSize: TextUnit = 22.sp,
    sideSize: TextUnit = 17.sp,
    itemHeight: Dp = 45.dp,
    columnWidth: Dp = 76.dp,
) {
    val colors = IrisTheme.colors
    val haptics = LocalColorOSHaptics.current
    val size = range.last - range.first + 1
    val repeats = if (wrap) 400 else 1
    val total = size * repeats
    val startIndex = if (wrap) (repeats / 2) * size + (value - range.first) else value - range.first
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val currentChange by rememberUpdatedState(onValueChange)
    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val mid = info.viewportSize.height / 2f - info.beforeContentPadding
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - mid) }?.index ?: startIndex
        }
    }

    // Report the centered value and tick once per step.
    LaunchedEffect(state) {
        snapshotFlow { centered }.distinctUntilChanged().collect { index ->
            val v = range.first + ((index % size) + size) % size
            if (v != value) haptics.perform(IrisHaptic.Tick)
            currentChange(v)
        }
    }
    // Follow the value when it is changed from outside.
    LaunchedEffect(value) {
        val current = range.first + ((centered % size) + size) % size
        if (current != value) {
            val base = if (wrap) (centered / size) * size else 0
            state.scrollToItem(base + (value - range.first))
        }
    }

    // The wheel snaps to its rows and never bounces: the rows scale by their distance from the centre, which a translated
    // list would throw off.
    Box(modifier.width(columnWidth).height(itemHeight * 3), contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            LazyColumn(
                state = state,
                flingBehavior = rememberSnapFlingBehavior(state),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(total) { index ->
                    val v = range.first + index % size
                    val distance by remember {
                        derivedStateOf {
                            val layout = state.layoutInfo
                            val item = layout.visibleItemsInfo.firstOrNull { it.index == index }
                            if (item == null) 2f else (abs(item.offset + item.size / 2f - (layout.viewportSize.height / 2f - layout.beforeContentPadding)) / item.size).coerceIn(0f, 2f)
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                        IrisText(
                            format(v),
                            Modifier.graphicsLayer {
                                val s = 1f - (1f - sideSize.value / centerSize.value) * distance.coerceAtMost(1f)
                                scaleX = s
                                scaleY = s
                                alpha = (1f - 0.67f * distance.coerceAtMost(1f)) / 0.9f
                            },
                            style = TextStyle(fontSize = centerSize, fontWeight = FontWeight(575)),
                            color = colors.content,
                        )
                    }
                }
            }
        }
    }
}
