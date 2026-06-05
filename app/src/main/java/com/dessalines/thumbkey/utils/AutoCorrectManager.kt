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

    private var lastCheckedWord = ""
    private var resultArrived = false
    private var lastWordIsTypo = false

    // Stored when the separator arrives before the async spell-check result.
    private var pendingAutocorrect: PendingAutocorrect? = null

    private data class PendingAutocorrect(
        val word: String,
        val separator: String,
        val apply: (correction: String, separator: String) -> Unit,
    )

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestionsFlow: StateFlow<List<String>> = _suggestions.asStateFlow()

    fun init() {
        try {
            val tsm = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)
                as TextServicesManager
            session = tsm.newSpellCheckerSession(null, null, this, true)
            Log.d(TAG, "AutoCorrectManager: session=${session != null}")
        } catch (e: Exception) {
            Log.e(TAG, "AutoCorrectManager: init failed: $e")
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
        resultArrived = false
        try {
            s.getSuggestions(TextInfo(word), 3)
        } catch (e: Exception) {
            Log.e(TAG, "AutoCorrectManager: getSuggestions error: $e")
            resultArrived = true
        }
    }

    fun clearSuggestions() {
        pendingAutocorrect = null
        lastCheckedWord = ""
        resultArrived = false
        lastWordIsTypo = false
        _suggestions.value = emptyList()
    }

    /**
     * Called when a word separator is typed. If the spell-check result is already available,
     * applies the correction immediately. If not, stores the callback to fire when the result
     * arrives (handles fast typists where the async result lags behind).
     */
    fun scheduleAutoCorrect(
        word: String,
        separator: String,
        apply: (correction: String, separator: String) -> Unit,
    ) {
        pendingAutocorrect = null
        if (word.length < 2 || word != lastCheckedWord) return

        if (resultArrived) {
            if (lastWordIsTypo) {
                val correction = _suggestions.value.firstOrNull()
                clearSuggestions()
                if (correction != null && correction != word) apply(correction, separator)
            } else {
                clearSuggestions()
            }
        } else {
            // Spell checker hasn't responded yet — defer until onGetSuggestions fires.
            pendingAutocorrect = PendingAutocorrect(word, separator, apply)
        }
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        resultArrived = true
        val info = results?.firstOrNull() ?: run {
            _suggestions.value = emptyList()
            lastWordIsTypo = false
            dispatchPending()
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
        dispatchPending()
    }

    private fun dispatchPending() {
        val pending = pendingAutocorrect ?: return
        pendingAutocorrect = null
        if (pending.word != lastCheckedWord) return
        if (lastWordIsTypo) {
            val correction = _suggestions.value.firstOrNull()
            clearSuggestions()
            if (correction != null && correction != pending.word) {
                pending.apply(correction, pending.separator)
            }
        } else {
            clearSuggestions()
        }
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {}
}
