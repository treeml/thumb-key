package com.dessalines.thumbkey.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
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

    private val localChecker by lazy { LocalSpellChecker(context) }
    private var session: SpellCheckerSession? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentWord = ""
    private var pendingAutocorrect: PendingAutocorrect? = null

    private data class PendingAutocorrect(
        val word: String,
        val separator: String,
        val apply: (correction: String, separator: String) -> Unit,
    )

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestionsFlow: StateFlow<List<String>> = _suggestions.asStateFlow()

    fun init() {
        // Try to get a system spell checker session for richer suggestions.
        // This is optional — LocalSpellChecker is always available as fallback.
        try {
            val tsm = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE)
                as TextServicesManager
            session = tsm.newSpellCheckerSession(null, null, this, true)
                ?: tsm.newSpellCheckerSession(null, null, this, false)
            Log.d(TAG, "AutoCorrectManager: system spell checker session=${session != null}")
        } catch (e: Exception) {
            Log.w(TAG, "AutoCorrectManager: system spell checker unavailable: $e")
        }
    }

    fun destroy() {
        session?.close()
        session = null
    }

    /**
     * Called after each letter is committed. Updates the suggestion bar
     * immediately using the local dictionary, and also fires off an async
     * request to the system spell checker if one is available.
     */
    fun requestSuggestions(word: String) {
        if (word.length < 2) {
            currentWord = word
            _suggestions.value = emptyList()
            return
        }
        if (word == currentWord) return
        currentWord = word

        // Immediate local suggestions — no async latency
        val localSuggestions = localChecker.getSuggestions(word)
        _suggestions.value = localSuggestions
        Log.d(TAG, "AutoCorrectManager: local '$word' → $localSuggestions")

        // Also ask the system spell checker (may provide better/richer results)
        session?.let { s ->
            try {
                s.getSentenceSuggestions(arrayOf(TextInfo(word)), 3)
            } catch (e: Exception) {
                try { s.getSuggestions(TextInfo(word), 3) } catch (e2: Exception) { /* ignore */ }
            }
        }
    }

    fun clearSuggestions() {
        pendingAutocorrect = null
        currentWord = ""
        _suggestions.value = emptyList()
    }

    /**
     * Called when a word separator is typed. Applies the best available
     * correction immediately (local result is always ready synchronously).
     */
    fun scheduleAutoCorrect(
        word: String,
        separator: String,
        apply: (correction: String, separator: String) -> Unit,
    ) {
        pendingAutocorrect = null
        if (word.length < 2) return

        // Never autocorrect contractions or words the local dictionary already recognises
        if (word.contains('\'') || localChecker.isCorrect(word)) {
            clearSuggestions()
            return
        }

        val suggestions = _suggestions.value
        if (suggestions.isNotEmpty()) {
            val correction = suggestions.first()
            // Safety guard: only apply if the correction shares the first letter.
            // This prevents wild substitutions like "adopt" → "coming".
            val sameFirstLetter = correction.firstOrNull()?.lowercaseChar() ==
                word.firstOrNull()?.lowercaseChar()
            clearSuggestions()
            if (correction != word && sameFirstLetter) apply(correction, separator)
        } else {
            clearSuggestions()
        }
    }

    // System spell checker callbacks — may improve on the local result
    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        val sentence = results?.firstOrNull() ?: return
        if (sentence.suggestionsCount == 0) return
        val wordInfo = sentence.getSuggestionsInfoAt(0) ?: return
        handleSystemResult(wordInfo)
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        val info = results?.firstOrNull() ?: return
        handleSystemResult(info)
    }

    private fun handleSystemResult(info: SuggestionsInfo) {
        if (info.suggestionsCount <= 0) return
        val isTypo = (info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0
        if (!isTypo) return
        val all = SpellCheckerHelper.getAllSuggestions(info)
        val suggestions = all.filter { it.isNotEmpty() }.take(3)
        if (suggestions.isEmpty()) return
        Log.d(TAG, "AutoCorrectManager: system suggestions for '$currentWord' → $suggestions")
        // Post to main thread since system callbacks may arrive on a binder thread
        mainHandler.post {
            if (_suggestions.value != suggestions) {
                _suggestions.value = suggestions
            }
            // Apply pending autocorrect if separator was typed before system result arrived
            val pending = pendingAutocorrect
            if (pending != null && pending.word == currentWord) {
                pendingAutocorrect = null
                val correction = suggestions.firstOrNull()
                val sameFirst = correction?.firstOrNull()?.lowercaseChar() ==
                    pending.word.firstOrNull()?.lowercaseChar()
                clearSuggestions()
                if (correction != null && correction != pending.word && sameFirst &&
                    !pending.word.contains('\'') && !localChecker.isCorrect(pending.word)
                ) {
                    pending.apply(correction, pending.separator)
                }
            }
        }
    }
}
