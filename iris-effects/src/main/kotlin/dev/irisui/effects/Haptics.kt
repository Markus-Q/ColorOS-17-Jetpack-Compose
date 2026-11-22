package dev.irisui.effects

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/** Semantic haptic events. Components fire these; the engine maps them to the device motor. */
enum class IrisHaptic {
    /** Light tap on a button. */
    Tap,

    /** Fine tick, for example a wheel crossing a row. */
    Tick,

    /** A slider thumb crossing a step while it is dragged (the stock sliders fire constant 305 for every step). */
    SliderStep,

    /** A selection changed (tab, segment, reorder slot). */
    Selection,
    Confirm,
    Reject,
    ToggleOn,
    ToggleOff,
    DragStart,
    DragEnd,
    LongPress,
    HeavyClick,
    Thud,
    Pop,
    DoubleClick,
}

/**
 * The haptic constants of ColorOS, accepted by [View.performHapticFeedback] on ColorOS / OxygenOS devices. Which one
 * the stock apps use where was read from the vibrator log of a OnePlus 15: switch toggle and round add button 302,
 * number picker steps 307; sliders fire 305 per step.
 */
object IrisHapticConstants {
    const val LONG_VIBRATE = 300
    const val KEYBOARD_TOUCH_FEEDBACK = 301
    const val GRANULAR_SHORT_VIBRATE = 302
    const val SHORT_VIBRATE = 303
    const val SHORT_TRIPLE_VIBRATE = 304
    const val STEPPING_SHORT_VIBRATE = 305
    const val EDGE_SHORT_VIBRATE = 306
    const val EDGE_LIST_VIBRATE = 307
    const val GRANULAR_SHORT_VIBRATE_SYNC = 308
    const val EDGE_SQUEEZE = 14
}

/** Where haptics come from. */
enum class HapticSource {
    /**
     * The ColorOS constants through [View.performHapticFeedback] (what the stock apps do); on devices of other makers this
     * falls back to [System].
     */
    ColorOS,

    /** [View.performHapticFeedback]: follows the system "touch feedback" setting and OEM tuning. */
    System,

    /** Predefined [VibrationEffect]s sent straight to the vibrator (CLICK, TICK, HEAVY_CLICK, THUD, POP...). */
    Direct,
}

/**
 * Plays [IrisHaptic] events on the device. [enabled] switches all of them off, [source] chooses how they reach the vibration motor.
 */
@Stable
interface IrisHaptics {
    var enabled: Boolean
    var source: HapticSource
    fun perform(event: IrisHaptic)
}

/** Haptics that do nothing: the default outside `IrisApp`, and what previews and tests use. */
object NoOpHaptics : IrisHaptics {
    override var enabled: Boolean = false
    override var source: HapticSource = HapticSource.System
    override fun perform(event: IrisHaptic) = Unit
}

/**
 * The haptics the components fire their events through: [NoOpHaptics] unless `IrisApp` or your own provider supplies real ones.
 */
val LocalColorOSHaptics = staticCompositionLocalOf<IrisHaptics> { NoOpHaptics }

/**
 * Creates [AndroidColorOSHaptics] for the current view and remembers it. `IrisApp` does this and provides the result through
 * [LocalColorOSHaptics].
 */
@Composable
fun rememberColorOSHaptics(): IrisHaptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view) { AndroidColorOSHaptics(view, context.applicationContext) }
}

/**
 * [IrisHaptics] on the vibrator of the device. [source] decides how an event is played first (the ColorOS constants on ColorOS / OxygenOS,
 * standard `performHapticFeedback` constants, or predefined `VibrationEffect`s); when that is not possible the next way is tried.
 * The source starts as the ColorOS constants on OPPO / OnePlus / realme devices and as the standard constants elsewhere.
 */
@SuppressLint("InlinedApi", "NewApi")
class AndroidColorOSHaptics(
    private val view: View,
    context: Context,
) : IrisHaptics {
    override var enabled: Boolean by mutableStateOf(true)
    override var source: HapticSource by mutableStateOf(if (isOplusDevice()) HapticSource.ColorOS else HapticSource.System)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override fun perform(event: IrisHaptic) {
        if (!enabled) return
        when (source) {
            HapticSource.ColorOS ->
                if (!(isOplusDevice() && performColorOS(event))) if (!performSystem(event)) performDirect(event)
            HapticSource.System -> if (!performSystem(event)) performDirect(event)
            HapticSource.Direct -> if (!performDirect(event)) performSystem(event)
        }
    }

    /** The events the stock apps really fire, with the constants they use. Everything else stays silent on purpose. */
    private fun performColorOS(event: IrisHaptic): Boolean {
        val constant = when (event) {
            IrisHaptic.Tap, IrisHaptic.ToggleOn, IrisHaptic.ToggleOff, IrisHaptic.Confirm ->
                IrisHapticConstants.GRANULAR_SHORT_VIBRATE
            IrisHaptic.Tick, IrisHaptic.Selection -> IrisHapticConstants.EDGE_LIST_VIBRATE
            IrisHaptic.SliderStep -> IrisHapticConstants.STEPPING_SHORT_VIBRATE
            else -> return false
        }
        return view.performHapticFeedback(constant)
    }

    private fun performSystem(event: IrisHaptic): Boolean {
        val sdk = Build.VERSION.SDK_INT
        val constant = when (event) {
            IrisHaptic.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
            IrisHaptic.Tick, IrisHaptic.SliderStep -> HapticFeedbackConstants.CLOCK_TICK
            IrisHaptic.Selection ->
                if (sdk >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK
            IrisHaptic.Confirm ->
                if (sdk >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK
            IrisHaptic.Reject ->
                if (sdk >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
            IrisHaptic.ToggleOn ->
                if (sdk >= 34) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.CONTEXT_CLICK
            IrisHaptic.ToggleOff ->
                if (sdk >= 34) HapticFeedbackConstants.TOGGLE_OFF else HapticFeedbackConstants.CONTEXT_CLICK
            IrisHaptic.DragStart ->
                if (sdk >= 34) HapticFeedbackConstants.DRAG_START else HapticFeedbackConstants.LONG_PRESS
            IrisHaptic.DragEnd ->
                if (sdk >= 30) HapticFeedbackConstants.GESTURE_END else HapticFeedbackConstants.KEYBOARD_RELEASE
            IrisHaptic.LongPress -> HapticFeedbackConstants.LONG_PRESS
            // No view constant exists for these; they only work through the direct source.
            IrisHaptic.HeavyClick, IrisHaptic.Thud, IrisHaptic.Pop, IrisHaptic.DoubleClick -> return false
        }
        return view.performHapticFeedback(constant)
    }

    private fun performDirect(event: IrisHaptic): Boolean {
        val v = vibrator ?: return false
        if (Build.VERSION.SDK_INT < 29 || !v.hasVibrator()) return false
        val effectId = when (event) {
            IrisHaptic.Tap -> VibrationEffect.EFFECT_TICK
            IrisHaptic.Tick, IrisHaptic.SliderStep -> VibrationEffect.EFFECT_TICK
            IrisHaptic.Selection -> VibrationEffect.EFFECT_TICK
            IrisHaptic.Confirm -> VibrationEffect.EFFECT_CLICK
            IrisHaptic.Reject -> VibrationEffect.EFFECT_DOUBLE_CLICK
            IrisHaptic.ToggleOn -> VibrationEffect.EFFECT_CLICK
            IrisHaptic.ToggleOff -> VibrationEffect.EFFECT_TICK
            IrisHaptic.DragStart -> EFFECT_POP
            IrisHaptic.DragEnd -> EFFECT_THUD
            IrisHaptic.LongPress -> VibrationEffect.EFFECT_HEAVY_CLICK
            IrisHaptic.HeavyClick -> VibrationEffect.EFFECT_HEAVY_CLICK
            IrisHaptic.Thud -> EFFECT_THUD
            IrisHaptic.Pop -> EFFECT_POP
            IrisHaptic.DoubleClick -> VibrationEffect.EFFECT_DOUBLE_CLICK
        }
        return try {
            val effect = VibrationEffect.createPredefined(effectId)
            if (Build.VERSION.SDK_INT >= 33) {
                v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
            } else {
                v.vibrate(effect)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    private companion object {
        // Platform effect ids that exist on the device HAL but are hidden from the public SDK.
        const val EFFECT_THUD = 3
        const val EFFECT_POP = 4
    }
}

/** True on ColorOS / OxygenOS devices (OPPO, OnePlus, realme), which accept the ColorOS haptic constants. */
fun isOplusDevice(): Boolean {
    val maker = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
    return "oneplus" in maker || "oppo" in maker || "realme" in maker
}
