package com.dessalines.thumbkey.utils

import android.content.Context
import com.dessalines.thumbkey.R

/**
 * Fast local spell checker backed by a bundled word list.
 * Does not depend on SpellCheckerSession or any system service.
 *
 * Uses edit-distance-1 candidate generation: instead of comparing the input
 * word against every dictionary word (O(n)), it enumerates all strings that
 * are 1 edit away from the input and checks each against a HashSet (O(1)).
 * For edit distance 2 it repeats the step on each edit-1 candidate.
 * This keeps latency well under 5 ms for typical word lengths.
 */
class LocalSpellChecker(context: Context) {

    private val words: HashSet<String>

    init {
        val raw = context.resources.openRawResource(R.raw.words_en)
            .bufferedReader()
            .readText()
        words = HashSet(raw.lines().map { it.trim() }.filter { it.isNotEmpty() })
    }

    /** Returns true when the word is in the dictionary or is a contraction. */
    fun isCorrect(word: String): Boolean {
        val lower = word.lowercase()
        return lower.contains('\'') || lower in words
    }

    /** Returns true when [candidate] is exactly 1 edit away from [word]. */
    fun isEditDistance1(word: String, candidate: String) =
        candidate.lowercase() in edits1(word.lowercase())

    /**
     * Returns up to [limit] corrections, closest edit distance first.
     * Returns empty list when the word is already correct.
     */
    fun getSuggestions(word: String, limit: Int = 3): List<String> {
        val lower = word.lowercase()
        // Never "correct" contractions — apostrophe words are valid as-is
        if (lower.contains('\'')) return emptyList()
        if (lower in words) return emptyList()

        // Edit distance 1 — fast enough for real-time use
        val ed1 = edits1(lower).filter { it in words }
        if (ed1.isNotEmpty()) return ed1.sortedBy { it }.take(limit)

        // Edit distance 2 — slightly slower but still fine
        val ed2 = edits1(lower)
            .flatMap { edits1(it) }
            .filter { it in words && it != lower }
            .toSet()
        return ed2.sortedBy { it }.take(limit)
    }

    private val alphabet = "abcdefghijklmnopqrstuvwxyz"

    private fun edits1(word: String): Set<String> {
        val results = mutableSetOf<String>()
        for (i in word.indices) {
            // Deletions
            results.add(word.removeRange(i, i + 1))
            // Substitutions
            for (c in alphabet) {
                if (word[i] != c) results.add(word.substring(0, i) + c + word.substring(i + 1))
            }
            // Transpositions
            if (i < word.length - 1) {
                results.add(word.substring(0, i) + word[i + 1] + word[i] + word.substring(i + 2))
            }
        }
        // Insertions
        for (i in 0..word.length) {
            for (c in alphabet) {
                results.add(word.substring(0, i) + c + word.substring(i))
            }
        }
        return results
    }
}
