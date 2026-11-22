package dev.irisui.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.irisui.core.IrisColors
import dev.irisui.core.IrisTheme
import dev.irisui.core.irisDarkColors
import dev.irisui.core.irisLightColors
import dev.irisui.effects.IrisOverscrollFactory
import dev.irisui.effects.LocalColorOSHaptics
import dev.irisui.effects.rememberColorOSHaptics
import dev.irisui.popups.IrisPopupHost

/**
 * One-stop root for an app that uses the whole kit: theme, haptics engine and the popup layer
 * (dialogs, sheets, menus). Pages themselves are built with the scaffolds from the navigation module.
 *
 * It also tells the system bars which icon color to use, so the clock and the battery stay readable when the theme is
 * forced to light on a dark system (or the other way round).
 *
 * [overscrollFactory] is what every list, row and pager of the app does at its end: the translating bounce of the stock
 * apps by default, `null` for no overscroll at all, or any other factory (for instance the Android stretch).
 */
@Composable
fun IrisApp(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    colors: IrisColors = if (darkTheme) irisDarkColors() else irisLightColors(),
    overscrollFactory: OverscrollFactory? = IrisOverscrollFactory,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    IrisTheme(darkTheme = darkTheme, colors = colors) {
        val haptics = rememberColorOSHaptics()
        CompositionLocalProvider(
            LocalColorOSHaptics provides haptics,
            LocalOverscrollFactory provides overscrollFactory,
        ) {
            IrisPopupHost(modifier, content)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
