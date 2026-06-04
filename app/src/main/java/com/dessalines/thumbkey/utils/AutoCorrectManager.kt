package com.dessalines.thumbkey.utils

import android.content.Context
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager

class AutoCorrectManager(
    private val context: Context,
) : SpellCheckerSession.SpellCheckerSessionListener {

    private var session: SpellCheckerSession? = null
    private var pendingWord: String? = null
    private var pendingCallback: ((String) -> Unit)? = null

    fun init() {
        try {
            val tsm = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)
                as TextServicesManager
            session = tsm.newSpellCheckerSession(null, null, this, true)
        } catch (e: Exception) {
            Log.e(TAG, "AutoCorrectManager: failed to init spell checker: $e")
        }
    }

    fun destroy() {
        session?.close()
        session = null
    }

    fun checkWord(
        word: String,
        onSuggestion: (corrected: String) -> Unit,
    ) {
        val s = session ?: return
        if (word.length < 2) return
        pendingWord = word
        pendingCallback = onSuggestion
        try {
            s.getSuggestions(TextInfo(word), 3)
        } catch (e: Exception) {
            Log.e(TAG, "AutoCorrectManager: getSuggestions error: $e")
            pendingWord = null
            pendingCallback = null
        }
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        val word = pendingWord ?: return
        val callback = pendingCallback ?: return
        pendingWord = null
        pendingCallback = null

        val info = results?.firstOrNull() ?: return
        val isTypo = (info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0
        if (isTypo && info.suggestionsCount > 0) {
            val suggestion = SpellCheckerHelper.getFirstSuggestion(info) ?: return
            if (suggestion.isNotEmpty() && suggestion != word) {
                callback(suggestion)
            }
        }
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {}
}
