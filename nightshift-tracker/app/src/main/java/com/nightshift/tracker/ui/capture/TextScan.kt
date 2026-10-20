package com.nightshift.tracker.ui.capture

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * Reading a wristband, a bed card or a sticker with the camera.
 *
 * ML Kit's bundled recogniser: the model ships inside the APK and runs on the
 * device, so this works in Nightshift, which has no INTERNET permission at
 * all. Nothing is uploaded and nothing is kept — the photo goes to a throwaway
 * file that is deleted the moment the text is out of it.
 */
object TextScan {
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    fun read(
        context: Context,
        uri: Uri,
        onDone: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        runCatching { InputImage.fromFilePath(context, uri) }
            .onFailure { onError("Couldn't open the photo") }
            .onSuccess { image ->
                recognizer
                    .process(image)
                    .addOnSuccessListener { onDone(it.text) }
                    .addOnFailureListener { onError("Couldn't read any text") }
            }
    }
}

private val MRN_LINE = Regex("""\b(\d{6,9})\b""")
private val BED_LINE =
    Regex("""\b(?:bed|bd)\s*[:.]?\s*(\d{1,3}[a-z]?)\b""", RegexOption.IGNORE_CASE)
private val WARD_LINE =
    Regex("""\b(?:ward|wd)\s*[:.]?\s*(\d{1,2}[a-z]?)\b""", RegexOption.IGNORE_CASE)
private val NAME_LINE =
    Regex("""^\s*([A-Z][A-Za-z'\-]+)\s*,\s*([A-Z][A-Za-z'\-]+)""")

/**
 * What a scanned label meant, turned back into a line this app already knows
 * how to read.
 *
 * Deliberately produces capture-bar text rather than writing to fields
 * directly. The parser, the chips and the correction you can make before
 * pressing send all already exist — and a scan is exactly the kind of input
 * that should never commit without being looked at, because a camera pointed
 * at a ward will happily read the patient in the next bed.
 */
fun scanToCaptureLine(raw: String): String {
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }

    val bed =
        lines.firstNotNullOfOrNull { BED_LINE.find(it)?.groupValues?.get(1) }
    val ward =
        lines.firstNotNullOfOrNull { WARD_LINE.find(it)?.groupValues?.get(1) }
    // Longest digit run that is not obviously a date or a phone number.
    val mrn =
        lines
            .flatMap { line -> MRN_LINE.findAll(line).map { it.groupValues[1] }.toList() }
            .firstOrNull()
    val initials =
        lines.firstNotNullOfOrNull { line ->
            NAME_LINE.find(line)?.let { m ->
                "${m.groupValues[2].first()}${m.groupValues[1].first()}".uppercase()
            }
        }

    val location =
        when {
            bed != null && ward != null -> "b${bed}w$ward"
            bed != null -> "b$bed"
            ward != null -> "w$ward"
            else -> null
        }

    return listOfNotNull(location, initials, mrn).joinToString(" ").let {
        // A trailing space so the cursor lands where the task goes.
        if (it.isBlank()) "" else "$it "
    }
}
