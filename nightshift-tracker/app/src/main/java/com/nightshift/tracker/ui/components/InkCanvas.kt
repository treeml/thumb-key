package com.nightshift.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nightshift.tracker.ui.theme.Ink
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextPrimary

// Ink is stored as JSON: a list of strokes, each a flat [x0,y0,x1,y1,...]
// list normalised to the capture canvas width (so it rescales cleanly).

private val gson = Gson()
private val strokesType = object : TypeToken<List<List<Float>>>() {}.type

fun encodeStrokes(strokes: List<List<Float>>): String = gson.toJson(strokes)

fun decodeStrokes(json: String?): List<List<Float>> =
    if (json.isNullOrBlank()) {
        emptyList()
    } else {
        runCatching { gson.fromJson<List<List<Float>>>(json, strokesType) }.getOrNull() ?: emptyList()
    }

/**
 * Full-width S Pen / finger handwriting capture dialog. Strokes are captured
 * from any pointer (stylus draws naturally with low latency via Compose's
 * pointer pipeline) and saved as vector data, not a bitmap.
 */
@Composable
fun InkCaptureDialog(
    initialJson: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    val strokes = remember { mutableStateListOf<List<Float>>().apply { addAll(decodeStrokes(initialJson)) } }
    val current = remember { mutableStateListOf<Float>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Handwritten note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Write with the S Pen. Saved as ink on the job card.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(Ink, RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    current.clear()
                                    current.add(down.position.x / size.width)
                                    current.add(down.position.y / size.width)
                                    var pointer = down
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointer.id } ?: break
                                        if (!change.pressed) {
                                            if (current.size >= 4) strokes.add(current.toList())
                                            current.clear()
                                            break
                                        }
                                        current.add(change.position.x / size.width)
                                        current.add(change.position.y / size.width)
                                        change.consume()
                                        pointer = change
                                    }
                                }
                            },
                ) {
                    val w = size.width
                    val all = strokes.toList() + listOf(current.toList())
                    for (stroke in all) {
                        if (stroke.size < 4) continue
                        val path = Path()
                        path.moveTo(stroke[0] * w, stroke[1] * w)
                        var i = 2
                        while (i + 1 < stroke.size) {
                            path.lineTo(stroke[i] * w, stroke[i + 1] * w)
                            i += 2
                        }
                        drawPath(
                            path,
                            color = TextPrimary,
                            style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                    }) { Text("Undo stroke") }
                    TextButton(onClick = { strokes.clear() }) { Text("Clear") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(if (strokes.isEmpty()) null else encodeStrokes(strokes.toList()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Surface2,
    )
}

/** Read-only render of saved ink inside a job card. */
@Composable
fun InkPreview(
    json: String,
    modifier: Modifier = Modifier,
) {
    val strokes = remember(json) { decodeStrokes(json) }
    if (strokes.isEmpty()) return
    // Height proportional to the ink's vertical extent, clamped for sanity.
    val maxY = strokes.maxOf { s -> s.filterIndexed { i, unused -> i % 2 == 1 }.maxOrNull() ?: 0f }
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height((maxY.coerceIn(0.15f, 0.9f) * 340).dp)
                .background(Ink, RoundedCornerShape(10.dp))
                .padding(4.dp),
    ) {
        val w = size.width
        for (stroke in strokes) {
            if (stroke.size < 4) continue
            val path = Path()
            path.moveTo(stroke[0] * w, stroke[1] * w)
            var i = 2
            while (i + 1 < stroke.size) {
                path.lineTo(stroke[i] * w, stroke[i + 1] * w)
                i += 2
            }
            drawPath(
                path,
                color = TextPrimary,
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
