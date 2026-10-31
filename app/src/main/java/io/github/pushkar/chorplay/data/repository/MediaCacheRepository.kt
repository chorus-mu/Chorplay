package io.github.pushkar.chorplay.data.repository

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Immutable
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import coil3.ImageLoader
import dagger.Lazy
import io.github.pushkar.chorplay.di.PlayerCache
import io.github.pushkar.chorplay.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** The caches the settings page measures and clears one by one. */
enum class MediaCacheType { VIDEOS, ARTWORK, OTHER }

/** Bytes each cache holds right now. */
@Immutable
data class MediaCacheUsage(
    val videos: Long,
    val artwork: Long,
    val other: Long,
) {
    val total: Long get() = videos + artwork + other

    fun of(type: MediaCacheType): Long =
        when (type) {
            MediaCacheType.VIDEOS -> videos
            MediaCacheType.ARTWORK -> artwork
            MediaCacheType.OTHER -> other
        }
}

/**
 * Measures and empties the app's caches: videos and Shorts, images, and network responses.
 * The caches are only opened when asked, on [Dispatchers.IO].
 *
 * Clearing removes resources the same way Media3's own evictor does while a player is running, so a
 * playing item simply fetches what it needs again; nothing is torn down under it.
 */
@Singleton
@OptIn(UnstableApi::class)
class MediaCacheRepository
    @Inject
    constructor(
        @PlayerCache private val videoCache: Lazy<SimpleCache>,
        private val imageLoader: Lazy<ImageLoader>,
        private val okHttpClient: OkHttpClient,
        private val youTube: YouTube,
    ) {
        suspend fun usage(): MediaCacheUsage =
            withContext(Dispatchers.IO) {
                MediaCacheUsage(
                    videos = safely { videoCache.get().cacheSpace },
                    artwork = safely { imageLoader.get().diskCache?.size ?: 0L },
                    other =
                        safely { okHttpClient.cache?.size() ?: 0L } +
                            safely { youTube.cacheDirectory?.contentSize() ?: 0L },
                )
            }

        suspend fun clear(type: MediaCacheType) =
            withContext(Dispatchers.IO) {
                when (type) {
                    MediaCacheType.VIDEOS -> {
                        videoCache.get().removeAll()
                    }

                    MediaCacheType.ARTWORK -> {
                        val loader = imageLoader.get()
                        loader.memoryCache?.clear()
                        loader.diskCache?.clear()
                    }

                    MediaCacheType.OTHER -> {
                        runCatching { okHttpClient.cache?.evictAll() }.onFailure { Log.w(TAG, "Could not clear the network cache", it) }
                        youTube.cacheDirectory?.listFiles()?.forEach { it.deleteRecursively() }
                    }
                }
            }

        private fun SimpleCache.removeAll() {
            keys.toList().forEach { key ->
                runCatching { removeResource(key) }.onFailure { Log.w(TAG, "Could not clear a cached item", it) }
            }
        }

        private fun File.contentSize(): Long = walkBottomUp().filter { it.isFile }.sumOf { it.length() }

        private inline fun safely(measure: () -> Long): Long =
            runCatching(measure).onFailure { Log.w(TAG, "Could not measure a cache", it) }.getOrDefault(0L)

        private companion object {
            const val TAG = "MediaCacheRepository"
        }
    }
