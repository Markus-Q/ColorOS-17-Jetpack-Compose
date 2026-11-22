package dev.irisui.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.irisui.buttons.IrisTextButton
import dev.irisui.content.IrisCard
import dev.irisui.content.IrisDivider
import dev.irisui.content.IrisGroupedList
import dev.irisui.content.IrisIconTile
import dev.irisui.content.IrisListIcon
import dev.irisui.content.IrisListItem
import dev.irisui.content.IrisSectionHeader
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.inputs.IrisSwitch

@Composable
fun CardDemo(onBack: () -> Unit) {
    var taps by remember { mutableIntStateOf(0) }
    DemoPage("Card", onBack, subtitle = "Rounded surface with squircle corners") {
        demoSection("Static") {
            IrisCard(Modifier.fillMaxWidth()) {
                IrisText("All files", style = IrisTheme.typography.body)
                IrisText("94.4 GB", Modifier.padding(top = 24.dp), style = IrisTheme.typography.largeTitle)
                Caption("of 256 GB used")
            }
        }
        demoSection("Clickable") {
            IrisCard(Modifier.fillMaxWidth(), onClick = { taps++ }) {
                IrisText("Tap me", style = IrisTheme.typography.headline)
                Caption("It shrinks a little and shows the press highlight. Taps: $taps")
            }
        }
        demoSection("Tiles") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Photos" to Color(0xFF1A8CFF), "Videos" to Color(0xFF6E56F5), "Audio" to Color(0xFFFF8A1F)).forEach { (label, color) ->
                    IrisCard(Modifier.weight(1f), onClick = {}) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            IrisIconTile(IrisIcons.Star, color, size = 44.dp)
                            IrisText(label, Modifier.padding(top = 8.dp), style = IrisTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListItemDemo(onBack: () -> Unit) {
    var checked by remember { mutableStateOf(true) }
    DemoPage("List item", onBack, subtitle = "The row used across ColorOS Settings") {
        demoSection("Title only") {
            IrisGroupedList { item { IrisListItem("Notifications", showChevron = true, onClick = {}) } }
        }
        demoSection("With summary") {
            IrisGroupedList {
                item { IrisListItem("Sound", supporting = "Default alarm sound", showChevron = true, onClick = {}) }
            }
        }
        demoSection("With value and chevron") {
            IrisGroupedList {
                item { IrisListItem("Wi-Fi", value = "Home network", showChevron = true, onClick = {}) }
            }
        }
        demoSection("With icon and switch") {
            IrisGroupedList {
                item {
                    IrisListItem(
                        "Auto brightness",
                        leading = { IrisListIcon(IrisIcons.Sun, Color(0xFFFFB020)) },
                        trailing = { IrisSwitch(checked, { checked = it }) },
                        onClick = { checked = !checked },
                    )
                }
            }
        }
        demoSection("Disabled") {
            IrisGroupedList {
                item { IrisListItem("Unavailable", supporting = "Needs a SIM card", enabled = false, onClick = {}) }
            }
        }
    }
}

@Composable
fun GroupedListDemo(onBack: () -> Unit) {
    var aeroplane by remember { mutableStateOf(false) }
    DemoPage("Grouped list", onBack, subtitle = "Rows share one card, dividers line up with the text") {
        demoSection("Settings style") {
            IrisGroupedList(dividerStart = 56.dp) {
                item {
                    IrisListItem(
                        "Aeroplane mode",
                        leading = { IrisListIcon(IrisIcons.Star, Color(0xFFFFB020)) },
                        trailing = { IrisSwitch(aeroplane, { aeroplane = it }) },
                    )
                }
                item {
                    IrisListItem(
                        "Wi-Fi",
                        value = "Home network",
                        leading = { IrisListIcon(IrisIcons.Search, Color(0xFF1A8CFF)) },
                        showChevron = true,
                        onClick = {},
                    )
                }
                item {
                    IrisListItem(
                        "Notifications",
                        leading = { IrisListIcon(IrisIcons.Bell, Color(0xFF1A8CFF)) },
                        showChevron = true,
                        onClick = {},
                    )
                }
            }
        }
        demoSection("Without icons") {
            IrisGroupedList {
                item { IrisListItem("Edit", onClick = {}) }
                item { IrisListItem("Grid view", onClick = {}) }
                item { IrisListItem("Settings", showChevron = true, onClick = {}) }
            }
        }
    }
}

@Composable
fun SectionHeaderDemo(onBack: () -> Unit) {
    DemoPage("Section header", onBack, subtitle = "Small gray caption above a group") {
        item {
            IrisSectionHeader("Brightness")
            Column(Modifier.padding(horizontal = 16.dp)) {
                IrisGroupedList { item { IrisListItem("Auto brightness") } }
            }
            IrisSectionHeader("Comfort view", trailing = { IrisTextButton("Edit", onClick = {}) })
            Column(Modifier.padding(horizontal = 16.dp)) {
                IrisGroupedList {
                    item { IrisListItem("Eye Care", showChevron = true, onClick = {}) }
                    item { IrisListItem("Screen colour mode", showChevron = true, onClick = {}) }
                }
            }
        }
    }
}

@Composable
fun DividerDemo(onBack: () -> Unit) {
    DemoPage("Divider", onBack, subtitle = "Hairline, optionally inset") {
        demoCard("Full width") {
            IrisText("Above")
            IrisDivider(Modifier.padding(vertical = 12.dp))
            IrisText("Below")
        }
        demoCard("Inset like a list with icons") {
            IrisText("Above")
            IrisDivider(Modifier.padding(vertical = 12.dp), start = 48.dp, end = 16.dp)
            IrisText("Below")
        }
    }
}

@Composable
fun BadgeDemo(onBack: () -> Unit) {
    DemoPage("Badge", onBack, subtitle = "Red dot and counts") {
        demoCard("Dot and counts") {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                dev.irisui.content.IrisRedDot()
                dev.irisui.content.IrisBadge(3)
                dev.irisui.content.IrisBadge(28)
                dev.irisui.content.IrisBadge(120)
            }
        }
        demoCard("On an icon") {
            androidx.compose.foundation.layout.Box(Modifier.padding(8.dp)) {
                dev.irisui.content.IrisIconTile(dev.irisui.core.IrisIcons.Bell, androidx.compose.ui.graphics.Color(0xFF1A8CFF))
                dev.irisui.content.IrisBadge(
                    5,
                    Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(top = 0.dp, end = 0.dp),
                )
            }
        }
    }
}
