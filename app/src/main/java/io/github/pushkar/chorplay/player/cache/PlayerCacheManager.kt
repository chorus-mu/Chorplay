package io.github.pushkar.chorplay.player.cache

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import io.github.pushkar.chorplay.BuildConfig
import io.github.pushkar.chorplay.data.local.MediaCacheLimits
import io.github.pushkar.chorplay.data.local.MediaCacheSizes
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.innertube.models.response.PlayerResponse
import io.github.pushkar.chorplay.player.datasource.FakeStreamWall
import io.github.pushkar.chorplay.player.datasource.FakeStreamWallDataSource
import io.github.pushkar.chorplay.player.datasource.RefusedStreamRetryDataSource
import io.github.pushkar.chorplay.player.datasource.RefusedStreamSwapper
import io.github.pushkar.chorplay.player.datasource.StreamSwapTable
import io.github.pushkar.chorplay.player.datasource.YouTubeHttpDataSource
import io.github.pushkar.chorplay.player.error.StreamDenialKind
import io.github.pushkar.chorplay.player.stream.ClientGateTracker
import io.github.pushkar.chorplay.player.stream.InnerTubeVideoStreamExtractor
import io.github.pushkar.chorplay.player.stream.TizenStreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@UnstableApi
class PlayerCacheManager(
    private val context: Context,
    currentVideoId: () -> String?,
) {
    companion object {
        private const val TAG = "PlayerCacheManager"

        @Volatile
        private var preloadedLimits: MediaCacheLimits? = null

        suspend fun preload(context: Context): Unit =
            withContext(Dispatchers.IO) {
                val appContext = context.applicationContext
                val limits = runCatching { readLimits(appContext) }.getOrDefault(MediaCacheLimits.DEFAULT)
                preloadedLimits = limits
                runCatching { SharedPlayerCacheProvider.getOrCreate(appContext, maxCacheSizeBytes = limits.videoBytes) }
                    .onFailure { Log.w(TAG, "Cache preload failed; initialize() will retry", it) }
            }

        fun configuredLimits(context: Context): MediaCacheLimits =
            preloadedLimits ?: runBlocking { readLimits(context.applicationContext) }.also { preloadedLimits = it }

        private suspend fun readLimits(context: Context): MediaCacheLimits {
            val preferences = PlayerPreferences(context)
            return MediaCacheLimits(
                videoBytes = MediaCacheSizes.mediaBytes(preferences.mediaCacheSizeMb.first()),
                artworkBytes = MediaCacheSizes.artworkBytes(preferences.artworkCacheSizeMb.first()),
            )
        }
    }

    private val videoCacheKeys = VideoCacheKeys(currentVideoId)

    private val refusedStreams =
        RefusedStreamSwapper(
            table = StreamSwapTable(),
            reportDenied = ClientGateTracker::reportDenied,
            resolve = ::resolveReplacement,
        )

    private var cache: SimpleCache? = null
    private var sharedDataSourceFactory: DataSource.Factory? = null
    private var sharedDashDataSourceFactory: DataSource.Factory? = null
    private var sharedProgressiveDataSourceFactory: DataSource.Factory? = null
    private var sharedHlsDataSourceFactory: DataSource.Factory? = null
    private var sharedLiveDashDataSourceFactory: DataSource.Factory? = null
    private var sharedLiveHlsDataSourceFactory: DataSource.Factory? = null

    fun initialize(): Boolean {
        val dashHttpFactory = YouTubeHttpDataSource.Factory()
        val progressiveHttpFactory = YouTubeHttpDataSource.Factory()
        val hlsHttpFactory = YouTubeHttpDataSource.Factory()

        val fakeWall = if (BuildConfig.DEBUG) FakeStreamWall.forDebugBuild(context) else null
        val liveDashUpstream = DefaultDataSource.Factory(context, dashHttpFactory)
        val dashUpstream = DefaultDataSource.Factory(context, fakeWall.around(dashHttpFactory)).swappingRefusals()
        val progressiveUpstream =
            DefaultDataSource.Factory(context, fakeWall.around(progressiveHttpFactory)).swappingRefusals()
        val hlsUpstream = DefaultDataSource.Factory(context, hlsHttpFactory)
        sharedLiveDashDataSourceFactory = liveDashUpstream
        sharedLiveHlsDataSourceFactory = hlsUpstream

        val legacyHttpFactory = YouTubeHttpDataSource.Factory()
        val upstream = DefaultDataSource.Factory(context, legacyHttpFactory)

        try {
            cache =
                SharedPlayerCacheProvider.getOrCreate(context, maxCacheSizeBytes = configuredLimits(context).videoBytes)

            val cacheFactory =
                CacheDataSource
                    .Factory()
                    .setCache(cache!!)
                    .setCacheKeyFactory(videoCacheKeys)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            sharedDashDataSourceFactory =
                CacheDataSource
                    .Factory()
                    .setCache(cache!!)
                    .setCacheKeyFactory(videoCacheKeys)
                    .setUpstreamDataSourceFactory(dashUpstream)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            sharedProgressiveDataSourceFactory =
                CacheDataSource
                    .Factory()
                    .setCache(cache!!)
                    .setCacheKeyFactory(videoCacheKeys)
                    .setUpstreamDataSourceFactory(progressiveUpstream)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            sharedHlsDataSourceFactory =
                CacheDataSource
                    .Factory()
                    .setCache(cache!!)
                    .setCacheKeyFactory(videoCacheKeys)
                    .setUpstreamDataSourceFactory(hlsUpstream)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            sharedDataSourceFactory = cacheFactory.setUpstreamDataSourceFactory(upstream)

            Log.d(TAG, "Cache initialized successfully")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Cache not available, using upstream only", e)
            sharedDataSourceFactory = upstream
            sharedDashDataSourceFactory = dashUpstream
            sharedProgressiveDataSourceFactory = progressiveUpstream
            sharedHlsDataSourceFactory = hlsUpstream
            sharedLiveDashDataSourceFactory = liveDashUpstream
            sharedLiveHlsDataSourceFactory = hlsUpstream
            return false
        }
    }

    fun getDataSourceFactory(): DataSource.Factory? = sharedDataSourceFactory

    fun getDashDataSourceFactory(): DataSource.Factory? = sharedDashDataSourceFactory

    fun getLiveDashDataSourceFactory(): DataSource.Factory? = sharedLiveDashDataSourceFactory

    fun getProgressiveDataSourceFactory(): DataSource.Factory? = sharedProgressiveDataSourceFactory

    fun getHlsDataSourceFactory(): DataSource.Factory? = sharedHlsDataSourceFactory

    fun getLiveHlsDataSourceFactory(): DataSource.Factory? = sharedLiveHlsDataSourceFactory

    fun getCacheSize(): Long = cache?.cacheSpace ?: 0L

    fun clearVideo(videoId: String) {
        val cache = cache ?: return
        val keys = videoCacheKeys.takeKeys(videoId)
        keys.forEach { key ->
            runCatching { cache.removeResource(key) }.onFailure {
                Log.w(
                    TAG,
                    "Could not clear a cached stream of $videoId",
                    it
                )
            }
        }
        Log.d(TAG, "Cleared ${keys.size} cached streams of $videoId")
    }

    fun release() {
        try {
            cache = null
            sharedDataSourceFactory = null
            sharedDashDataSourceFactory = null
            sharedProgressiveDataSourceFactory = null
            sharedHlsDataSourceFactory = null
            sharedLiveDashDataSourceFactory = null
            sharedLiveHlsDataSourceFactory = null
            Log.d(TAG, "Cache references cleared (SimpleCache lifecycle managed by SharedPlayerCacheProvider)")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing cache", e)
        }
    }

    fun isCacheAvailable(): Boolean = cache != null

    fun registerStreams(
        videoId: String,
        urls: Collection<String?>,
    ) = refusedStreams.register(videoId, urls)

    private suspend fun resolveReplacement(
        videoId: String,
        kind: StreamDenialKind,
    ): List<PlayerResponse.StreamingData.Format>? {
        val result =
            when (kind) {
                StreamDenialKind.URL_EXPIRED -> InnerTubeVideoStreamExtractor.extract(videoId)
                else -> TizenStreamResolver.resolve(videoId)
            } ?: return null
        return result.videoFormats + result.audioFormats
    }

    private fun DataSource.Factory.swappingRefusals(): DataSource.Factory =
        RefusedStreamRetryDataSource.Factory(
            ResolvingDataSource.Factory(this, refusedStreams::resolveDataSpec),
            refusedStreams::onRefused,
        )

    private fun FakeStreamWall?.around(http: DataSource.Factory): DataSource.Factory =
        this?.let { FakeStreamWallDataSource.Factory(http, it) } ?: http
}
