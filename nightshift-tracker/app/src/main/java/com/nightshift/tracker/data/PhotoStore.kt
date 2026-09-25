package com.nightshift.tracker.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Where clinical photos live: a private directory inside the app, and nowhere
 * else.
 *
 * This is deliberately NOT MediaStore. A photo taken through MediaStore lands
 * in the camera roll, which means it syncs to Google Photos, shows up in the
 * gallery next to everything else, and is shared by every app the user grants
 * photo access to. For a picture of somebody's wound that is the wrong answer
 * in about five different ways. Files here are readable only by this app, go
 * when the app is uninstalled, and are excluded from cloud backup.
 *
 * Images are re-encoded down to [MAX_EDGE] on the long side after capture. A
 * modern phone camera writes 8-12 MB per frame; a wound photo needs none of
 * that, and the device this runs on is the same one holding the database.
 */
object PhotoStore {
    private const val DIR = "photos"
    private const val MAX_EDGE = 1600
    private const val QUALITY = 85

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun file(context: Context, fileName: String): File = File(dir(context), fileName)

    fun newFileName(): String = "${UUID.randomUUID()}.jpg"

    /**
     * A content:// URI the camera app can write to. Granting the camera app
     * temporary write access to one file is the whole reason for the
     * FileProvider — it keeps the directory itself private.
     */
    fun uriFor(context: Context, fileName: String): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.photos",
            file(context, fileName),
        )

    /** Shrink and re-encode in place. Safe to call on a file the camera just wrote. */
    fun compress(context: Context, fileName: String) {
        val target = file(context, fileName)
        if (!target.exists() || target.length() == 0L) return
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return
            var sample = 1
            while (longest / sample > MAX_EDGE * 2) sample *= 2
            val decoded =
                BitmapFactory.decodeFile(
                    target.path,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                ) ?: return
            val scale = MAX_EDGE.toFloat() / maxOf(decoded.width, decoded.height)
            val out =
                if (scale >= 1f) {
                    decoded
                } else {
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * scale).toInt().coerceAtLeast(1),
                        (decoded.height * scale).toInt().coerceAtLeast(1),
                        true,
                    )
                }
            FileOutputStream(target).use { out.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        }
    }

    /** Decode for display, sampled down to roughly [maxEdge]. Call off the main thread. */
    fun decode(context: Context, fileName: String, maxEdge: Int): Bitmap? {
        val target = file(context, fileName)
        if (!target.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge * 2) sample *= 2
            BitmapFactory.decodeFile(
                target.path,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )
        }.getOrNull()
    }

    fun delete(context: Context, fileName: String) {
        runCatching { file(context, fileName).delete() }
    }

    /**
     * Remove image files no row points at any more.
     *
     * Deleting a photo removes its row immediately but leaves the file for a
     * day, so the undo that restores the row still has an image to show. This
     * sweeps what is genuinely orphaned, on startup, once.
     */
    fun sweepOrphans(context: Context, keep: Set<String>) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        runCatching {
            dir(context).listFiles()?.forEach { file ->
                if (file.name !in keep && file.lastModified() < cutoff) file.delete()
            }
        }
    }
}
