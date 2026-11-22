# iris-popups

Package `dev.irisui.popups`. Dialog, bottom sheet, popup menu.

[All modules](README.md)

## BottomSheet.kt

### IrisBottomSheet

Bottom sheet. It slides up from the bottom, can be dragged down to dismiss (with rubber band
resistance upwards and a springy return) and dims the page behind it.

```kotlin
@Composable
fun IrisBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    fullHeight: Boolean = false,
    showHandle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
)
```

<sub>Source: [BottomSheet.kt:55](../../iris-popups/src/main/kotlin/dev/irisui/popups/BottomSheet.kt#L55)</sub>

## Dialog.kt

### IrisDialog

Confirmation dialog. It docks near the bottom of the screen like on ColorOS: a dimmed page,
a rounded glass card, a centered title and two pill buttons side by side.

```kotlin
@Composable
fun IrisDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissText: String? = "Cancel",
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    /** Greys the confirm button out (for example while a text field is still empty). */
    confirmEnabled: Boolean = true,
)
```

<sub>Source: [Dialog.kt:54](../../iris-popups/src/main/kotlin/dev/irisui/popups/Dialog.kt#L54)</sub>

### IrisDialogContainer

Dialog shell without any content, for fully custom dialogs. The card rides above the navigation bar and the keyboard.

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IrisDialogContainer(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
)
```

<sub>Source: [Dialog.kt:139](../../iris-popups/src/main/kotlin/dev/irisui/popups/Dialog.kt#L139)</sub>

## PopupHost.kt

### IrisPopupHost

Wrap the whole app (or one screen) in this host. Dialogs, sheets and menus are drawn in a layer
above `content` and blur it through the host's backdrop.

```kotlin
@Composable
fun IrisPopupHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
)
```

<sub>Source: [PopupHost.kt:31](../../iris-popups/src/main/kotlin/dev/irisui/popups/PopupHost.kt#L31)</sub>

## PopupMenu.kt

### IrisPopupMenuBox

Anchors a popup menu to `anchor`. The menu opens as a blurred glass card that pops out of a point near the anchor
(79 % across, 33 % down the panel), the way the "more" menus of ColorOS apps do, and closes the same way, faster.

```kotlin
@Composable
fun IrisPopupMenuBox(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * For a menu opened from a full width list row (a dropdown preference): the panel is centered on the screen and
     * its top edge sits at the vertical center of the row, instead of hugging the anchor's edge like a "more" menu.
     */
    centerOnScreen: Boolean = false,
    menu: @Composable ColumnScope.() -> Unit,
    anchor: @Composable () -> Unit,
)
```

<sub>Source: [PopupMenu.kt:129](../../iris-popups/src/main/kotlin/dev/irisui/popups/PopupMenu.kt#L129)</sub>

### IrisMenuItem

One row of a popup menu: 40 dp high, text 20 dp from the panel edge (15 sp). Pressing, sliding and selecting is
handled by the panel, so rows have no touch feedback of their own. Closes the menu after `onClick`.

```kotlin
@Composable
fun IrisMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    checked: Boolean = false,
    destructive: Boolean = false,
)
```

<sub>Source: [PopupMenu.kt:318](../../iris-popups/src/main/kotlin/dev/irisui/popups/PopupMenu.kt#L318)</sub>

### IrisMenuDivider

Gap between groups of rows: 12 dp high with a 4 dp band in 8 % black.

```kotlin
@Composable
fun IrisMenuDivider(modifier: Modifier = Modifier)
```

<sub>Source: [PopupMenu.kt:362](../../iris-popups/src/main/kotlin/dev/irisui/popups/PopupMenu.kt#L362)</sub>

