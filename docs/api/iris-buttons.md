# iris-buttons

Package `dev.irisui.buttons`. Primary, secondary, icon, round plus, text.

[All modules](README.md)

## Buttons.kt

### IrisPrimaryButton

Filled accent pill with the persistent colored glow underneath. Like every ColorOS button it
shrinks while held, follows a dragging finger with rubber-band resistance and springs back
with an overshoot.

```kotlin
@Composable
fun IrisPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    color: Color = IrisTheme.colors.accent,
    glow: Boolean = true,
)
```

<sub>Source: [Buttons.kt:60](../../iris-buttons/src/main/kotlin/dev/irisui/buttons/Buttons.kt#L60)</sub>

### IrisSecondaryButton

Neutral pill for secondary actions.

```kotlin
@Composable
fun IrisSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color = IrisTheme.colors.surfaceVariant,
    contentColor: Color = IrisTheme.colors.content,
)
```

<sub>Source: [Buttons.kt:98](../../iris-buttons/src/main/kotlin/dev/irisui/buttons/Buttons.kt#L98)</sub>

### IconButtonStyle

Look of a `IrisIconButton`: `Glass` is the round glass button of the top bars, `Filled` the accent coloured one with its glow,
`Plain` is just the icon.

```kotlin
enum class IconButtonStyle {
    Glass,
    Filled,
    Plain,
}
```

<sub>Source: [Buttons.kt:131](../../iris-buttons/src/main/kotlin/dev/irisui/buttons/Buttons.kt#L131)</sub>

### IrisIconButton

Round icon button (the round buttons of the top bar included). Shrinks while held and follows the finger like every
ColorOS button, however far it is dragged: the touch belongs to the button, a list behind it does not scroll. (The real
toolbar buttons let go as soon as the finger leaves them; here they can be dragged like everything else.)

```kotlin
@Composable
fun IrisIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    style: IconButtonStyle = IconButtonStyle.Glass,
    size: Dp = 40.dp,
    tint: Color = Color.Unspecified,
)
```

<sub>Source: [Buttons.kt:148](../../iris-buttons/src/main/kotlin/dev/irisui/buttons/Buttons.kt#L148)</sub>

### IrisRoundPlusButton

The big round "+" button. Always lit by its colored glow. It shrinks while held and, unlike the
toolbar buttons, keeps its pressed state however far the finger is dragged away; it stretches
towards the finger and springs back with an overshoot when released.

```kotlin
@Composable
fun IrisRoundPlusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = IrisIcons.Plus,
    contentDescription: String? = "Add",
    color: Color = IrisTheme.colors.accent,
    size: Dp = 56.dp,
)
```

<sub>Source: [Buttons.kt:208](../../iris-buttons/src/main/kotlin/dev/irisui/buttons/Buttons.kt#L208)</sub>

### IrisTextButton

Text-only action in the accent color.

```kotlin
@Composable
fun IrisTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = IrisTheme.colors.accent,
)
```

<sub>Source: [Buttons.kt:242](../../iris-buttons/src/main/kotlin/dev/irisui/buttons/Buttons.kt#L242)</sub>

