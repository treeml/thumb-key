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
    /** Absolute deadline in epoch millis, from a clock time like "at 0400". */
    val dueAt: Long? = null,
) {
    val isEmpty: Boolean get() = text.isBlank() && bed.isBlank()
}

/**
 * Clock times, written however they come out at 3 am: 0400, 04:00, 4am,
 * 4.30pm, midnight, midday. A time already past today means tomorrow, which
 * is almost always what a night shift means by "6 o'clock".
 */
private val AMPM_TIME =
    Regex("""\b(?:at\s+|by\s+|due\s+|@)?(1[0-2]|0?[1-9])(?:[:.]([0-5]\d))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
private val COLON_TIME =
    Regex("""\b(?:at\s+|by\s+|due\s+|@)?([01]?\d|2[0-3]):([0-5]\d)\b""")
private val FOUR_DIGIT_TIME =
    Regex("""\b(?:at\s+|by\s+|due\s+|@)?([01]\d|2[0-3])([0-5]\d)\b""")
private val MIDNIGHT = Regex("""\b(?:at\s+|by\s+|due\s+)?midnight\b""", RegexOption.IGNORE_CASE)
private val MIDDAY = Regex("""\b(?:at\s+|by\s+|due\s+)?(?:midday|noon)\b""", RegexOption.IGNORE_CASE)

/** The next time the clock reads [hour]:[minute] — today if still ahead. */
private fun nextOccurrence(hour: Int, minute: Int, now: Long): Long {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = now
    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
    cal.set(java.util.Calendar.MINUTE, minute)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    if (cal.timeInMillis <= now) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    return cal.timeInMillis
}

/**
 * Finds the first clock time in [text] and reports both the absolute deadline
 * it means and the span it occupied, so the caller can cut it out of the words.
 */
private fun findClock(text: String, now: Long): Pair<Long, IntRange>? {
    AMPM_TIME.find(text)?.let { m ->
        val rawHour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].toIntOrNull() ?: 0
        val isPm = m.groupValues[3].lowercase() == "pm"
        val hour =
            when {
                isPm && rawHour != 12 -> rawHour + 12
                !isPm && rawHour == 12 -> 0
                else -> rawHour
            }
        return nextOccurrence(hour, minute, now) to m.range
    }
    COLON_TIME.find(text)?.let { m ->
        return nextOccurrence(m.groupValues[1].toInt(), m.groupValues[2].toInt(), now) to m.range
    }
    FOUR_DIGIT_TIME.find(text)?.let { m ->
        return nextOccurrence(m.groupValues[1].toInt(), m.groupValues[2].toInt(), now) to m.range
    }
    MIDNIGHT.find(text)?.let { return nextOccurrence(0, 0, now) to it.range }
    MIDDAY.find(text)?.let { return nextOccurrence(12, 0, now) to it.range }
    return null
}

/**
 * A clock time typed on its own, into a picker rather than into a sentence.
 * Here a bare number is unambiguous — nobody types "400" into a field labelled
 * "at" and means a dose — so "7" is 07:00 and "400" is 04:00. parseCapture
 * deliberately does not accept those: in a whole line they'd eat real numbers.
 */
fun parseClock(raw: String, now: Long = System.currentTimeMillis()): Long? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    findClock(trimmed, now)?.let { return it.first }
    if (!trimmed.all { it.isDigit() }) return null
    val (hour, minute) =
        when (trimmed.length) {
            1, 2 -> trimmed.toInt() to 0
            3 -> trimmed.substring(0, 1).toInt() to trimmed.substring(1).toInt()
            4 -> trimmed.substring(0, 2).toInt() to trimmed.substring(2).toInt()
            else -> return null
        }
    if (hour !in 0..23 || minute !in 0..59) return null
    return nextOccurrence(hour, minute, now)
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

fun parseCapture(raw: String, now: Long = System.currentTimeMillis()): ParsedCapture {
    var text = raw
    var bed = ""
    var priority = 2
    var minutes: Int? = null
    var dueAt: Long? = null

    fun consume(regex: Regex, onMatch: (MatchResult) -> Unit) {
        val match = regex.find(text) ?: return
        onMatch(match)
        text = text.removeRange(match.range)
    }

    // Clock times first: "0400" must not be mistaken for anything else.
    findClock(text, now)?.let { (at, range) ->
        dueAt = at
        text = text.removeRange(range)
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

    return ParsedCapture(
        text = text,
        bed = bed,
        priority = priority,
        timerMinutes = minutes,
        dueAt = dueAt,
    )
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
        dueAt?.let { due ->
            val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val mins = ((due - System.currentTimeMillis()) / 60_000L).coerceAtLeast(0)
            val within =
                if (mins >= 60) "in ${mins / 60}h ${mins % 60}m" else "in ${mins}m"
            add("due ${fmt.format(java.util.Date(due))} · $within")
        }
    }
