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
    private const val KEY_GROUP_BED = "group_jobs_by_bed"
    private const val KEY_ONDEVICE_VOICE = "on_device_voice"

    val leftHanded = MutableStateFlow(false)
    val largeText = MutableStateFlow(false)
    val nightVision = MutableStateFlow(false)
    val haptics = MutableStateFlow(true)

    /** Jobs tab: group under bed headings instead of a flat urgency list. */
    val groupJobsByBed = MutableStateFlow(false)

    /**
     * Ask the speech recogniser to stay on the device.
     *
     * On by default. It is a request, not a guarantee — Android decides, and a
     * phone with no offline model may refuse to listen at all, which is why
     * this can be turned off. With it off, dictated words go to Google's speech
     * service like any other keyboard mic would send them.
     */
    val onDeviceVoice = MutableStateFlow(true)

    fun load(context: Context) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        leftHanded.value = p.getBoolean(KEY_LEFT, false)
        largeText.value = p.getBoolean(KEY_TEXT, false)
        nightVision.value = p.getBoolean(KEY_NIGHT, false)
        haptics.value = p.getBoolean(KEY_HAPTIC, true)
        groupJobsByBed.value = p.getBoolean(KEY_GROUP_BED, false)
        onDeviceVoice.value = p.getBoolean(KEY_ONDEVICE_VOICE, true)
    }

    private fun put(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply()
    }

    fun setOnDeviceVoice(context: Context, value: Boolean) {
        onDeviceVoice.value = value
        put(context, KEY_ONDEVICE_VOICE, value)
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

    fun setGroupJobsByBed(context: Context, value: Boolean) {
        groupJobsByBed.value = value
        put(context, KEY_GROUP_BED, value)
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
