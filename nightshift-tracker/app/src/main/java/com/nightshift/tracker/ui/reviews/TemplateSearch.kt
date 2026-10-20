package com.nightshift.tracker.ui.reviews

/**
 * Finding the right template by typing what the nurse just said.
 *
 * The old horizontal strip of chips meant scrolling sideways through a list
 * that only grows. Typing is faster and doesn't care how long the list gets —
 * but only if it forgives the way people actually type at 3 am: an abbreviation
 * ("sob", "bno", "temp"), a synonym ("fell", "found on floor"), or a plain
 * typo. "cheat pain" has to find Chest pain, or the box is just another thing
 * that doesn't work when you're tired.
 */

/**
 * Optimal string alignment distance — Levenshtein plus adjacent transposition,
 * so "cheat"/"chest" and "hte"/"the" cost one, not two. Transposition is the
 * single most common typing error, and treating it as two edits is what makes
 * naive fuzzy search feel stupid.
 */
private fun editDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) d[i][0] = i
    for (j in 0..b.length) d[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + cost)
            }
        }
    }
    return d[a.length][b.length]
}

/** How wrong a word is allowed to be. Short words get no slack — too many collide. */
private fun tolerance(length: Int): Int =
    when {
        length <= 3 -> 0
        length <= 5 -> 1
        else -> 2
    }

private fun tokens(text: String): List<String> =
    text
        .lowercase()
        .split(Regex("""[^a-z0-9+]+"""))
        .filter { it.isNotBlank() }

/** Every word this template should answer to. */
private fun haystack(template: ReviewTemplate): List<String> =
    tokens(template.label) + tokens(template.reason) + template.aliases.flatMap { tokens(it) }

/**
 * Lower is better; null means this template does not match at all. Every word
 * typed has to land somewhere, so "cheat pain" reaches Chest pain but not
 * Stent pain — an extra word narrows the list instead of widening it.
 */
private fun score(template: ReviewTemplate, queryTokens: List<String>, rawQuery: String): Int? {
    val words = haystack(template)
    var total = 0
    for (q in queryTokens) {
        val best =
            words.minOfOrNull { word ->
                when {
                    word == q -> 0
                    word.startsWith(q) -> 1
                    q.length >= 3 && word.contains(q) -> 2
                    else -> {
                        val d = editDistance(q, word)
                        if (d <= tolerance(q.length)) 3 + d else Int.MAX_VALUE
                    }
                }
            } ?: Int.MAX_VALUE
        if (best == Int.MAX_VALUE) return null
        total += best
    }
    // Typing the start of the label itself is the strongest possible signal.
    if (template.label.lowercase().startsWith(rawQuery.lowercase().trim())) total -= 2
    return total
}

/**
 * Matching templates, best first. An empty query returns the whole list in its
 * natural order, so the box doubles as the menu when you don't know what to
 * call the thing yet.
 */
fun searchTemplates(
    query: String,
    from: List<ReviewTemplate> = reviewTemplates,
): List<ReviewTemplate> {
    val queryTokens = tokens(query)
    if (queryTokens.isEmpty()) return from
    return from
        .mapIndexedNotNull { index, template ->
            score(template, queryTokens, query)?.let { Triple(template, it, index) }
        }.sortedWith(compareBy({ it.second }, { it.third }))
        .map { it.first }
}
