package com.nightshift.tracker.ai

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * UroDay build: talks to the Claude Messages API over HTTPS.
 *
 * Deliberately a small hand-rolled client rather than the anthropic-java SDK —
 * that SDK targets server JVMs and is not supported on Android.
 *
 * Only de-identified text ever reaches this class (see [buildDeidentifiedBatch]).
 */
object AiFactory {
    const val AVAILABLE = true

    fun create(context: Context): NoteTidier? {
        val key = AiPrefs.apiKey(context)
        return if (key.isBlank()) null else ClaudeNoteTidier(key)
    }
}

private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
private const val MODEL = "claude-opus-5"

class ClaudeNoteTidier(
    private val apiKey: String,
) : NoteTidier {
    override suspend fun tidy(deidentifiedNotes: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body =
                    JsonObject().apply {
                        addProperty("model", MODEL)
                        addProperty("max_tokens", 16000)
                        addProperty("system", TIDY_SYSTEM_PROMPT.trim())
                        add(
                            "output_config",
                            JsonObject().apply { addProperty("effort", "medium") },
                        )
                        // Server-side fallback: if a safety classifier declines the
                        // request, it is retried on another model instead of just
                        // failing in the doctor's hand.
                        addProperty("fallbacks", "default")
                        add(
                            "messages",
                            JsonArray().apply {
                                add(
                                    JsonObject().apply {
                                        addProperty("role", "user")
                                        addProperty(
                                            "content",
                                            "Tidy these ward round notes for pasting into the " +
                                                "Digital Health Record:\n\n$deidentifiedNotes",
                                        )
                                    },
                                )
                            },
                        )
                    }

                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection)
                conn.apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 30_000
                    readTimeout = 240_000
                    setRequestProperty("content-type", "application/json")
                    setRequestProperty("x-api-key", apiKey)
                    setRequestProperty("anthropic-version", "2023-06-01")
                    setRequestProperty("anthropic-beta", "server-side-fallback-2026-07-01")
                }
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val text =
                    (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                conn.disconnect()

                if (code !in 200..299) {
                    error(friendlyError(code, text))
                }

                val json = JsonParser.parseString(text).asJsonObject
                val stopReason = json.get("stop_reason")?.takeIf { !it.isJsonNull }?.asString
                if (stopReason == "refusal") {
                    error("The model declined this request. Notes were not changed.")
                }
                val out =
                    json
                        .getAsJsonArray("content")
                        .mapNotNull { block ->
                            block.asJsonObject
                                .takeIf { it.get("type")?.asString == "text" }
                                ?.get("text")
                                ?.asString
                        }.joinToString("\n")
                        .trim()
                if (out.isBlank()) error("Empty response from the model.")
                out
            }
        }

    private fun friendlyError(code: Int, body: String): String {
        val apiMessage =
            runCatching {
                JsonParser
                    .parseString(body)
                    .asJsonObject
                    .getAsJsonObject("error")
                    .get("message")
                    .asString
            }.getOrNull()
        return when (code) {
            401 -> "API key rejected (401). Check the key in AI settings."
            403 -> "Access denied (403). Check the key's permissions."
            429 -> "Rate limited (429). Wait a moment and try again."
            in 500..599 -> "Anthropic API error ($code). Try again shortly."
            else -> apiMessage ?: "Request failed ($code)."
        }
    }
}
