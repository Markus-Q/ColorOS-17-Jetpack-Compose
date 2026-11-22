package dev.irisui.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irisui.buttons.IrisPrimaryButton
import dev.irisui.buttons.IrisSecondaryButton
import dev.irisui.content.IrisGroupedList
import dev.irisui.content.IrisListIcon
import dev.irisui.content.IrisListItem
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.inputs.IrisSwitch
import dev.irisui.inputs.IrisUnderlineTextField
import dev.irisui.popups.IrisBottomSheet
import dev.irisui.popups.IrisDialog
import dev.irisui.popups.IrisMenuDivider
import dev.irisui.popups.IrisMenuItem
import dev.irisui.popups.IrisPopupMenuBox

@Composable
fun DialogDemo(onBack: () -> Unit) {
    var plain by remember { mutableStateOf(false) }
    var destructive by remember { mutableStateOf(false) }
    var withInput by remember { mutableStateOf(false) }
    var savedName by remember { mutableStateOf("OnePlus 15") }
    var name by remember { mutableStateOf(savedName) }
    var result by remember { mutableStateOf("-") }
    DemoPage("Dialog", onBack, subtitle = "Docked glass card, dimmed page") {
        demoSection("Variants") {
            IrisPrimaryButton("Standard dialog", { plain = true }, Modifier.fillMaxWidth())
            IrisSecondaryButton("Destructive dialog", { destructive = true }, Modifier.fillMaxWidth())
            IrisSecondaryButton("Dialog with input", { withInput = true }, Modifier.fillMaxWidth())
            Caption("Last result: $result")
        }
    }
    IrisDialog(
        visible = plain,
        onDismissRequest = { plain = false; result = "dismissed" },
        title = "Allow notifications?",
        message = "The app can send you alerts about new activity. You can change this later in Settings.",
        confirmText = "Allow",
        onConfirm = { plain = false; result = "allowed" },
        dismissText = "Not now",
    )
    IrisDialog(
        visible = destructive,
        onDismissRequest = { destructive = false; result = "kept" },
        title = "Delete this note?",
        message = "This note will be permanently deleted from this device. Deleted items are kept in \"Recently deleted\" for 30 days.",
        confirmText = "Delete",
        destructive = true,
        onConfirm = { destructive = false; result = "deleted" },
    )
    // The "Change device name" dialog of Settings: underlined field with counter and clear button, Save stays grey
    // until the name has changed.
    val focus = remember { FocusRequester() }
    IrisDialog(
        visible = withInput,
        onDismissRequest = { withInput = false; name = savedName },
        title = "Change device name",
        message = "Your device name will be displayed when you use Bluetooth or Wi-Fi Direct.",
        confirmText = "Save",
        confirmEnabled = name.isNotBlank() && name != savedName,
        onConfirm = { withInput = false; savedName = name; result = "saved \"$name\"" },
        content = {
            Spacer(Modifier.height(12.dp))
            IrisUnderlineTextField(name, { name = it }, maxLength = 32, focusRequester = focus)
            LaunchedEffect(Unit) { focus.requestFocus() }
        },
    )
}

@Composable
fun BottomSheetDemo(onBack: () -> Unit) {
    var sheet by remember { mutableStateOf(false) }
    var full by remember { mutableStateOf(false) }
    var vibrate by remember { mutableStateOf(true) }
    DemoPage("Bottom sheet", onBack, subtitle = "Drag it down: rubber band up, spring back") {
        demoSection("Open") {
            IrisPrimaryButton("New alarm sheet", { sheet = true }, Modifier.fillMaxWidth())
            IrisSecondaryButton("Full height sheet", { full = true }, Modifier.fillMaxWidth())
        }
    }
    IrisBottomSheet(visible = sheet, onDismissRequest = { sheet = false }) {
        SheetHeader("New alarm", onCancel = { sheet = false }, onDone = { sheet = false })
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            IrisGroupedList(color = IrisTheme.colors.surfaceElevated) {
                item { IrisListItem("Sound", supporting = "Default alarm sound", showChevron = true, onClick = {}) }
                item {
                    IrisListItem(
                        "Vibrate",
                        trailing = { IrisSwitch(vibrate, { vibrate = it }) },
                        onClick = { vibrate = !vibrate },
                    )
                }
                item { IrisListItem("Snooze", supporting = "5 minutes, 3 times", showChevron = true, onClick = {}) }
            }
        }
        Box(Modifier.padding(16.dp)) { Caption("Swipe the sheet down or tap the dimmed area to close.") }
    }
    IrisBottomSheet(visible = full, onDismissRequest = { full = false }, fullHeight = true) {
        SheetHeader("Full height", onCancel = { full = false }, onDone = { full = false })
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            IrisGroupedList(color = IrisTheme.colors.surfaceElevated) {
                repeat(10) { index ->
                    item {
                        IrisListItem(
                            "Option ${index + 1}",
                            leading = { IrisListIcon(IrisIcons.Star, Color(0xFFFFB020)) },
                            showChevron = true,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onCancel: () -> Unit, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IrisSecondaryButton("Cancel", onCancel, Modifier)
        IrisText(title, style = IrisTheme.typography.headline)
        IrisSecondaryButton("Done", onDone, Modifier)
    }
}

@Composable
fun PopupMenuDemo(onBack: () -> Unit) {
    var menu1 by remember { mutableStateOf(false) }
    var menu2 by remember { mutableStateOf(false) }
    var sortEdited by remember { mutableStateOf(true) }
    var picked by remember { mutableStateOf("-") }
    DemoPage("Popup menu", onBack, subtitle = "Scales out of the corner nearest the anchor") {
        demoSection("Anchored at the end") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IrisPopupMenuBox(
                    expanded = menu1,
                    onDismissRequest = { menu1 = false },
                    menu = {
                        IrisMenuItem("Edit", onClick = { picked = "Edit" })
                        IrisMenuItem("Grid view", onClick = { picked = "Grid view" })
                        IrisMenuItem("Settings", onClick = { picked = "Settings" })
                        IrisMenuDivider()
                        IrisMenuItem("Sort by time created", onClick = { sortEdited = false }, checked = !sortEdited)
                        IrisMenuItem("Sort by time edited", onClick = { sortEdited = true }, checked = sortEdited)
                    },
                ) {
                    IrisSecondaryButton("Open menu", { menu1 = true })
                }
            }
        }
        demoSection("Anchored at the start, with icons") {
            IrisPopupMenuBox(
                expanded = menu2,
                onDismissRequest = { menu2 = false },
                menu = {
                    IrisMenuItem("Favorite", onClick = { picked = "Favorite" }, icon = IrisIcons.Heart)
                    IrisMenuItem("Remind me", onClick = { picked = "Remind me" }, icon = IrisIcons.Bell)
                    IrisMenuDivider()
                    IrisMenuItem("Delete", onClick = { picked = "Delete" }, icon = IrisIcons.Trash, destructive = true)
                },
            ) {
                IrisSecondaryButton("Open menu with icons", { menu2 = true })
            }
            Caption("Picked: $picked")
        }
    }
}
