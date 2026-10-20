package com.nightshift.tracker.ai

import android.content.Context
import com.nightshift.tracker.data.Review
import com.nightshift.tracker.data.WardRound

/**
 * Optional "tidy this note" helper.
 *
 * Privacy rules the whole feature is built around:
 *  - Only the UroDay flavor has INTERNET permission at all; Nightshift's
 *    AiFactory is a no-op stub, so its APK cannot reach a network.
 *  - Identifiers never leave the phone. [deidentify] swaps the patient names,
 *    MRNs and bed numbers this shift actually contains for placeholders, and
 *    [Deidentifier.reidentify] puts them back locally afterwards.
 *  - The model is instructed to reformat only. Output is always shown for
 *    review before it can be sent anywhere.
 */
interface NoteTidier {
    suspend fun tidy(deidentifiedNotes: String): Result<String>
}

object AiPrefs {
    private const val FILE = "uroday_ai"
    private const val KEY = "api_key"

    fun apiKey(context: Context): String =
        context
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY, "")
            .orEmpty()

    fun setApiKey(context: Context, value: String) {
        context
            .getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, value.trim())
            .apply()
    }

    fun hasKey(context: Context): Boolean = apiKey(context).isNotBlank()
}

/** Restores identifiers into a tidied note. The map lives only in memory. */
class Deidentifier(
    private val restore: Map<String, String>,
) {
    fun reidentify(text: String): String {
        var out = text
        for ((placeholder, real) in restore) {
            out = out.replace(placeholder, real)
        }
        return out
    }
}

/**
 * Replaces every identifier this shift knows about with a placeholder.
 *
 * Names and MRNs are replaced wherever they appear (they are distinctive).
 * Bed numbers are replaced ONLY after the word "Bed" — blanket-replacing a
 * bare number would corrupt doses and results, which is precisely the kind of
 * silent damage this feature must never do.
 */
fun deidentify(
    text: String,
    reviews: List<Review>,
    rounds: List<WardRound>,
): Pair<String, Deidentifier> {
    val names = (reviews.map { it.patientName } + rounds.map { it.patientName })
        .map { it.trim() }
        .filter { it.length >= 3 }
        .distinct()
        .sortedByDescending { it.length }
    val mrns = (reviews.map { it.mrn } + rounds.map { it.mrn })
        .map { it.trim() }
        .filter { it.length >= 3 }
        .distinct()
        .sortedByDescending { it.length }
    val beds = (reviews.map { it.bed } + rounds.map { it.bed })
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    var out = text
    val restore = LinkedHashMap<String, String>()

    names.forEachIndexed { i, name ->
        val token = "[[P${i + 1}]]"
        if (out.contains(name, ignoreCase = true)) {
            out = out.replace(name, token, ignoreCase = true)
            restore[token] = name
        }
    }
    mrns.forEachIndexed { i, mrn ->
        val token = "[[M${i + 1}]]"
        if (out.contains(mrn, ignoreCase = true)) {
            out = out.replace(mrn, token, ignoreCase = true)
            restore[token] = mrn
        }
    }
    beds.forEachIndexed { i, bed ->
        val token = "[[B${i + 1}]]"
        val pattern = Regex("""(?i)\bbed\s+${Regex.escape(bed)}\b""")
        if (pattern.containsMatchIn(out)) {
            out = pattern.replace(out, "Bed $token")
            restore[token] = bed
        }
    }
    return out to Deidentifier(restore)
}

const val TIDY_SYSTEM_PROMPT = """
You are helping an Australian junior doctor (JMO) tidy up clinical notes so
they can be pasted into a Digital Health Record.

ABSOLUTE RULES:
- Reformat and clarify ONLY. Never add clinical content, findings, results,
  diagnoses, doses or plans that are not in the input.
- If something is ambiguous or missing, leave it as written or omit it. Never
  guess. Never write "no abnormality detected", "patient stable", or any
  similar filler unless the note says so.
- Do not add safety-netting, advice, differentials or recommendations of your
  own. You are a typist, not a clinician.
- Preserve every placeholder token exactly as written (e.g. [[P1]], [[B1]],
  [[M1]]). They stand in for patient identifiers.
- Preserve every number, unit, dose and time exactly as written.
- Keep standard Australian clinical abbreviations a colleague would expect
  (IDC, TURP, POD, CBI, TOV, UEC, eGFR, MSU, ABG). Expand only shorthand that
  is genuinely unclear (e.g. "wnl" -> "within normal limits").
- Do not add a signature, doctor name, or disclaimer.

STYLE:
- Fix grammar, spelling, capitalisation and spacing; turn fragments into clean
  clinical shorthand lines.
- Keep it terse and factual. No filler, no narrative prose.
- Keep the existing structure, headings and order. Keep section labels that are
  present (Overnight / O E / Results / Impression / Plan).
- Plans read best as short dashed action lines.

Return ONLY the tidied notes. No preamble, no commentary, no markdown fences.
"""
