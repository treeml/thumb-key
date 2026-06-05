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

    // Correction candidates only (not the original word)
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestionsFlow: StateFlow<List<String>> = _suggestions.asStateFlow()

    // The word currently being typed — exposed so the UI can show it in the middle slot
    private val _currentWordFlow = MutableStateFlow("")
    val currentWordFlow: StateFlow<String> = _currentWordFlow.asStateFlow()

    fun init() {
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
     * immediately using the local dictionary.
     */
    fun requestSuggestions(word: String) {
        if (word.length < 2) {
            currentWord = word
            _currentWordFlow.value = word
            _suggestions.value = emptyList()
            return
        }
        if (word == currentWord) return
        currentWord = word
        _currentWordFlow.value = word

        val localSuggestions = localChecker.getSuggestions(word)
        _suggestions.value = localSuggestions
        Log.d(TAG, "AutoCorrectManager: local '$word' → $localSuggestions")

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
        _currentWordFlow.value = ""
        _suggestions.value = emptyList()
    }

    /**
     * Called when a word separator is typed. Auto-applies a correction only when:
     *  - the word is not a contraction and not already in the dictionary
     *  - the correction shares the first letter (prevents wild substitutions)
     *  - the correction is edit-distance-1 (not a speculative ed-2 guess)
     */
    fun scheduleAutoCorrect(
        word: String,
        separator: String,
        apply: (correction: String, separator: String) -> Unit,
    ) {
        pendingAutocorrect = null
        if (word.length < 2) return

        if (word.contains('\'') || localChecker.isCorrect(word)) {
            clearSuggestions()
            return
        }

        val suggestions = _suggestions.value
        if (suggestions.isNotEmpty()) {
            val correction = suggestions.first()
            val sameFirstLetter = correction.firstOrNull()?.lowercaseChar() ==
                word.firstOrNull()?.lowercaseChar()
            val isEd1 = localChecker.isEditDistance1(word, correction)
            clearSuggestions()
            if (correction != word && sameFirstLetter && isEd1) apply(correction, separator)
        } else {
            clearSuggestions()
        }
    }

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
        mainHandler.post {
            if (_suggestions.value != suggestions) {
                _suggestions.value = suggestions
            }
            val pending = pendingAutocorrect
            if (pending != null && pending.word == currentWord) {
                pendingAutocorrect = null
                val correction = suggestions.firstOrNull()
                val sameFirst = correction?.firstOrNull()?.lowercaseChar() ==
                    pending.word.firstOrNull()?.lowercaseChar()
                val isEd1 = correction != null && localChecker.isEditDistance1(pending.word, correction)
                clearSuggestions()
                if (correction != null && correction != pending.word && sameFirst && isEd1 &&
                    !pending.word.contains('\'') && !localChecker.isCorrect(pending.word)
                ) {
                    pending.apply(correction, pending.separator)
                }
            }
        }
    }
}
