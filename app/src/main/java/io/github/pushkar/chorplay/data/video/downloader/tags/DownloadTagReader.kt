package io.github.pushkar.chorplay.data.video.downloader.tags

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.inspector.MetadataRetriever
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
class DownloadTagReader
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun read(uri: Uri): EmbeddedTags? =
        withContext(Dispatchers.IO) {
            val embedded = EmbeddedTags.fromEntries(entries(uri))
            if (embedded.chor != null) return@withContext embedded
            val platform = readWithPlatform(uri)
            val merged =
                platform?.let { embedded.withFallback(it.title, it.artist, it.cover) } ?: embedded
            merged.takeIf { tags ->
                tags.cover != null ||
                        listOf(
                            tags.title,
                            tags.artist,
                            
                            tags.description,
                            tags.comment,
                            tags.lyrics
                        ).any { it != null }
            }
        }

    internal suspend fun entries(uri: Uri): List<Metadata.Entry> =
        withContext(Dispatchers.IO) { withTimeoutOrNull(TIMEOUT_MS) { retrieveEntries(uri) }.orEmpty() }

    private suspend fun retrieveEntries(uri: Uri): List<Metadata.Entry> =
        try {
            MetadataRetriever.Builder(context, MediaItem.fromUri(uri)).build().use { retriever ->
                retriever.retrieveTrackGroups().await().metadataEntries()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Media3 metadata read failed for $uri: ${e.message}")
            emptyList()
        }

    private fun readWithPlatform(uri: Uri): EmbeddedTags? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == null || uri.scheme == "file") retriever.setDataSource(uri.path) else retriever.setDataSource(
                context,
                uri
            )
            EmbeddedTags(
                chor = null,
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                
                cover = retriever.embeddedPicture,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Platform metadata read failed for $uri: ${e.message}")
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private suspend fun <T> ListenableFuture<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addListener(
                {
                    try {
                        continuation.resume(get())
                    } catch (e: ExecutionException) {
                        continuation.resumeWithException(e.cause ?: e)
                    } catch (e: CancellationException) {
                        continuation.cancel(e)
                    }
                },
                MoreExecutors.directExecutor(),
            )
            continuation.invokeOnCancellation { cancel(false) }
        }

    private companion object {
        const val TAG = "DownloadTagReader"
        const val TIMEOUT_MS = 10_000L
    }
}
