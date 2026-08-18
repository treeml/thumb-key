package com.nightshift.tracker.ai

import android.content.Context
import com.nightshift.tracker.data.WardRound

/**
 * Optional "tidy these notes" helper.
 *
 * Privacy rules that the whole feature is built around:
 *  - Only the UroDay flavor has INTERNET permission at all; Nightshift's
 *    AiFactory is a no-op stub, so its APK cannot reach a network.
 *  - Identifiers (name, MRN, bed) NEVER leave the phone. [Deidentifier]
 *    swaps them for placeholders before the request and puts them back
 *    locally afterwards.
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

/**
 * Replaces patient identifiers with stable placeholders before a request and
 * restores them afterwards. The map lives only in memory on the device.
 */
class Deidentifier {
    private val restore = LinkedHashMap<String, String>()

    fun token(kind: String, index: Int, realValue: String): String {
        val placeholder = "[[$kind$index]]"
        if (realValue.isNotBlank()) restore[placeholder] = realValue
        return placeholder
    }

    fun reidentify(text: String): String {
        var out = text
        for ((placeholder, real) in restore) {
            out = out.replace(placeholder, real)
        }
        return out
    }
}

const val TIDY_SYSTEM_PROMPT = """
You are helping an Australian junior doctor (JMO) on a urology ward tidy up
ward-round notes so they can be pasted into a Digital Health Record.

ABSOLUTE RULES:
- Reformat and clarify ONLY. Never add clinical content, findings, results,
  diagnoses, doses or plans that are not in the input.
- If something is ambiguous or missing, leave it as written or omit it. Never
  guess. Never write "no abnormality detected" or similar unless the note says so.
- Do not add safety-netting, advice, or recommendations of your own.
- Preserve every placeholder token exactly as written (e.g. [[P1]], [[B1]],
  [[M1]]). They stand in for patient identifiers.
- Keep standard Australian clinical abbreviations that a colleague would
  expect (IDC, TURP, POD, CBI, TOV, UEC, eGFR, MSU). Expand only shorthand
  that is genuinely unclear (e.g. "wnl" -> "within normal limits").
- Do not add a signature, doctor name, or disclaimer.

STYLE:
- Fix grammar, spelling, capitalisation and spacing; convert fragments into
  clean clinical shorthand lines.
- Keep it terse and factual. No filler, no narrative prose.
- Keep each patient under its existing heading, in the same order given.
- Keep the section labels that are present (Overnight / O E / Results / Plan).
- Plans read best as short numbered or dashed action lines.

Return ONLY the tidied notes. No preamble, no commentary, no markdown fences.
"""

/**
 * Builds the de-identified batch text sent to the model, plus the
 * [Deidentifier] needed to restore identifiers in the response.
 */
fun buildDeidentifiedBatch(rounds: List<WardRound>): Pair<String, Deidentifier> {
    val de = Deidentifier()
    val sb = StringBuilder()
    rounds.forEachIndexed { i, r ->
        val n = i + 1
        val name = de.token("P", n, r.patientName)
        val bed = de.token("B", n, r.bed)
        val mrn = de.token("M", n, r.mrn)
        sb.appendLine("### PATIENT $n")
        sb.appendLine("Patient: $name | Bed $bed | MRN: $mrn")
        if (r.dxOp.isNotBlank()) sb.appendLine("Dx/Op: ${r.dxOp.trim()}")
        if (r.overnight.isNotBlank()) sb.appendLine("Overnight: ${r.overnight.trim()}")
        if (r.exam.isNotBlank()) sb.appendLine("O/E: ${r.exam.trim()}")
        if (r.results.isNotBlank()) sb.appendLine("Results: ${r.results.trim()}")
        if (r.plan.isNotBlank()) sb.appendLine("Plan: ${r.plan.trim()}")
        sb.appendLine()
    }
    return sb.toString().trim() to de
}
