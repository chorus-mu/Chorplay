package io.github.pushkar.chorplay.data.video.downloader.tags

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class CoverArtLoader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
) {
    suspend fun load(
        url: String?,
        kind: DownloadKind,
    ): ByteArray? {
        if (url.isNullOrBlank()) return null
        return try {
            val request =
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .size(Size.ORIGINAL)
                    .allowHardware(false)
                    .build()
            val source = imageLoader.execute(request).image?.toBitmap() ?: return null
            withContext(Dispatchers.Default) { encode(shape(source, kind)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cover art unavailable for $url: ${e.message}")
            null
        }
    }

    private fun shape(
        source: Bitmap,
        kind: DownloadKind,
    ): Bitmap {
        val (width, height) = coverSize(source.width, source.height, kind)
        return when {
            width == source.width && height == source.height -> source
            else -> Bitmap.createScaledBitmap(source, width, height, true)
        }
    }

    private fun encode(bitmap: Bitmap): ByteArray? =
        JPEG_QUALITIES.firstNotNullOfOrNull { quality ->
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray().takeIf { it.size <= IlstAtoms.MAX_COVER_BYTES }
        }

    companion object {
        private const val TAG = "CoverArtLoader"
        private const val MAX_VIDEO_EDGE = 1_280
        private val JPEG_QUALITIES = listOf(90, 75)

        fun coverSize(
            width: Int,
            height: Int,
            kind: DownloadKind,
        ): Pair<Int, Int> {
            val longest = maxOf(width, height)
            if (longest <= MAX_VIDEO_EDGE) return width to height
            val scale = MAX_VIDEO_EDGE.toFloat() / longest
            return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
        }
    }
}
