# iris-ui

Package `dev.irisui.ui`. Umbrella: depends on every module above.

[All modules](README.md)

## IrisApp.kt

### IrisApp

One-stop root for an app that uses the whole kit: theme, haptics engine and the popup layer
(dialogs, sheets, menus). Pages themselves are built with the scaffolds from the navigation module.

It also tells the system bars which icon color to use, so the clock and the battery stay readable when the theme is
forced to light on a dark system (or the other way round).

`overscrollFactory` is what every list, row and pager of the app does at its end: the translating bounce of the stock
apps by default, `null` for no overscroll at all, or any other factory (for instance the Android stretch).

```kotlin
@Composable
fun IrisApp(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: IrisColors = if (darkTheme) irisDarkColors() else irisLightColors(),
    overscrollFactory: OverscrollFactory? = IrisOverscrollFactory,
    content: @Composable () -> Unit,
)
```

<sub>Source: [IrisApp.kt:35](../../iris-ui/src/main/kotlin/dev/irisui/ui/IrisApp.kt#L35)</sub>

