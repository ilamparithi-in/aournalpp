package dev.ilamparithi.aournalpp.data

import android.content.Context
import android.content.Intent
object X11Preferences {

    const val ACTION_PREFERENCES_CHANGED = "com.termux.x11.ACTION_PREFERENCES_CHANGED"

    // Output / Display
    const val KEY_DISPLAY_RES_MODE = "displayResolutionMode"
    const val KEY_DISPLAY_SCALE = "displayScale"
    const val KEY_DISPLAY_RES_EXACT = "displayResolutionExact"
    const val KEY_DISPLAY_RES_CUSTOM = "displayResolutionCustom"
    const val KEY_DISPLAY_FILTERING = "displayFilteringMode"
    const val KEY_ADJUST_RESOLUTION = "adjustResolution"
    const val KEY_DISPLAY_STRETCH = "displayStretch"
    const val KEY_RESEED = "Reseed"
    const val KEY_FULLSCREEN = "fullscreen"
    const val KEY_SCREEN_IDLE_TIMEOUT = "screenIdleTimeout"

    // Pointer / Stylus
    const val KEY_TOUCH_MODE = "touchMode"
    const val KEY_SCALE_TOUCHPAD = "scaleTouchpad"
    const val KEY_SHOW_STYLUS_CLICK_OVERRIDE = "showStylusClickOverride"
    const val KEY_STYLUS_IS_MOUSE = "stylusIsMouse"
    const val KEY_STYLUS_BUTTON_CONTACT_MODIFIER = "stylusButtonContactModifierMode"
    const val KEY_SHOW_MOUSE_HELPER = "showMouseHelper"
    const val KEY_TRANSFORM_CAPTURED_POINTER = "transformCapturedPointer"
    const val KEY_CAPTURED_POINTER_SPEED = "capturedPointerSpeedFactor"
    const val KEY_TAP_TO_MOVE = "tapToMove"
    const val KEY_IGNORE_GAMEPAD_EVENTS = "ignoreGamepadEvents"

    // Keyboard
    const val KEY_ENFORCE_CHAR_BASED_INPUT = "enforceCharBasedInput"

    // Clipboard
    const val KEY_CLIPBOARD_ENABLE = "clipboardEnable"

    // Lenovo Pen Mapping
    const val KEY_LENOVO_PEN_SHOW_DETECTIONS = "lenovoPenShowDetections"
    const val KEY_LENOVO_PEN_DEBUG_TOGGLE_TOASTS = "lenovoPenDebugToggleToasts"

    const val KEY_LENOVO_PEN_SINGLE_ACTION = "lenovoPenSinglePressAction"
    const val KEY_LENOVO_PEN_SINGLE_TOGGLE = "lenovoPenSinglePressToggle"
    const val KEY_LENOVO_PEN_SINGLE_OFF_ON_LIFT = "lenovoPenSinglePressToggleOffOnLift"
    const val KEY_LENOVO_PEN_SINGLE_DURATION = "lenovoPenSinglePressDurationMs"

    const val KEY_LENOVO_PEN_DOUBLE_ACTION = "lenovoPenDoublePressAction"
    const val KEY_LENOVO_PEN_DOUBLE_TOGGLE = "lenovoPenDoublePressToggle"
    const val KEY_LENOVO_PEN_DOUBLE_OFF_ON_LIFT = "lenovoPenDoublePressToggleOffOnLift"
    const val KEY_LENOVO_PEN_DOUBLE_DURATION = "lenovoPenDoublePressDurationMs"

    const val KEY_LENOVO_PEN_TRIPLE_ACTION = "lenovoPenTriplePressAction"
    const val KEY_LENOVO_PEN_TRIPLE_TOGGLE = "lenovoPenTriplePressToggle"
    const val KEY_LENOVO_PEN_TRIPLE_OFF_ON_LIFT = "lenovoPenTriplePressToggleOffOnLift"
    const val KEY_LENOVO_PEN_TRIPLE_DURATION = "lenovoPenTriplePressDurationMs"

    const val KEY_LENOVO_PEN_LONG_ACTION = "lenovoPenLongPressAction"
    const val KEY_LENOVO_PEN_LONG_TOGGLE = "lenovoPenLongPressToggle"
    const val KEY_LENOVO_PEN_LONG_OFF_ON_LIFT = "lenovoPenLongPressToggleOffOnLift"
    const val KEY_LENOVO_PEN_LONG_DURATION = "lenovoPenLongPressDurationMs"

    const val KEY_LENOVO_PEN_LONG_CLICK_ACTION = "lenovoPenLongPressClickAction"
    const val KEY_LENOVO_PEN_LONG_CLICK_TOGGLE = "lenovoPenLongPressClickToggle"
    const val KEY_LENOVO_PEN_LONG_CLICK_OFF_ON_LIFT = "lenovoPenLongPressClickToggleOffOnLift"
    const val KEY_LENOVO_PEN_LONG_CLICK_DURATION = "lenovoPenLongPressClickDurationMs"

    fun getPrefs(context: Context): android.content.SharedPreferences {
        return context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    }

    fun initDefaults(context: Context) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        var changed = false

        // Default touch mode to Direct Touch ("3") for new installs
        if (!prefs.contains(KEY_TOUCH_MODE)) {
            editor.putString(KEY_TOUCH_MODE, "3")
            changed = true
        }

        // Always enable clipboard
        if (!prefs.contains(KEY_CLIPBOARD_ENABLE) || !prefs.getBoolean(KEY_CLIPBOARD_ENABLE, true)) {
            editor.putBoolean(KEY_CLIPBOARD_ENABLE, true)
            changed = true
        }

        if (changed) {
            editor.apply()
        }
    }

    fun notifyChanged(context: Context, key: String) {
        try {
            val intent = Intent(ACTION_PREFERENCES_CHANGED).apply {
                putExtra("key", key)
                putExtra("fromBroadcast", true)
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }
}
