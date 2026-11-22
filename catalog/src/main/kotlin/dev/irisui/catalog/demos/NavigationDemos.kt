package dev.irisui.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irisui.buttons.IrisIconButton
import dev.irisui.buttons.IrisPrimaryButton
import dev.irisui.buttons.IrisRoundPlusButton
import dev.irisui.content.IrisCard
import dev.irisui.content.IrisIconTile
import dev.irisui.content.IrisListItem
import dev.irisui.content.IrisSectionHeader
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.navigation.IrisBackButton
import dev.irisui.navigation.IrisBottomNavPill
import dev.irisui.navigation.IrisLargeTitleScaffold
import dev.irisui.navigation.IrisScaffold
import dev.irisui.navigation.IrisSegmentedTabs
import dev.irisui.navigation.IrisTopBar
import dev.irisui.navigation.NavItem
import dev.irisui.navigation.titleScrollOffset
import dev.irisui.popups.IrisMenuDivider
import dev.irisui.popups.IrisMenuItem
import dev.irisui.popups.IrisPopupMenuBox

private val tileColors = listOf(
    Color(0xFF1A8CFF), Color(0xFFFF7A18), Color(0xFF34C759), Color(0xFFAF52DE), Color(0xFFFF375F), Color(0xFF30B0C7),
)

/** Rows with colored tiles: plenty of contrast for the glass surfaces to blur. */
private fun androidx.compose.foundation.lazy.LazyListScope.sampleRows(count: Int, prefix: String) {
    items(count) { index ->
        IrisCard(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            contentPadding = PaddingValues(0.dp),
        ) {
            IrisListItem(
                headline = "$prefix ${index + 1}",
                supporting = "Scroll to watch the glass blur the rows beneath it",
                leading = { IrisIconTile(IrisIcons.Star, tileColors[index % tileColors.size]) },
                showChevron = true,
            )
        }
    }
}

@Composable
fun BottomNavPillDemo(onBack: () -> Unit) {
    val items = remember {
        listOf(
            NavItem("Home", IrisIcons.Home, IrisIcons.HomeFilled),
            NavItem("Explore", IrisIcons.Search),
            NavItem("Favorites", IrisIcons.Heart, IrisIcons.HeartFilled),
            NavItem("Profile", IrisIcons.Person, IrisIcons.PersonFilled),
        )
    }
    var selected by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    IrisLargeTitleScaffold(
        title = "Bottom nav pill",
        subtitle = "Tap, or slide a finger across the pill",
        scrollOffset = listState.titleScrollOffset(),
        navigationIcon = { IrisBackButton(onBack) },
        bottomBar = { IrisBottomNavPill(items, selected, { selected = it }) },
        floatingActionButton = { IrisRoundPlusButton(onClick = {}) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item { LargeTitle() }
            item { IrisSectionHeader("Selected: ${items[selected].label}") }
            sampleRows(24, items[selected].label)
        }
    }
}

@Composable
fun TopBarDemo(onBack: () -> Unit) {
    val listState = rememberLazyListState()
    var menuOpen by remember { mutableStateOf(false) }
    var sortByEdited by remember { mutableStateOf(true) }
    IrisScaffold(
        topBar = {
            IrisTopBar(
                title = "Top bar",
                navigationIcon = { IrisBackButton(onBack) },
                actions = {
                    IrisIconButton(IrisIcons.Search, onClick = {}, contentDescription = "Search")
                    IrisPopupMenuBox(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        menu = {
                            IrisMenuItem("Edit", onClick = {})
                            IrisMenuItem("Grid view", onClick = {})
                            IrisMenuDivider()
                            IrisMenuItem("Sort by time edited", onClick = { sortByEdited = true }, checked = sortByEdited)
                            IrisMenuItem("Sort by time created", onClick = { sortByEdited = false }, checked = !sortByEdited)
                        },
                    ) {
                        IrisIconButton(IrisIcons.More, onClick = { menuOpen = true }, contentDescription = "More")
                    }
                },
            )
        },
        topBlurAlpha = { (listState.titleScrollOffset()() / 24f).coerceIn(0f, 1f) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item {
                IrisText(
                    "Compact top bar with glass icon buttons. The content scrolls underneath and the progressive blur fades in.",
                    Modifier.padding(20.dp),
                    style = IrisTheme.typography.subtitle,
                    color = IrisTheme.colors.contentSecondary,
                )
            }
            sampleRows(30, "Row")
        }
    }
}

@Composable
fun LargeTitleDemo(onBack: () -> Unit) {
    val listState = rememberLazyListState()
    IrisLargeTitleScaffold(
        title = "Notes",
        subtitle = "9 notes",
        scrollOffset = listState.titleScrollOffset(),
        navigationIcon = { IrisBackButton(onBack) },
        actions = { IrisIconButton(IrisIcons.Search, onClick = {}, contentDescription = "Search") },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item { LargeTitle() }
            sampleRows(30, "Note")
        }
    }
}

@Composable
fun SegmentedTabsDemo(onBack: () -> Unit) {
    var two by remember { mutableIntStateOf(0) }
    var three by remember { mutableIntStateOf(1) }
    var four by remember { mutableIntStateOf(0) }
    DemoPage("Segmented tabs", onBack, subtitle = "Slide the thumb or tap a segment") {
        demoSection("Two segments") {
            IrisSegmentedTabs(listOf("Notes", "To-dos"), two, { two = it })
        }
        demoSection("Three segments") {
            IrisSegmentedTabs(listOf("Day", "Week", "Month"), three, { three = it })
            Caption("Selected: ${listOf("Day", "Week", "Month")[three]}")
        }
        demoSection("Four segments, neutral thumb") {
            IrisSegmentedTabs(
                listOf("All", "Photos", "Videos", "Audio"),
                four,
                { four = it },
                thumbColor = IrisTheme.colors.content.copy(alpha = 0.18f),
                selectedContentColor = IrisTheme.colors.content,
            )
        }
    }
}

@Composable
fun PageTransitionsDemo(onBack: () -> Unit, onPushDetail: () -> Unit) {
    DemoPage("Page transitions", onBack, subtitle = "Every screen in this catalog uses them") {
        demoCard("How it moves") {
            IrisText("Push: the new page slides in from the right with a long soft landing while the page underneath drifts left by about a quarter of the width and dims.")
            Spacer(Modifier.height(10.dp))
            Caption("Pop plays the same motion in reverse. The system back gesture pops the stack.")
        }
        demoSection {
            IrisPrimaryButton("Push another page", onPushDetail, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun DetailPage(depth: Int, onBack: () -> Unit, onPushDetail: () -> Unit) {
    DemoPage("Page $depth", onBack, subtitle = "Stack depth $depth") {
        demoSection {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                IrisText("Pushed page", style = IrisTheme.typography.title)
            }
            IrisPrimaryButton("Push deeper", onPushDetail, Modifier.fillMaxWidth())
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Caption("Swipe back or press the back button to see the reverse transition.")
            }
        }
    }
}
