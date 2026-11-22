# iris-inputs

Package `dev.irisui.inputs`. Switch, text field, search bar, checkbox, slider.

[All modules](README.md)

## Checkbox.kt

### IrisCheckbox

Rounded-square checkbox, 18 dp inside a 30 dp touch area, as in the selection mode of the stock apps: a 1.7 dp ring
in the faint label color turns into an accent filled square with a white check that draws itself. Like the real one
it is silent (no haptic).

```kotlin
@Composable
fun IrisCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

<sub>Source: [Checkbox.kt:40](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/Checkbox.kt#L40)</sub>

### IrisRadioButton

Radio button: 20 dp. Selected it is an accent disc with a white 12 dp dot, unselected a 1.7 dp ring in the faint
label color (measured on the ringer mode selector of Settings).

```kotlin
@Composable
fun IrisRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

<sub>Source: [Checkbox.kt:106](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/Checkbox.kt#L106)</sub>

## NumberPicker.kt

### IrisNumberPicker

Wheel for numbers (hours, minutes, durations), three rows high: the centered value in the label color at 22 sp, the
neighbours at 17 sp and 30 % brightness, as in the Clock sheets. It snaps to a row when released and every step ticks
the haptic motor with the picker constant of the stock apps. With `wrap` the wheel turns around at the ends.

```kotlin
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
)
```

<sub>Source: [NumberPicker.kt:45](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/NumberPicker.kt#L45)</sub>

## Progress.kt

### IrisProgressBar

Horizontal progress bar, measured on the storage card of the Files app: 8 dp high, fully
rounded track in the same tone as the empty part of a slider, the accent fill ends in a straight edge. Changes of
`progress` glide instead of jumping.

```kotlin
@Composable
fun IrisProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = IrisTheme.colors.accent,
    trackColor: Color = IrisTheme.colors.trackEmpty,
)
```

<sub>Source: [Progress.kt:35](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/Progress.kt#L35)</sub>

### IrisProgressBar

Progress bar that does not know how far it is: a quarter of the bar sweeps back and forth.

```kotlin
@Composable
fun IrisProgressBar(
    modifier: Modifier = Modifier,
    color: Color = IrisTheme.colors.accent,
    trackColor: Color = IrisTheme.colors.trackEmpty,
)
```

<sub>Source: [Progress.kt:52](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/Progress.kt#L52)</sub>

### IrisLoadingSpinner

Round loading indicator: an accent arc of 270 degrees turning once a second.

```kotlin
@Composable
fun IrisLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = IrisTheme.colors.accent,
    strokeWidth: Dp = 2.5.dp,
)
```

<sub>Source: [Progress.kt:75](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/Progress.kt#L75)</sub>

### IrisCircularProgress

Ring that fills like a progress bar.

```kotlin
@Composable
fun IrisCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = IrisTheme.colors.accent,
    trackColor: Color = IrisTheme.colors.trackEmpty,
    strokeWidth: Dp = 4.dp,
)
```

<sub>Source: [Progress.kt:103](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/Progress.kt#L103)</sub>

## Slider.kt

### IrisSlider

Thick ColorOS slider, rebuilt from recordings of the volume rows of Settings and the Icons page of the theme app:

 * The bar is 20 dp high inside a 28 dp touch area. The thumb is a white 12 dp dot whose center travels from 10 dp
   inside the left end to 10 dp inside the right end; the accent fill always reaches 10 dp past it.
 * While it is held the thumb swells to 16 dp and a soft light spot (a color dodge of up to 54 % that falls
   off over about 60 dp) follows it, brightening the fill and the track. Both ease back after release.
 * **Continuous** (steps = 0): a tap slides the thumb to the spot with a critically damped spring of 0.3 s, while it
   is dragged the thumb trails the finger with a 0.15 s spring.
 * **Stepped** (steps &gt; 0, with `showMarks` the Rounded Corner style): a dot marks every step. Pressing the thumb
   and dragging moves it by 61.5 % of the finger's way, and each time the finger crosses a mark the thumb catches up
   with it. On release the thumb slides to the mark nearest to it, a tap on the bar slides it to the mark nearest
   to the tap. Without `showMarks` (the Icon size style) the thumb follows the finger 1:1 and only snaps on release.

Every step crossed while the finger is down fires the slider haptic of the stock apps (constant 305), for a
continuous slider every whole percent.

A press on the thumb starts dragging at once. A press elsewhere on the bar of a continuous slider only picks the
value once the finger moves sideways or lifts. The whole slider also follows a dragging finger with the rubber-band
physics of every touchable element (stretching along the drag, springing back with an overshoot); a swipe that goes
up or down from a slider belongs to the slider, not to a scrolling page behind it, and does not change the value.

```kotlin
@Composable
fun IrisSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    startIcon: ImageVector? = null,
    endIcon: ImageVector? = null,
    hapticFeedback: Boolean = true,
    showMarks: Boolean = true,
)
```

<sub>Source: [Slider.kt:183](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/Slider.kt#L183)</sub>

## Switch.kt

### IrisSwitch

ColorOS switch: 44 x 24 dp track, 18 dp white thumb with 3 dp margins.

A tap flips it: the thumb is a pill between two edges that both follow the toggle spring (bounce 0.3, 0.4 s); the
trailing edge starts about 29 ms after the leading one, so the thumb stretches while it travels and squeezes a little
when it overshoots. The thumb can also be dragged: it follows the finger one to one, the state flips (with the toggle
haptic) when it crosses the middle, and on release it springs to the end of the current state. The track lightens by
the press overlay while held and cross-fades from grey to the accent color together with the trailing edge.

```kotlin
@Composable
fun IrisSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
)
```

<sub>Source: [Switch.kt:67](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/Switch.kt#L67)</sub>

## TextFields.kt

### IrisTextField

Filled text field with a rounded container. The border lights up in the accent color on focus.

```kotlin
@Composable
fun IrisTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
)
```

<sub>Source: [TextFields.kt:60](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/TextFields.kt#L60)</sub>

### IrisUnderlineTextField

Text field of the stock dialogs (the "Change device name" dialog of Settings): the text sits on a bare 48 dp row with
a 1 dp underline that turns blue while the field has focus. While editing, a character counter ("10/32") and a round
clear button appear at the end of the row.

With `maxLength` input beyond the limit is cut off and the counter is shown. Pass a `focusRequester` to focus the
field as soon as its dialog opens.

```kotlin
@Composable
fun IrisUnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    maxLength: Int? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
)
```

<sub>Source: [TextFields.kt:143](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/TextFields.kt#L143)</sub>

### IrisSearchBar

Pill-shaped search bar. Set `glass` to blur the content behind it when it floats over a list.
While it has focus the field narrows and a "Cancel" pill of the same material slides in beside it, like the search
page of Settings (field 728 px and pill 225 px wide at 480 dpi, 31 px apart).

```kotlin
@Composable
fun IrisSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: ((String) -> Unit)? = null,
    glass: Boolean = false,
    showCancel: Boolean = true,
)
```

<sub>Source: [TextFields.kt:243](../../iris-inputs/src/main/kotlin/dev/irisui/inputs/TextFields.kt#L243)</sub>

