package dev.irisui.catalog.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irisui.content.IrisCard
import dev.irisui.content.IrisSectionHeader
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.navigation.IrisBackButton
import dev.irisui.navigation.IrisLargeTitleScaffold
import dev.irisui.navigation.titleScrollOffset

/** Standard demo page: back button, collapsing large title and a lazy list of demo sections. */
@Composable
fun DemoPage(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    IrisLargeTitleScaffold(
        title = title,
        subtitle = subtitle,
        scrollOffset = listState.titleScrollOffset(),
        navigationIcon = { IrisBackButton(onBack) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item { LargeTitle() }
            content()
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** A titled block of demo content with page margins. */
fun LazyListScope.demoSection(header: String? = null, content: @Composable ColumnScope.() -> Unit) {
    item {
        Column(Modifier.fillMaxWidth()) {
            if (header != null) IrisSectionHeader(header)
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = if (header == null) 6.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/** Same as [demoSection] but the content sits in a card. */
fun LazyListScope.demoCard(header: String? = null, content: @Composable ColumnScope.() -> Unit) {
    demoSection(header) {
        IrisCard(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    IrisText(
        text,
        modifier,
        style = IrisTheme.typography.bodySmall,
        color = IrisTheme.colors.contentSecondary,
    )
}

/** Colorful backdrop so blur and glass have something to show. */
@Composable
fun DemoWallpaper(modifier: Modifier = Modifier) {
    Canvas(modifier.clipToBounds()) {
        drawRect(Brush.linearGradient(listOf(Color(0xFF14213D), Color(0xFF3A1C71), Color(0xFF0B3954)), Offset.Zero, Offset(size.width, size.height)))
        fun blob(color: Color, cx: Float, cy: Float, r: Float) = drawCircle(
            Brush.radialGradient(listOf(color, Color.Transparent), Offset(cx * size.width, cy * size.height), r * size.width),
            r * size.width,
            Offset(cx * size.width, cy * size.height),
        )
        blob(Color(0xFFFF7A18), 0.2f, 0.18f, 0.55f)
        blob(Color(0xFFFF2E93), 0.85f, 0.4f, 0.5f)
        blob(Color(0xFF1A8CFF), 0.3f, 0.7f, 0.6f)
        blob(Color(0xFF7CFFB2), 0.9f, 0.9f, 0.4f)
    }
}
