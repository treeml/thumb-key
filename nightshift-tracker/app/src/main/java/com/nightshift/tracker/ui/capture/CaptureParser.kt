package com.nightshift.tracker.ui.capture

/**
 * Turns one spoken or typed line into a structured job.
 *
 * The whole point is that on a moving round you type (or dictate) the way you
 * would say it, and never touch a dropdown:
 *
 *   "b12 chase potassium !1 30m"        -> bed 12, urgent, 30-min timer
 *   "bed 7 recheck BP in 30 min urgent" -> bed 7, urgent, 30-min timer
 *   "review bloods later"               -> routine, no timer
 *
 * Parsing is always *shown* before it commits (the chips under the bar), so it
 * never silently guesses wrong — the user can see and correct it.
 */
data class ParsedCapture(
    val text: String,
    val bed: String,
    val priority: Int,
    val timerMinutes: Int?,
) {
    val isEmpty: Boolean get() = text.isBlank() && bed.isBlank()
}

private val BED_SHORT = Regex("""\bb(\d{1,3}[a-dA-D]?)\b""")
private val BED_WORD = Regex("""\bbed\s*(\d{1,3}[a-dA-D]?)\b""", RegexOption.IGNORE_CASE)
private val BANG_PRIORITY = Regex("""(?:^|\s)!([123])\b""")
private val MINUTES = Regex("""\b(\d{1,3})\s*(?:m|min|mins|minute|minutes)\b""", RegexOption.IGNORE_CASE)
private val HOURS = Regex("""\b(\d{1,2})(?:\.(\d))?\s*(?:h|hr|hrs|hour|hours)\b""", RegexOption.IGNORE_CASE)
private val URGENT_WORDS = Regex("""\b(urgent|urgently|asap|now|stat)\b""", RegexOption.IGNORE_CASE)
private val SOON_WORDS = Regex("""\b(soon|shortly)\b""", RegexOption.IGNORE_CASE)
private val ROUTINE_WORDS = Regex("""\b(routine|later|whenever|non[- ]urgent)\b""", RegexOption.IGNORE_CASE)
private val FILLER = Regex("""\b(in|at|for)\s*$""", RegexOption.IGNORE_CASE)

fun parseCapture(raw: String): ParsedCapture {
    var text = raw
    var bed = ""
    var priority = 2
    var minutes: Int? = null

    fun consume(regex: Regex, onMatch: (MatchResult) -> Unit) {
        val match = regex.find(text) ?: return
        onMatch(match)
        text = text.removeRange(match.range)
    }

    // Timer: hours first so "1h30" style input doesn't lose the hour.
    consume(HOURS) { m ->
        val whole = m.groupValues[1].toIntOrNull() ?: 0
        val tenths = m.groupValues[2].toIntOrNull() ?: 0
        minutes = whole * 60 + tenths * 6
    }
    consume(MINUTES) { m ->
        val mins = m.groupValues[1].toIntOrNull()
        if (mins != null) minutes = (minutes ?: 0) + mins
    }

    consume(BED_WORD) { m -> bed = m.groupValues[1] }
    if (bed.isBlank()) consume(BED_SHORT) { m -> bed = m.groupValues[1] }

    consume(BANG_PRIORITY) { m -> priority = m.groupValues[1].toInt() }
    if (priority == 2) {
        consume(URGENT_WORDS) { priority = 1 }
    }
    if (priority == 2) {
        consume(ROUTINE_WORDS) { priority = 3 }
    }
    if (priority == 2) {
        consume(SOON_WORDS) { priority = 2 }
    }

    // Tidy the leftovers: collapse whitespace, drop dangling "in"/"at"/"for".
    text = text.replace(Regex("""\s+"""), " ").trim()
    text = FILLER.replace(text, "").trim()
    text = text.trim(' ', ',', '-', ':')
    if (text.isNotEmpty()) text = text.replaceFirstChar { it.uppercase() }

    return ParsedCapture(text = text, bed = bed, priority = priority, timerMinutes = minutes)
}

/** Human-readable summary of what the parser understood, for the chips. */
fun ParsedCapture.chips(): List<String> =
    buildList {
        if (bed.isNotBlank()) add("Bed $bed")
        add(
            when (priority) {
                1 -> "URGENT"
                3 -> "ROUTINE"
                else -> "SOON"
            },
        )
        timerMinutes?.let { m ->
            add(if (m >= 60 && m % 60 == 0) "timer ${m / 60}h" else "timer ${m}m")
        }
    }
