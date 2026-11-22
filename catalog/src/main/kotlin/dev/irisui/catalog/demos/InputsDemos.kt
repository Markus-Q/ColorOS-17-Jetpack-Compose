package dev.irisui.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irisui.content.IrisGroupedList
import dev.irisui.content.IrisListIcon
import dev.irisui.content.IrisListItem
import dev.irisui.core.IrisIcons
import dev.irisui.effects.LocalBackdrop
import dev.irisui.effects.backdropSource
import dev.irisui.effects.rememberBackdrop
import dev.irisui.inputs.IrisCheckbox
import dev.irisui.inputs.IrisCircularProgress
import dev.irisui.inputs.IrisLoadingSpinner
import dev.irisui.inputs.IrisProgressBar
import dev.irisui.inputs.IrisSearchBar
import dev.irisui.inputs.IrisSlider
import dev.irisui.inputs.IrisSwitch
import dev.irisui.inputs.IrisTextField
import kotlin.math.roundToInt

@Composable
fun SwitchDemo(onBack: () -> Unit) {
    var aeroplane by remember { mutableStateOf(false) }
    var autoBrightness by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(true) }
    DemoPage("Switch", onBack, subtitle = "Springs across and ticks the haptics") {
        demoSection("In a list") {
            IrisGroupedList(dividerStart = 56.dp) {
                item {
                    IrisListItem(
                        "Aeroplane mode",
                        leading = { IrisListIcon(IrisIcons.Star, Color(0xFFFFB020)) },
                        trailing = { IrisSwitch(aeroplane, { aeroplane = it }) },
                        onClick = { aeroplane = !aeroplane },
                    )
                }
                item {
                    IrisListItem(
                        "Auto brightness",
                        leading = { IrisListIcon(IrisIcons.Sun, Color(0xFFFFB020)) },
                        trailing = { IrisSwitch(autoBrightness, { autoBrightness = it }) },
                        onClick = { autoBrightness = !autoBrightness },
                    )
                }
                item {
                    IrisListItem(
                        "Dark mode",
                        supporting = "Easier on the eyes at night",
                        leading = { IrisListIcon(IrisIcons.Bell, Color(0xFF1A8CFF)) },
                        trailing = { IrisSwitch(darkMode, { darkMode = it }) },
                        onClick = { darkMode = !darkMode },
                    )
                }
            }
        }
        demoSection("Loose") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                IrisSwitch(aeroplane, { aeroplane = it })
                IrisSwitch(true, null)
                IrisSwitch(false, null, enabled = false)
                IrisSwitch(true, null, enabled = false)
            }
            Caption("Press and hold: the track lights up. Release: the thumb stretches while it travels and squeezes when it overshoots.")
        }
    }
}

@Composable
fun TextFieldDemo(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("name@") }
    var note by remember { mutableStateOf("") }
    DemoPage("Text field", onBack, subtitle = "Rounded fill, accent border on focus") {
        demoSection("Basic") {
            IrisTextField(name, { name = it }, label = "Name", placeholder = "Your name")
        }
        demoSection("With icon") {
            IrisTextField(note, { note = it }, placeholder = "Search notes", leadingIcon = IrisIcons.Search)
        }
        demoSection("Error") {
            IrisTextField(
                email,
                { email = it },
                label = "Email",
                isError = !email.contains("@") || !email.substringAfter("@").contains("."),
                supportingText = "Enter a complete address such as name@example.com",
            )
        }
        demoSection("Disabled") {
            IrisTextField("Read only", {}, label = "Device name", enabled = false)
        }
    }
}

@Composable
fun SearchBarDemo(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var glassQuery by remember { mutableStateOf("") }
    val fruits = listOf("Apple", "Apricot", "Banana", "Blueberry", "Cherry", "Mango", "Orange", "Peach", "Pear", "Plum")
    val matches = fruits.filter { it.contains(query, ignoreCase = true) }
    DemoPage("Search bar", onBack, subtitle = "Tap it: a Cancel action slides in") {
        demoSection("Filtering") {
            IrisSearchBar(query, { query = it })
            IrisGroupedList {
                if (matches.isEmpty()) {
                    item { IrisListItem("No results") }
                }
                matches.forEach { fruit -> item { IrisListItem(fruit) } }
            }
        }
        demoSection("Glass over content") {
            val backdrop = rememberBackdrop()
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                DemoWallpaper(Modifier.fillMaxWidth().height(140.dp).backdropSource(backdrop))
                androidx.compose.runtime.CompositionLocalProvider(LocalBackdrop provides backdrop) {
                    IrisSearchBar(
                        glassQuery,
                        { glassQuery = it },
                        Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
                        glass = true,
                    )
                }
            }
        }
    }
}

@Composable
fun CheckboxDemo(onBack: () -> Unit) {
    val labels = listOf("Photos", "Videos", "Audio", "Documents")
    val checked = remember { mutableStateListOf(true, false, true, false) }
    val all = checked.all { it }
    DemoPage("Checkbox", onBack, subtitle = "Rounded square, the tick draws itself") {
        demoSection("Selection list") {
            IrisGroupedList {
                item {
                    IrisListItem(
                        "Select all",
                        trailing = { IrisCheckbox(all, { v -> checked.indices.forEach { checked[it] = v } }) },
                        onClick = { val v = !all; checked.indices.forEach { checked[it] = v } },
                    )
                }
                labels.forEachIndexed { index, label ->
                    item {
                        IrisListItem(
                            label,
                            trailing = { IrisCheckbox(checked[index], { checked[index] = it }) },
                            onClick = { checked[index] = !checked[index] },
                        )
                    }
                }
            }
            Caption("${checked.count { it }} of ${labels.size} selected")
        }
        demoSection("Loose") {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                IrisCheckbox(false, null)
                IrisCheckbox(true, null)
                IrisCheckbox(true, null, enabled = false)
            }
        }
    }
}

@Composable
fun SliderDemo(onBack: () -> Unit) {
    var brightness by remember { mutableFloatStateOf(0.55f) }
    var corner by remember { mutableFloatStateOf(1f) }
    var level by remember { mutableFloatStateOf(2f) }
    var iconSize by remember { mutableFloatStateOf(1f) }
    var volume by remember { mutableFloatStateOf(30f) }
    DemoPage("Slider", onBack, subtitle = "Thick bar, a light follows the thumb while you hold it") {
        demoCard("Brightness") {
            IrisSlider(brightness, { brightness = it }, startIcon = IrisIcons.Sun, endIcon = IrisIcons.Sun)
            Caption("${(brightness * 100).roundToInt()} %")
        }
        demoCard("Rounded corner: dots mark the steps") {
            IrisSlider(corner, { corner = it }, valueRange = 0f..2f, steps = 1)
            Caption("Step ${corner.roundToInt() + 1} of 3. The thumb moves slower than the finger and catches up at every dot.")
        }
        demoCard("Five steps") {
            IrisSlider(level, { level = it }, valueRange = 0f..4f, steps = 3)
            Caption("Level ${level.roundToInt() + 1} of 5")
        }
        demoCard("Icon size: steps without dots") {
            IrisSlider(iconSize, { iconSize = it }, valueRange = 0f..2f, steps = 1, showMarks = false)
            Caption(listOf("Small", "Default", "Large")[iconSize.roundToInt().coerceIn(0, 2)])
        }
        demoCard("Volume") {
            IrisSlider(volume, { volume = it }, valueRange = 0f..100f)
            Caption("${volume.roundToInt()}")
        }
        demoSection("Disabled") {
            IrisSlider(0.4f, {}, enabled = false)
        }
    }
}

@Composable
fun ProgressDemo(onBack: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0.38f) }
    DemoPage("Progress", onBack, subtitle = "Bar, ring and spinner of the stock apps") {
        demoCard("Linear, 8 dp (Files storage card)") {
            IrisProgressBar(progress)
            Caption("${(progress * 100).roundToInt()} %  -  drag the slider to see it glide")
            IrisSlider(progress, { progress = it })
        }
        demoCard("Indeterminate") {
            IrisProgressBar()
        }
        demoCard("Ring and spinner") {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IrisCircularProgress(progress)
                IrisLoadingSpinner()
                IrisLoadingSpinner(size = 40.dp, strokeWidth = 3.5.dp)
            }
        }
    }
}
