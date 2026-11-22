package dev.irisui.catalog.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.irisui.buttons.IrisIconButton
import dev.irisui.buttons.IrisPrimaryButton
import dev.irisui.buttons.IrisRoundPlusButton
import dev.irisui.buttons.IrisSecondaryButton
import dev.irisui.buttons.IrisTextButton
import dev.irisui.buttons.IconButtonStyle
import dev.irisui.core.IrisIcons
import dev.irisui.core.IrisTheme
import dev.irisui.effects.backdropSource
import dev.irisui.effects.rememberBackdrop

@Composable
fun PrimaryButtonDemo(onBack: () -> Unit) {
    DemoPage("Primary button", onBack, subtitle = "Accent pill with a glow that is always on") {
        demoSection("Default") {
            IrisPrimaryButton("Continue", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
        demoSection("With icon") {
            IrisPrimaryButton("Add alarm", onClick = {}, modifier = Modifier.fillMaxWidth(), leadingIcon = IrisIcons.Plus)
        }
        demoSection("Colors") {
            IrisPrimaryButton("Green", onClick = {}, modifier = Modifier.fillMaxWidth(), color = Color(0xFF34C759))
            IrisPrimaryButton("Orange", onClick = {}, modifier = Modifier.fillMaxWidth(), color = Color(0xFFFF9F0A))
            IrisPrimaryButton("Red", onClick = {}, modifier = Modifier.fillMaxWidth(), color = IrisTheme.colors.destructive)
        }
        demoSection("Disabled, without glow") {
            IrisPrimaryButton("Disabled", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
            IrisPrimaryButton("No glow", onClick = {}, modifier = Modifier.fillMaxWidth(), glow = false)
        }
    }
}

@Composable
fun SecondaryButtonDemo(onBack: () -> Unit) {
    DemoPage("Secondary button", onBack, subtitle = "Quiet pill for the second choice") {
        demoSection("Default") {
            IrisSecondaryButton("Cancel", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
        demoSection("With icon") {
            IrisSecondaryButton("Sort", onClick = {}, modifier = Modifier.fillMaxWidth(), leadingIcon = IrisIcons.Tune)
        }
        demoSection("Next to a primary button") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IrisSecondaryButton("Later", onClick = {}, modifier = Modifier.weight(1f))
                IrisPrimaryButton("Allow", onClick = {}, modifier = Modifier.weight(1f))
            }
        }
        demoSection("Disabled") {
            IrisSecondaryButton("Disabled", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
        }
    }
}

@Composable
fun IconButtonDemo(onBack: () -> Unit) {
    DemoPage("Icon button", onBack, subtitle = "Round glass, filled and plain") {
        demoSection("Glass over the page") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IrisIconButton(IrisIcons.Search, onClick = {})
                IrisIconButton(IrisIcons.More, onClick = {})
                IrisIconButton(IrisIcons.Bell, onClick = {})
                IrisIconButton(IrisIcons.Heart, onClick = {})
            }
        }
        demoSection("Glass over a wallpaper") {
            val backdrop = rememberBackdrop()
            Box(Modifier.fillMaxWidth().height(150.dp)) {
                DemoWallpaper(Modifier.fillMaxWidth().height(150.dp).backdropSource(backdrop))
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A button sampling the wallpaper through its own backdrop.
                    androidx.compose.runtime.CompositionLocalProvider(dev.irisui.effects.LocalBackdrop provides backdrop) {
                        IrisIconButton(IrisIcons.Search, onClick = {}, size = 48.dp)
                        IrisIconButton(IrisIcons.Star, onClick = {}, size = 48.dp)
                        IrisIconButton(IrisIcons.Alarm, onClick = {}, size = 48.dp)
                    }
                }
            }
        }
        demoSection("Filled") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IrisIconButton(IrisIcons.Plus, onClick = {}, style = IconButtonStyle.Filled)
                IrisIconButton(IrisIcons.Check, onClick = {}, style = IconButtonStyle.Filled, size = 48.dp)
                IrisIconButton(IrisIcons.Close, onClick = {}, style = IconButtonStyle.Filled, size = 56.dp)
            }
        }
        demoSection("Plain, disabled") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                IrisIconButton(IrisIcons.Trash, onClick = {}, style = IconButtonStyle.Plain)
                IrisIconButton(IrisIcons.Trash, onClick = {}, enabled = false)
            }
        }
    }
}

@Composable
fun RoundPlusButtonDemo(onBack: () -> Unit) {
    DemoPage("Round plus button", onBack, subtitle = "Press and hold: it shrinks. Drag: it stretches") {
        demoSection("Accent") {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                IrisRoundPlusButton(onClick = {})
            }
        }
        demoSection("Colors") {
            Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IrisRoundPlusButton(onClick = {}, color = Color(0xFFFFB800))
                IrisRoundPlusButton(onClick = {}, color = Color(0xFF34C759))
                IrisRoundPlusButton(onClick = {}, color = Color(0xFFFF375F))
            }
        }
        demoSection("Sizes") {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IrisRoundPlusButton(onClick = {}, size = 44.dp)
                IrisRoundPlusButton(onClick = {}, size = 56.dp)
                IrisRoundPlusButton(onClick = {}, size = 72.dp, icon = IrisIcons.Alarm)
            }
        }
    }
}

@Composable
fun TextButtonDemo(onBack: () -> Unit) {
    var count by remember { mutableIntStateOf(0) }
    DemoPage("Text button", onBack, subtitle = "Accent text, lights up while pressed") {
        demoSection("Default") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IrisTextButton("Edit", onClick = { count++ })
                IrisTextButton("Select all", onClick = { count++ })
                IrisTextButton("Done", onClick = { count++ })
            }
            Caption("Taps: $count")
        }
        demoSection("Colors, disabled") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IrisTextButton("Delete", onClick = {}, color = IrisTheme.colors.destructive)
                IrisTextButton("Neutral", onClick = {}, color = IrisTheme.colors.content)
                IrisTextButton("Disabled", onClick = {}, enabled = false)
            }
        }
    }
}
