package dev.irisui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.irisui.catalog.demos.BadgeDemo
import dev.irisui.catalog.demos.BlurLabDemo
import dev.irisui.catalog.demos.ProgressDemo
import dev.irisui.catalog.demos.BottomNavPillDemo
import dev.irisui.catalog.demos.BottomSheetDemo
import dev.irisui.catalog.demos.CardDemo
import dev.irisui.catalog.demos.CheckboxDemo
import dev.irisui.catalog.demos.DetailPage
import dev.irisui.catalog.demos.DialogDemo
import dev.irisui.catalog.demos.DividerDemo
import dev.irisui.catalog.demos.DragPhysicsDemo
import dev.irisui.catalog.demos.GroupedListDemo
import dev.irisui.catalog.demos.HapticsDemo
import dev.irisui.catalog.demos.IconButtonDemo
import dev.irisui.catalog.demos.LargeTitleDemo
import dev.irisui.catalog.demos.ListItemDemo
import dev.irisui.catalog.demos.PageTransitionsDemo
import dev.irisui.catalog.demos.PersistentGlowDemo
import dev.irisui.catalog.demos.PopupMenuDemo
import dev.irisui.catalog.demos.PressAnimationDemo
import dev.irisui.catalog.demos.PrimaryButtonDemo
import dev.irisui.catalog.demos.RoundPlusButtonDemo
import dev.irisui.catalog.demos.SearchBarDemo
import dev.irisui.catalog.demos.HabitsExample
import dev.irisui.catalog.demos.PreferencesExample
import dev.irisui.catalog.demos.TypeScale
import dev.irisui.catalog.demos.SecondaryButtonDemo
import dev.irisui.catalog.demos.SectionHeaderDemo
import dev.irisui.catalog.demos.SegmentedTabsDemo
import dev.irisui.catalog.demos.SliderDemo
import dev.irisui.catalog.demos.SwitchDemo
import dev.irisui.catalog.demos.TextButtonDemo
import dev.irisui.catalog.demos.TextFieldDemo
import dev.irisui.catalog.demos.TopBarDemo
import dev.irisui.catalog.demos.TouchGlowDemo
import dev.irisui.navigation.IrisPageHost
import dev.irisui.navigation.PageStack
import dev.irisui.navigation.rememberPageStack
import dev.irisui.ui.IrisApp

sealed interface CatalogPage {
    data object Home : CatalogPage
    data class Demo(val destination: CatalogDestination) : CatalogPage
    data class Detail(val depth: Int) : CatalogPage
}

/** [showTouches] draws a circle under the finger (see [TouchIndicator]); it is only for screen recordings. */
@Composable
fun CatalogApp(start: CatalogDestination? = null, darkTheme: Boolean? = null, showTouches: Boolean = false) {
    if (showTouches) TouchIndicator { CatalogAppContent(start, darkTheme) } else CatalogAppContent(start, darkTheme)
}

@Composable
private fun CatalogAppContent(start: CatalogDestination?, darkTheme: Boolean?) {
    IrisApp(darkTheme = darkTheme ?: androidx.compose.foundation.isSystemInDarkTheme()) {
        val stack: PageStack<CatalogPage> = rememberPageStack(CatalogPage.Home)
        LaunchedEffect(Unit) {
            if (start != null && stack.depth == 1) stack.push(CatalogPage.Demo(start))
        }
        IrisPageHost(stack) { page ->
            when (page) {
                CatalogPage.Home -> HomeScreen(onOpen = { stack.push(CatalogPage.Demo(it)) })
                is CatalogPage.Demo -> DemoScreen(
                    destination = page.destination,
                    onBack = { stack.pop() },
                    onPushDetail = { stack.push(CatalogPage.Detail(stack.depth)) },
                )
                is CatalogPage.Detail -> DetailPage(
                    depth = page.depth,
                    onBack = { stack.pop() },
                    onPushDetail = { stack.push(CatalogPage.Detail(stack.depth)) },
                )
            }
        }
    }
}

@Composable
private fun DemoScreen(destination: CatalogDestination, onBack: () -> Unit, onPushDetail: () -> Unit) {
    when (destination) {
        CatalogDestination.BottomNavPill -> BottomNavPillDemo(onBack)
        CatalogDestination.TopBar -> TopBarDemo(onBack)
        CatalogDestination.LargeTitle -> LargeTitleDemo(onBack)
        CatalogDestination.SegmentedTabs -> SegmentedTabsDemo(onBack)
        CatalogDestination.PageTransitions -> PageTransitionsDemo(onBack, onPushDetail)

        CatalogDestination.PrimaryButton -> PrimaryButtonDemo(onBack)
        CatalogDestination.SecondaryButton -> SecondaryButtonDemo(onBack)
        CatalogDestination.IconButton -> IconButtonDemo(onBack)
        CatalogDestination.RoundPlusButton -> RoundPlusButtonDemo(onBack)
        CatalogDestination.TextButton -> TextButtonDemo(onBack)

        CatalogDestination.Switch -> SwitchDemo(onBack)
        CatalogDestination.TextField -> TextFieldDemo(onBack)
        CatalogDestination.SearchBar -> SearchBarDemo(onBack)
        CatalogDestination.Checkbox -> CheckboxDemo(onBack)
        CatalogDestination.Slider -> SliderDemo(onBack)
        CatalogDestination.Progress -> ProgressDemo(onBack)

        CatalogDestination.Card -> CardDemo(onBack)
        CatalogDestination.ListItem -> ListItemDemo(onBack)
        CatalogDestination.GroupedList -> GroupedListDemo(onBack)
        CatalogDestination.SectionHeader -> SectionHeaderDemo(onBack)
        CatalogDestination.Divider -> DividerDemo(onBack)
        CatalogDestination.Badge -> BadgeDemo(onBack)

        CatalogDestination.Dialog -> DialogDemo(onBack)
        CatalogDestination.BottomSheet -> BottomSheetDemo(onBack)
        CatalogDestination.PopupMenu -> PopupMenuDemo(onBack)

        CatalogDestination.BlurLab -> BlurLabDemo(onBack)
        CatalogDestination.PressAnimation -> PressAnimationDemo(onBack)
        CatalogDestination.Haptics -> HapticsDemo(onBack)
        CatalogDestination.TouchGlow -> TouchGlowDemo(onBack)
        CatalogDestination.PersistentGlow -> PersistentGlowDemo(onBack)
        CatalogDestination.DragPhysics -> DragPhysicsDemo(onBack)

        CatalogDestination.PreferencesExample -> PreferencesExample(onBack)
        CatalogDestination.HabitsExample -> HabitsExample(onBack)
        CatalogDestination.TypeScale -> TypeScale(onBack)
    }
}
