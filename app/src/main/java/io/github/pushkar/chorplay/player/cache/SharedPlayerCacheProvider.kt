package io.github.pushkar.chorplay.player.cache

import android.content.Context
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import io.github.pushkar.chorplay.player.config.PlayerConfig
import java.io.File

@UnstableApi
object SharedPlayerCacheProvider {
    private const val TAG = "SharedPlayerCache"

    @Volatile
    private var cache: SimpleCache? = null

    @Volatile
    private var database: DatabaseProvider? = null

    @Synchronized
    fun databaseProvider(context: Context): DatabaseProvider =
        database ?: StandaloneDatabaseProvider(context.applicationContext).also { database = it }

    @Synchronized
    fun getOrCreate(
        context: Context,
        maxCacheSizeBytes: Long = PlayerConfig.CACHE_SIZE_BYTES,
    ): SimpleCache = cache ?: create(context, PlayerConfig.CACHE_DIR_NAME, maxCacheSizeBytes).also { cache = it }

    fun existing(): SimpleCache? = cache

    @Synchronized
    fun release() {
        cache?.release()
        cache = null
        Log.d(TAG, "Player caches released")
    }

    private fun create(
        context: Context,
        folder: String,
        maxCacheSizeBytes: Long,
    ): SimpleCache {
        val cacheDir = File(context.applicationContext.cacheDir, folder)
        val evictor =
            if (maxCacheSizeBytes <= 0) NoOpCacheEvictor() else LeastRecentlyUsedCacheEvictor(maxCacheSizeBytes)
        Log.d(TAG, "Creating SimpleCache: dir=$cacheDir, maxBytes=$maxCacheSizeBytes")
        return SimpleCache(cacheDir, evictor, databaseProvider(context))
    }
}
