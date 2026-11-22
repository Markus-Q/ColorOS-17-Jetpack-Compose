package dev.irisui.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.irisui.buttons.IrisRoundPlusButton
import dev.irisui.buttons.IrisSecondaryButton
import dev.irisui.content.IrisCard
import dev.irisui.content.IrisGroupedList
import dev.irisui.content.IrisIconTile
import dev.irisui.content.IrisListItem
import dev.irisui.content.IrisSectionHeader
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.inputs.IrisCheckbox
import dev.irisui.inputs.IrisCircularProgress
import dev.irisui.inputs.IrisNumberPicker
import dev.irisui.inputs.IrisSlider
import dev.irisui.inputs.IrisSwitch
import dev.irisui.inputs.IrisTextField
import dev.irisui.navigation.IrisBackButton
import dev.irisui.navigation.IrisBottomNavPill
import dev.irisui.navigation.IrisChip
import dev.irisui.navigation.IrisChipRow
import dev.irisui.navigation.IrisLargeTitleScaffold
import dev.irisui.navigation.IrisScaffold
import dev.irisui.navigation.IrisSegmentedTabs
import dev.irisui.navigation.IrisTopBar
import dev.irisui.navigation.NavItem
import dev.irisui.navigation.titleScrollOffset
import dev.irisui.popups.IrisBottomSheet
import dev.irisui.popups.IrisMenuItem
import dev.irisui.popups.IrisPopupMenuBox
import kotlin.math.roundToInt

/*
 * Example screens: whole pages composed from the kit, with content of their own. They show how the pieces fit together:
 * grouped lists with every kind of row, a card with a progress ring, tabs, chips, pickers and a sheet.
 */

/** A settings page: grouped lists with a dropdown, a stepped slider, switches, value rows and chevrons. */
@Composable
fun PreferencesExample(onBack: () -> Unit) {
    var theme by remember { mutableStateOf("System") }
    var themeMenu by remember { mutableStateOf(false) }
    var textSize by remember { mutableFloatStateOf(1f) }
    var reduceMotion by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    var badges by remember { mutableStateOf(true) }
    DemoPage("Preferences", onBack, subtitle = "A settings page from grouped lists") {
        item { IrisSectionHeader("Appearance") }
        item {
            ExampleGroup {
                item {
                    // A dropdown preference: the choice opens as a popup centered on the screen and the row stays lit.
                    IrisPopupMenuBox(
                        expanded = themeMenu,
                        onDismissRequest = { themeMenu = false },
                        modifier = Modifier.fillMaxWidth(),
                        centerOnScreen = true,
                        menu = {
                            listOf("System", "Light", "Dark").forEach { choice ->
                                IrisMenuItem(choice, onClick = { theme = choice }, checked = choice == theme)
                            }
                        },
                    ) {
                        IrisListItem("Theme", value = theme, showDropdown = true, highlighted = themeMenu, onClick = { themeMenu = true })
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IrisText("Text size", Modifier.weight(1f), style = IrisTheme.typography.body)
                            IrisText(
                                listOf("Small", "Default", "Large")[textSize.roundToInt().coerceIn(0, 2)],
                                style = IrisTheme.typography.bodySmall,
                                color = IrisTheme.colors.contentSecondary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        IrisSlider(textSize, { textSize = it }, valueRange = 0f..2f, steps = 1)
                    }
                }
                item {
                    IrisListItem(
                        "Reduce motion",
                        supporting = "Shorter transitions and no bounce.",
                        trailing = { IrisSwitch(reduceMotion, { reduceMotion = it }) },
                        onClick = { reduceMotion = !reduceMotion },
                    )
                }
            }
        }
        item { IrisSectionHeader("Notifications") }
        item {
            ExampleGroup {
                item {
                    IrisListItem(
                        "Allow notifications",
                        trailing = { IrisSwitch(notifications, { notifications = it }) },
                        onClick = { notifications = !notifications },
                    )
                }
                item { IrisListItem("Quiet hours", value = "22:00 to 07:00", showChevron = true, onClick = {}) }
                item {
                    IrisListItem(
                        "Badges",
                        supporting = "Show a red dot on items with something new.",
                        enabled = notifications,
                        trailing = { IrisSwitch(badges && notifications, { badges = it }, enabled = notifications) },
                        onClick = { badges = !badges },
                    )
                }
            }
        }
        item { IrisSectionHeader("About") }
        item {
            ExampleGroup {
                item { IrisListItem("Version", supporting = "1.0.0") }
                item { IrisListItem("Open source licenses", showChevron = true, onClick = {}) }
            }
        }
    }
}

private data class Habit(val name: String, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color)

private val habits = listOf(
    Habit("Drink water", "8 glasses", IrisIcons.Sun, Color(0xFF1A8CFF)),
    Habit("Read", "20 pages", IrisIcons.Star, Color(0xFFAF52DE)),
    Habit("Stretch", "10 minutes", IrisIcons.Heart, Color(0xFFFF375F)),
    Habit("Walk", "6000 steps", IrisIcons.Person, Color(0xFF34C759)),
    Habit("Journal", "Three lines", IrisIcons.Bell, Color(0xFFFF7A18)),
)

/** A tracker page: card with a progress ring, segmented tabs, a checklist, a bottom pill and a sheet to add an item. */
@Composable
fun HabitsExample(onBack: () -> Unit) {
    val done = remember { mutableStateOf(setOf(0, 2, 3)) }
    var range by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val count = done.value.size
    IrisLargeTitleScaffold(
        title = "Habits",
        subtitle = "$count of ${habits.size} done today",
        scrollOffset = listState.titleScrollOffset(),
        navigationIcon = { IrisBackButton(onBack) },
        bottomBar = {
            IrisBottomNavPill(
                items = listOf(
                    NavItem("Today", IrisIcons.Home, IrisIcons.HomeFilled),
                    NavItem("Progress", IrisIcons.Grid, IrisIcons.GridFilled),
                    NavItem("Reminders", IrisIcons.Bell, IrisIcons.BellFilled),
                ),
                selectedIndex = tab,
                onSelectedChange = { tab = it },
            )
        },
        floatingActionButton = { IrisRoundPlusButton(onClick = { sheet = true }, contentDescription = "New habit") },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = padding) {
            item { LargeTitle() }
            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    IrisCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                IrisCircularProgress(count / habits.size.toFloat(), size = 72.dp, strokeWidth = 7.dp)
                                IrisText("${(count * 100 / habits.size)}%", style = IrisTheme.typography.label)
                            }
                            Column {
                                IrisText("Today", style = IrisTheme.typography.headline)
                                IrisText(
                                    if (count == habits.size) "All done, well played." else "${habits.size - count} to go.",
                                    style = IrisTheme.typography.bodySmall,
                                    color = IrisTheme.colors.contentSecondary,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    IrisSegmentedTabs(listOf("Day", "Week", "Month"), range, { range = it })
                }
            }
            item { IrisSectionHeader("Habits") }
            item {
                ExampleGroup {
                    habits.forEachIndexed { index, habit ->
                        item {
                            val checked = index in done.value
                            IrisListItem(
                                headline = habit.name,
                                supporting = habit.detail,
                                leading = { IrisIconTile(habit.icon, habit.color) },
                                trailing = { IrisCheckbox(checked, { done.value = if (it) done.value + index else done.value - index }) },
                                onClick = { done.value = if (checked) done.value - index else done.value + index },
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    NewHabitSheet(sheet) { sheet = false }
}

/** The sheet of [HabitsExample]: header buttons, a text field, a row of chips, a wheel picker and a switch row. */
@Composable
private fun NewHabitSheet(visible: Boolean, onClose: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var repeat by remember { mutableIntStateOf(0) }
    var times by remember { mutableIntStateOf(3) }
    var remind by remember { mutableStateOf(true) }
    IrisBottomSheet(visible = visible, onDismissRequest = onClose) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IrisSecondaryButton("Cancel", onClose)
            IrisText("New habit", style = IrisTheme.typography.dialogTitle)
            IrisSecondaryButton("Add", onClose)
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            IrisTextField(name, { name = it }, Modifier.fillMaxWidth(), placeholder = "Name of the habit")
        }
        IrisChipRow(Modifier.padding(vertical = 4.dp)) {
            listOf("Every day", "Weekdays", "Weekends", "Custom").forEachIndexed { index, label ->
                item { IrisChip(label, repeat == index, { repeat = index }) }
            }
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            IrisGroupedList(color = IrisTheme.colors.surfaceElevated) {
                item {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IrisText("Times a day", Modifier.weight(1f), style = IrisTheme.typography.body)
                        IrisNumberPicker(times, { times = it }, 1..9, format = { it.toString() })
                    }
                }
                item {
                    IrisListItem(
                        "Remind me",
                        trailing = { IrisSwitch(remind, { remind = it }) },
                        onClick = { remind = !remind },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Every text style of the theme with its name, size and weight. */
@Composable
fun TypeScale(onBack: () -> Unit) {
    val type = IrisTheme.typography
    val styles: List<Pair<String, TextStyle>> = listOf(
        "largeTitleXL" to type.largeTitleXL, "largeTitle" to type.largeTitle, "numeric" to type.numeric,
        "title" to type.title, "dialogTitle" to type.dialogTitle, "topBarTitle" to type.topBarTitle,
        "headline" to type.headline, "body" to type.body, "subtitle" to type.subtitle, "button" to type.button,
        "label" to type.label, "menuItem" to type.menuItem, "chip" to type.chip, "subtitleSmall" to type.subtitleSmall,
        "bodySmall" to type.bodySmall, "sectionHeader" to type.sectionHeader, "caption" to type.caption,
        "tabLabel" to type.tabLabel,
    )
    IrisScaffold(
        topBar = { IrisTopBar(title = "Type scale", navigationIcon = { IrisBackButton(onBack) }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item { Spacer(Modifier.height(8.dp)) }
            styles.forEach { (name, style) ->
                item {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        IrisText("Sphinx of black quartz", style = style)
                        IrisText(
                            "$name  ${style.fontSize.value.toInt()} sp, weight ${style.fontWeight?.weight ?: 400}",
                            style = IrisTheme.typography.caption,
                            color = IrisTheme.colors.contentSecondary,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ExampleGroup(content: dev.irisui.content.GroupedListScope.() -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp)) { IrisGroupedList(content = content) }
}
