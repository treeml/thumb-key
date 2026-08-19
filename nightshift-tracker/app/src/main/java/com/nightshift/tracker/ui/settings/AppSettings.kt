package com.nightshift.tracker.ui.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Device-local preferences. Deliberately tiny and synchronous — these are read
 * during composition on every screen, so they must never be a suspend call.
 */
object AppSettings {
    private const val FILE = "app_settings"
    private const val KEY_LEFT = "left_handed"
    private const val KEY_TEXT = "large_text"
    private const val KEY_NIGHT = "night_vision"
    private const val KEY_HAPTIC = "haptics"

    val leftHanded = MutableStateFlow(false)
    val largeText = MutableStateFlow(false)
    val nightVision = MutableStateFlow(false)
    val haptics = MutableStateFlow(true)

    fun load(context: Context) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        leftHanded.value = p.getBoolean(KEY_LEFT, false)
        largeText.value = p.getBoolean(KEY_TEXT, false)
        nightVision.value = p.getBoolean(KEY_NIGHT, false)
        haptics.value = p.getBoolean(KEY_HAPTIC, true)
    }

    private fun put(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply()
    }

    fun setLeftHanded(context: Context, value: Boolean) {
        leftHanded.value = value
        put(context, KEY_LEFT, value)
    }

    fun setLargeText(context: Context, value: Boolean) {
        largeText.value = value
        put(context, KEY_TEXT, value)
    }

    fun setNightVision(context: Context, value: Boolean) {
        nightVision.value = value
        put(context, KEY_NIGHT, value)
    }

    fun setHaptics(context: Context, value: Boolean) {
        haptics.value = value
        put(context, KEY_HAPTIC, value)
    }
}

/**
 * True when the user holds the phone in their left hand. Primary actions move
 * to the left edge (inside the thumb arc) and destructive ones move away from
 * it — the mirror image of the right-handed layout, not a shrunken version.
 */
@Composable
fun leftHanded(): Boolean {
    val value by AppSettings.leftHanded.collectAsStateWithLifecycle()
    return value
}
