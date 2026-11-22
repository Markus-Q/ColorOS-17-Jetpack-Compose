package dev.irisui.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A "portal" layer for dialogs, sheets and popup menus. Popups register their content here
 * and the host draws it above the page, so every popup can use the same blurred backdrop and
 * animate freely instead of living in a separate platform window.
 */
@Stable
class OverlayHost {
    internal class Entry(val content: MutableState<@Composable () -> Unit>)

    internal val entries = mutableStateListOf<Entry>()

    internal fun add(content: @Composable () -> Unit): Entry {
        val entry = Entry(mutableStateOf(content))
        entries.add(entry)
        return entry
    }

    internal fun remove(entry: Entry) {
        entries.remove(entry)
    }
}

/**
 * The [OverlayHost] that popups register with; `null` outside a popup host, where popups are composed in place instead.
 */
val LocalOverlayHost = staticCompositionLocalOf<OverlayHost?> { null }

/** Draws everything that popups registered with [host]. Place it above the page content. */
@Composable
fun OverlayHostLayer(host: OverlayHost) {
    host.entries.forEach { entry ->
        key(entry) { entry.content.value() }
    }
}

/**
 * Renders [content] in the nearest [OverlayHost]; falls back to inline composition when there is none.
 * The content stays alive as long as this call stays in the composition, so popups keep their
 * own exit animation by delaying removal until it finished.
 */
@Composable
fun OverlayPortal(content: @Composable () -> Unit) {
    val host = LocalOverlayHost.current
    if (host == null) {
        content()
        return
    }
    val latest by rememberUpdatedState(content)
    DisposableEffect(host) {
        val entry = host.add { latest() }
        onDispose { host.remove(entry) }
    }
}
