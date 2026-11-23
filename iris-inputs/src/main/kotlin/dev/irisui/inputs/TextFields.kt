package dev.irisui.inputs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.irisui.core.IrisIcon
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisSettingsBlue
import dev.irisui.core.IrisText
import dev.irisui.core.IrisTheme
import dev.irisui.effects.GlassDefaults
import dev.irisui.effects.irisClickable
import dev.irisui.effects.glass
import dev.irisui.effects.touchMotion

/** Filled text field with a rounded container. The border lights up in the accent color on focus. */
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
) {
    val colors = IrisTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderColor = when {
        isError -> colors.destructive
        focused -> colors.accent
        else -> colors.content.copy(alpha = 0f)
    }
    Column(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f)) {
        if (label != null) {
            IrisText(
                label,
                Modifier.padding(start = 4.dp, bottom = 6.dp),
                style = IrisTheme.typography.sectionHeader,
                color = if (isError) colors.destructive else colors.contentSecondary,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = IrisTheme.typography.body.merge(color = colors.content),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clip(IrisTheme.shapes.field)
                        .background(colors.field)
                        .border(1.5.dp, borderColor, IrisTheme.shapes.field)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (leadingIcon != null) IrisIcon(leadingIcon, null, tint = colors.contentSecondary, size = 22.dp)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            IrisText(placeholder, style = IrisTheme.typography.body, color = colors.contentTertiary)
                        }
                        inner()
                    }
                }
            },
        )
        if (supportingText != null) {
            IrisText(
                supportingText,
                Modifier.padding(start = 4.dp, top = 6.dp),
                style = IrisTheme.typography.caption,
                color = if (isError) colors.destructive else colors.contentSecondary,
            )
        }
    }
}

/**
 * Text field of the stock dialogs (the "Change device name" dialog of Settings): the text sits on a bare 48 dp row with
 * a 1 dp underline that turns blue while the field has focus. While editing, a character counter ("10/32") and a round
 * clear button appear at the end of the row.
 *
 * With [maxLength] input beyond the limit is cut off and the counter is shown. Pass a [focusRequester] to focus the
 * field as soon as its dialog opens.
 */
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
) {
    val colors = IrisTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val lineColor = if (focused) (if (colors.isDark) IrisSettingsBlue else colors.accent) else colors.divider
    Column(modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.4f)) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(if (maxLength != null) it.take(maxLength) else it) },
            enabled = enabled,
            singleLine = singleLine,
            textStyle = IrisTheme.typography.menuItem.copy(fontSize = 16.sp).merge(color = colors.content),
            cursorBrush = SolidColor(if (colors.isDark) IrisSettingsBlue else colors.accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interaction,
            modifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier,
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp).padding(end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            IrisText(placeholder, style = IrisTheme.typography.menuItem.copy(fontSize = 16.sp), color = colors.contentTertiary)
                        }
                        inner()
                    }
                    if (focused && value.isNotEmpty()) {
                        if (maxLength != null) {
                            IrisText(
                                "${value.length}/$maxLength",
                                Modifier.padding(start = 8.dp),
                                style = IrisTheme.typography.caption,
                                color = colors.contentSecondary,
                            )
                        }
                        Box(
                            Modifier
                                .padding(start = 10.dp)
                                .size(18.dp)
                                .clip(IrisTheme.shapes.pill)
                                .background(colors.content.copy(alpha = 0.6f))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    onValueChange("")
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            IrisIcon(IrisIcons.Close, "Clear", tint = colors.background, size = 10.dp)
                        }
                    }
                }
            },
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(lineColor))
    }
}

/**
 * Surface of the search field and its Cancel pill. Measured on Settings: #1F1F1F fill, a rim light at the top (15 %
 * white) and a weaker one at the bottom (11 %) that fades out towards the sides.
 */
@Composable
private fun searchSurface(shape: Shape, glass: Boolean): Modifier {
    val colors = IrisTheme.colors
    return if (glass) {
        // The search bar material is the top bar colour layers without any blur.
        Modifier.glass(shape, GlassDefaults.searchBar())
    } else {
        Modifier
            .background(colors.field, shape)
            .border(
                1.dp,
                Brush.verticalGradient(
                    0f to colors.content.copy(alpha = 0.15f),
                    0.5f to colors.content.copy(alpha = 0f),
                    1f to colors.content.copy(alpha = 0.11f),
                ),
                shape,
            )
    }
}

/**
 * Pill-shaped search bar. Set [glass] to blur the content behind it when it floats over a list.
 * While it has focus the field narrows and a "Cancel" pill of the same material slides in beside it, like the search
 * page of Settings (field 728 px and pill 225 px wide at 480 dpi, 31 px apart).
 */
@Composable
fun IrisSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: ((String) -> Unit)? = null,
    glass: Boolean = false,
    showCancel: Boolean = true,
) {
    val colors = IrisTheme.colors
    val shape = IrisTheme.shapes.pill
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = IrisTheme.typography.body.merge(color = colors.content),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onSearch?.invoke(query)
                focusManager.clearFocus()
            }),
            interactionSource = interaction,
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .then(searchSurface(shape, glass))
                        .clip(shape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            focusRequester.requestFocus()
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IrisIcon(IrisIcons.Search, null, tint = colors.contentSecondary, size = 20.dp)
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            IrisText(placeholder, style = IrisTheme.typography.body, color = colors.contentSecondary)
                        }
                        inner()
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(shape)
                                .background(colors.contentTertiary)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    onQueryChange("")
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            IrisIcon(IrisIcons.Close, "Clear", tint = colors.background, size = 12.dp)
                        }
                    }
                }
            },
        )
        val slide = spring<IntSize>(dampingRatio = 1f, stiffness = (2.0 * Math.PI / 0.3).let { (it * it).toFloat() })
        AnimatedVisibility(
            visible = showCancel && focused,
            enter = expandHorizontally(slide) + fadeIn(tween(150)),
            exit = shrinkHorizontally(slide) + fadeOut(tween(120)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .height(40.dp)
                        .touchMotion(shape = shape, highlight = colors.content)
                        .then(searchSurface(shape, glass))
                        .clip(shape)
                        .irisClickable(
                            onClick = {
                                onQueryChange("")
                                focusManager.clearFocus()
                            },
                        )
                        .padding(horizontal = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    IrisText("Cancel", style = IrisTheme.typography.menuItem, color = colors.content, maxLines = 1)
                }
            }
        }
    }
}
