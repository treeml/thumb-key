package com.dessalines.thumbkey.utils

import android.content.Context
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutoCorrectManager(
    private val context: Context,
) : SpellCheckerSession.SpellCheckerSessionListener {

    private var session: SpellCheckerSession? = null
    private var lastCheckedWord: String = ""
    private var lastWordIsTypo: Boolean = false

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestionsFlow: StateFlow<List<String>> = _suggestions.asStateFlow()

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

    fun requestSuggestions(word: String) {
        if (word.length < 2) {
            _suggestions.value = emptyList()
            return
        }
        if (word == lastCheckedWord) return
        val s = session ?: return
        lastCheckedWord = word
        try {
            s.getSuggestions(TextInfo(word), 3)
        } catch (e: Exception) {
            Log.e(TAG, "AutoCorrectManager: getSuggestions error: $e")
        }
    }

    fun clearSuggestions() {
        lastCheckedWord = ""
        lastWordIsTypo = false
        _suggestions.value = emptyList()
    }

    // Returns the first correction only if the word was a typo, then clears state.
    fun consumeAutoCorrection(word: String): String? {
        if (word != lastCheckedWord || !lastWordIsTypo) {
            clearSuggestions()
            return null
        }
        val correction = _suggestions.value.firstOrNull()
        clearSuggestions()
        return correction
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        val info = results?.firstOrNull() ?: run {
            _suggestions.value = emptyList()
            return
        }
        val isTypo = (info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0
        lastWordIsTypo = isTypo
        if (isTypo) {
            val all = SpellCheckerHelper.getAllSuggestions(info)
            _suggestions.value = all.filter { it.isNotEmpty() }.take(3).toList()
        } else {
            _suggestions.value = emptyList()
        }
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {}
}
