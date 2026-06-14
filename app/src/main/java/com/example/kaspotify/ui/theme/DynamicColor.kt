package com.example.kaspotify.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a vibrant accent color from the artwork at [artworkUri], or null if the artwork can't
 * be loaded or has no usable swatch. Recomputed whenever [artworkUri] changes.
 */
@Composable
fun rememberArtworkAccentColor(artworkUri: Any?): State<Color?> {
    val context = LocalContext.current
    return produceState<Color?>(initialValue = null, key1 = artworkUri) {
        value = if (artworkUri == null) null else extractAccentColor(context, artworkUri)
    }
}

private suspend fun extractAccentColor(context: Context, artworkUri: Any): Color? =
    withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(artworkUri)
            .allowHardware(false)
            .build()
        val result = ImageLoader(context).execute(request)
        val bitmap: Bitmap? = when (result) {
            is SuccessResult -> result.drawable.toBitmapOrNull()
            is ErrorResult -> null
            else -> null
        }
        bitmap?.let { bmp ->
            val palette = Palette.from(bmp).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
            swatch?.let { Color(it.rgb) }
        }
    }
