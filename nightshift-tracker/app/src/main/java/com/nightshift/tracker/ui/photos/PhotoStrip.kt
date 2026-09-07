package com.nightshift.tracker.ui.photos

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nightshift.tracker.BuildConfig
import com.nightshift.tracker.data.Photo
import com.nightshift.tracker.data.PhotoStore
import com.nightshift.tracker.ui.MainViewModel
import com.nightshift.tracker.ui.components.ArmedDeleteButton
import com.nightshift.tracker.ui.components.DbTextField
import com.nightshift.tracker.ui.design.NsAction
import com.nightshift.tracker.ui.design.Radius
import com.nightshift.tracker.ui.design.SectionLabel
import com.nightshift.tracker.ui.design.Space
import com.nightshift.tracker.ui.theme.Outline
import com.nightshift.tracker.ui.theme.Surface1
import com.nightshift.tracker.ui.theme.Surface2
import com.nightshift.tracker.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Photos on a round entry or a review: a wound, a stoma, what came out of the
 * catheter.
 *
 * UroDay only, and a no-op elsewhere, so the shared surfaces can call it
 * unconditionally.
 *
 * The picture goes straight into the app's private storage and never touches
 * the camera roll — see PhotoStore for why that matters. It also never goes
 * near the note-tidy: that path sends text and only text, so an image cannot
 * leave the phone through it.
 */
@Composable
fun PhotoStrip(
    ownerId: String,
    vm: MainViewModel,
    generation: Int,
) {
    if (!BuildConfig.URO) return

    val context = LocalContext.current
    // remember the flow, not just its value: photosFor builds a new query each
    // call, and without this every recomposition would tear down and restart
    // the collection.
    val photoFlow = remember(ownerId) { vm.photosFor(ownerId) }
    val photos by photoFlow.collectAsStateWithLifecycle(emptyList())
    var pending by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<Photo?>(null) }

    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val name = pending
            pending = null
            // A cancelled capture leaves an empty file behind; the sweeper takes
            // it, and nothing is recorded against the round either way.
            if (saved && name != null) vm.attachPhoto(ownerId, name)
        }

    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        SectionLabel(if (photos.isEmpty()) "PHOTOS" else "PHOTOS (${photos.size})")
        Row(
            horizontalArrangement = Arrangement.spacedBy(Space.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        ) {
            photos.forEach { photo ->
                Thumbnail(photo = photo, onClick = { viewing = photo })
            }
            NsAction(
                label = if (photos.isEmpty()) "Take photo" else "Add",
                onClick = {
                    val name = PhotoStore.newFileName()
                    pending = name
                    camera.launch(PhotoStore.uriFor(context, name))
                },
                icon = Icons.Filled.AddAPhoto,
            )
        }
        if (photos.isEmpty()) {
            Text(
                "Stays on this phone: app-private storage, not the camera roll, " +
                    "not backed up to Google, never sent with a tidied note.",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }

    viewing?.let { photo ->
        PhotoViewer(
            photo = photo,
            generation = generation,
            onCaption = { vm.setPhotoCaption(photo, it) },
            onDelete = {
                vm.deletePhotoWithUndo(photo)
                viewing = null
            },
            onDismiss = { viewing = null },
        )
    }
}

@Composable
private fun Thumbnail(
    photo: Photo,
    onClick: () -> Unit,
) {
    val image = rememberDecoded(photo.fileName, 240)
    Box(
        Modifier
            .size(72.dp)
            .background(Surface2, RoundedCornerShape(Radius.sm))
            .border(1.dp, Outline, RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = photo.caption.ifBlank { "Clinical photo" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(Radius.sm)),
            )
        } else {
            Text("—", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun PhotoViewer(
    photo: Photo,
    generation: Int,
    onCaption: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val image = rememberDecoded(photo.fileName, 1400)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Photo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                val bitmap = image
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = photo.caption.ifBlank { "Clinical photo" },
                        contentScale = ContentScale.Fit,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(Surface1, RoundedCornerShape(Radius.sm)),
                    )
                } else {
                    Text(
                        "The image file is not on this device. Photos are not carried " +
                            "in an exported backup — only the record that one was taken.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = Space.md),
                    )
                }
                DbTextField(
                    value = photo.caption,
                    onCommit = onCaption,
                    label = "Caption (no names)",
                    seedKey = "${photo.id}-$generation-caption",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ArmedDeleteButton(onConfirmedDelete = onDelete, idleLabel = "Delete photo")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        containerColor = Surface2,
    )
}

/**
 * Decode off the main thread and hold the result for as long as the file name
 * is the same. Bitmaps are far too expensive to decode during composition.
 */
@Composable
private fun rememberDecoded(fileName: String, maxEdge: Int): ImageBitmap? {
    val context = LocalContext.current
    val state =
        produceState<ImageBitmap?>(initialValue = null, fileName, maxEdge) {
            value =
                withContext(Dispatchers.IO) {
                    val bitmap: Bitmap? = PhotoStore.decode(context, fileName, maxEdge)
                    bitmap?.asImageBitmap()
                }
        }
    return state.value
}
