# iris-content

Package `dev.irisui.content`. Card, list item, grouped list, section header, divider.

[All modules](README.md)

## Badge.kt

### IrisRedDot

Red dot that hints at something new (no number): 8 dp, the destructive container red.

```kotlin
@Composable
fun IrisRedDot(modifier: Modifier = Modifier, color: Color = IrisTheme.colors.destructiveContainer)
```

<sub>Source: [Badge.kt:20](../../iris-content/src/main/kotlin/dev/irisui/content/Badge.kt#L20)</sub>

### IrisBadge

Red badge with a count: a pill at least 16 dp high with white digits (11 sp); counts above `max` read "max+".

```kotlin
@Composable
fun IrisBadge(
    count: Int,
    modifier: Modifier = Modifier,
    max: Int = 99,
    color: Color = IrisTheme.colors.destructiveContainer,
)
```

<sub>Source: [Badge.kt:28](../../iris-content/src/main/kotlin/dev/irisui/content/Badge.kt#L28)</sub>

## Card.kt

### IrisCard

Rounded surface for grouping content. With `onClick` it behaves like a touchable ColorOS surface: it shrinks a little
while held (large cards barely, small ones more) and gets lighter. A card is not a glass button, so it does not follow
a dragging finger and never claims a drag: a page scrolls from a card.

```kotlin
@Composable
fun IrisCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = IrisTheme.colors.surface,
    shape: Shape = IrisTheme.shapes.card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
)
```

<sub>Source: [Card.kt:26](../../iris-content/src/main/kotlin/dev/irisui/content/Card.kt#L26)</sub>

## ListItems.kt

### IrisListItem

A row as used throughout ColorOS Settings: optional leading icon, title with an optional
summary, a trailing value / chevron / custom control. Rows with `onClick` show the press highlight.

Metrics are those of the stock preference row: 54 dp minimum height, 10 dp vertical and 16 dp horizontal padding,
the summary directly under the title, 16 dp between the text and a trailing switch.

```kotlin
@Composable
fun IrisListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    showChevron: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    /** Shows the small up / down arrows of a row that opens a choice menu instead of a chevron. */
    showDropdown: Boolean = false,
    /** Lights the row while a popup menu opened from it is showing (15 % white over the card). */
    highlighted: Boolean = false,
)
```

<sub>Source: [ListItems.kt:52](../../iris-content/src/main/kotlin/dev/irisui/content/ListItems.kt#L52)</sub>

### IrisListIcon

Free-standing colored glyph as used in Settings rows (24 dp box: the text then starts 56 dp from the card edge).

```kotlin
@Composable
fun IrisListIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier)
```

<sub>Source: [ListItems.kt:190](../../iris-content/src/main/kotlin/dev/irisui/content/ListItems.kt#L190)</sub>

### IrisIconTile

Glyph on a rounded colored tile, as used in the Files app.

```kotlin
@Composable
fun IrisIconTile(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: Shape = IrisTheme.shapes.small,
)
```

<sub>Source: [ListItems.kt:198](../../iris-content/src/main/kotlin/dev/irisui/content/ListItems.kt#L198)</sub>

### GroupedListScope

Scope of the content of `IrisGroupedList`: add the rows with `item`.

```kotlin
@Stable
class GroupedListScope internal constructor() {
    // Adds one row to the group; dividers are inserted between rows automatically.
    fun item(content: @Composable () -> Unit)
}
```

<sub>Source: [ListItems.kt:215](../../iris-content/src/main/kotlin/dev/irisui/content/ListItems.kt#L215)</sub>

### IrisGroupedList

A card that holds several rows separated by inset dividers. `dividerStart` should match where
the row text begins (56.dp with a leading icon, 16.dp without).

```kotlin
@Composable
fun IrisGroupedList(
    modifier: Modifier = Modifier,
    dividerStart: Dp = 16.dp,
    color: Color = IrisTheme.colors.surface,
    content: GroupedListScope.() -> Unit,
)
```

<sub>Source: [ListItems.kt:229](../../iris-content/src/main/kotlin/dev/irisui/content/ListItems.kt#L229)</sub>

## Section.kt

### IrisSectionHeader

Small gray caption above a group of rows ("Volume", "Sound effects"). Same metrics as the stock category title:
text aligned with the row text of the cards (32 dp from the screen edge), 16 dp gap above (when a card precedes it)
plus 8 dp padding, 8 dp padding below.

```kotlin
@Composable
fun IrisSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    /** Space above the text: 23.33 dp after a card, 18 dp when it is the first thing under the top bar. */
    topPadding: Dp = 23.33.dp,
)
```

<sub>Source: [Section.kt:25](../../iris-content/src/main/kotlin/dev/irisui/content/Section.kt#L25)</sub>

### IrisDivider

Hairline divider. Rows inside a group use `start` to line up with the text after the icon. The default thickness is
one device pixel, like the real dividers (20 % white on dark, 12 % black on light).

```kotlin
@Composable
fun IrisDivider(
    modifier: Modifier = Modifier,
    start: Dp = 0.dp,
    end: Dp = 0.dp,
    thickness: Dp = Dp.Unspecified,
)
```

<sub>Source: [Section.kt:53](../../iris-content/src/main/kotlin/dev/irisui/content/Section.kt#L53)</sub>

