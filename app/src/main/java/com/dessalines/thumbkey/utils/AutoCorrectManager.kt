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
    // True only when the word is flagged as a typo (for auto-apply on separator)
    private var lastWordIsTypo = false

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
        val s = session ?: run {
            Log.w(TAG, "AutoCorrectManager: session is null, skipping requestSuggestions")
            return
        }
        lastCheckedWord = word
        resultArrived = false
        try {
            // getSentenceSuggestions is the non-deprecated API (API 16+).
            // It fires onGetSentenceSuggestions; we also accept onGetSuggestions as a fallback.
            s.getSentenceSuggestions(arrayOf(TextInfo(word)), 3)
        } catch (e: Exception) {
            Log.e(TAG, "AutoCorrectManager: getSentenceSuggestions error: $e")
            // Try the older API as fallback
            try {
                s.getSuggestions(TextInfo(word), 3)
            } catch (e2: Exception) {
                Log.e(TAG, "AutoCorrectManager: getSuggestions fallback error: $e2")
                resultArrived = true
            }
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
     * Called when a word separator is typed.
     * If suggestions are ready: applies correction immediately if word was a typo.
     * If still waiting: defers until onGetSentenceSuggestions fires.
     */
    fun scheduleAutoCorrect(
        word: String,
        separator: String,
        apply: (correction: String, separator: String) -> Unit,
    ) {
        pendingAutocorrect = null
        if (word.length < 2 || word != lastCheckedWord) return

        if (resultArrived) {
            if (lastWordIsTypo && _suggestions.value.isNotEmpty()) {
                val correction = _suggestions.value.first()
                clearSuggestions()
                if (correction != word) apply(correction, separator)
            } else {
                clearSuggestions()
            }
        } else {
            pendingAutocorrect = PendingAutocorrect(word, separator, apply)
        }
    }

    // Primary callback — fires when getSentenceSuggestions() is used
    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        resultArrived = true
        val sentence = results?.firstOrNull()
        if (sentence == null || sentence.suggestionsCount == 0) {
            _suggestions.value = emptyList()
            lastWordIsTypo = false
            dispatchPending()
            return
        }
        val wordInfo = sentence.getSuggestionsInfoAt(0)
        processSuggestionsInfo(wordInfo)
    }

    // Fallback callback — fires when getSuggestions() is used
    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        resultArrived = true
        val info = results?.firstOrNull()
        if (info == null) {
            _suggestions.value = emptyList()
            lastWordIsTypo = false
            dispatchPending()
            return
        }
        processSuggestionsInfo(info)
    }

    private fun processSuggestionsInfo(info: SuggestionsInfo) {
        val attrs = info.suggestionsAttributes
        // RESULT_ATTR_LOOKS_LIKE_TYPO = 0x0004
        // RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS = 0x0008 (API 31+)
        lastWordIsTypo = (attrs and SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO) != 0

        if (info.suggestionsCount > 0) {
            val all = SpellCheckerHelper.getAllSuggestions(info)
            Log.d(TAG, "AutoCorrectManager: word='$lastCheckedWord' attrs=$attrs isTypo=$lastWordIsTypo suggestions=${all.toList()}")
            _suggestions.value = all.filter { it.isNotEmpty() }.take(3).toList()
        } else {
            Log.d(TAG, "AutoCorrectManager: word='$lastCheckedWord' attrs=$attrs no suggestions")
            _suggestions.value = emptyList()
        }
        dispatchPending()
    }

    private fun dispatchPending() {
        val pending = pendingAutocorrect ?: return
        pendingAutocorrect = null
        if (pending.word != lastCheckedWord) return
        if (lastWordIsTypo && _suggestions.value.isNotEmpty()) {
            val correction = _suggestions.value.first()
            clearSuggestions()
            if (correction != pending.word) {
                pending.apply(correction, pending.separator)
            }
        } else {
            clearSuggestions()
        }
    }
}
