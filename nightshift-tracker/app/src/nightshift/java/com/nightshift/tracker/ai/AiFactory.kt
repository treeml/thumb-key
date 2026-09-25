package com.nightshift.tracker.ai

import android.content.Context

/**
 * Nightshift build: there is no AI client and no network code in this APK.
 * The manifest also has no INTERNET permission, so the OS itself enforces it.
 */
object AiFactory {
    const val AVAILABLE = false

    fun create(context: Context): NoteTidier? = null
}
