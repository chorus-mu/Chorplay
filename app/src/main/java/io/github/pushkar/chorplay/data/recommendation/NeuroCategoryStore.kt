package io.github.pushkar.chorplay.data.recommendation

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import io.github.pushkar.chorplay.data.profile.ProfileFiles
import io.github.pushkar.chorplay.data.profile.StoreScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

internal class NeuroCategoryStore(
    private val appContext: Context,
) {
    companion object {
        private const val TAG = "ChorplayNeuroEngine"
        private const val MAX_CHANNELS = 1000
    }

    @Serializable
    data class StoredChannel(
        val counts: Map<String, Int> = emptyMap(),
        val at: Long = 0L,
    )

    @Serializable
    data class CategoryIndex(
        val channels: Map<String, StoredChannel> = emptyMap(),
    )

    private object CategorySerializer : Serializer<CategoryIndex> {
        override val defaultValue: CategoryIndex = CategoryIndex()

        override suspend fun readFrom(input: InputStream): CategoryIndex =
            try {
                val text = input.bufferedReader().readText()
                if (text.isBlank()) defaultValue else Json { ignoreUnknownKeys = true }.decodeFromString<CategoryIndex>(
                    text
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read category index", e)
                defaultValue
            }

        override suspend fun writeTo(
            t: CategoryIndex,
            output: OutputStream,
        ) {
            output.write(Json.encodeToString(t).toByteArray())
        }
    }

    private val categoryDataStore: DataStore<CategoryIndex> by lazy {
        DataStoreFactory.create(
            serializer = CategorySerializer,
            produceFile = {
                ProfileFiles.dataStore(
                    appContext,
                    StoreScope.PROFILE_PRIVATE,
                    "flow_neuro_categories_v1.json"
                )
            },
        )
    }

    @Volatile
    private var channels: Map<String, StoredChannel> = emptyMap()

    @Volatile
    private var dirty = false

    suspend fun load() {
        withContext(Dispatchers.IO) {
            try {
                channels =
                    categoryDataStore.data
                        .first()
                        .channels
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load category index", e)
            }
        }
    }

    fun categoryOf(channelId: String): ChannelCategory? {
        val counts = channels[channelId]?.counts ?: return null
        val top = counts.maxByOrNull { it.value } ?: return null
        return ChannelCategory(top.key, top.value / (counts.values.sum() + 1.0))
    }

    fun size(): Int = channels.size

    fun index(): CategoryIndex = CategoryIndex(channels)

    fun restore(index: CategoryIndex) {
        channels = index.channels
    }

    fun record(
        channelId: String,
        categoryId: String,
        now: Long = System.currentTimeMillis(),
    ) {
        if (channelId.isBlank()) return
        val existing = channels[channelId]?.counts.orEmpty()
        val updated =
            channels + (channelId to StoredChannel(existing + (categoryId to (existing[categoryId] ?: 0) + 1), now))
        channels =
            if (updated.size > MAX_CHANNELS) {
                updated.entries
                    .sortedByDescending { it.value.at }
                    .take(MAX_CHANNELS)
                    .associate { it.key to it.value }
            } else {
                updated
            }
        dirty = true
    }

    suspend fun persistIfDirty() {
        if (!dirty) return
        dirty = false
        withContext(Dispatchers.IO) {
            try {
                val snapshot = channels
                categoryDataStore.updateData { CategoryIndex(snapshot) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist category index", e)
            }
        }
    }

    suspend fun clear() {
        channels = emptyMap()
        dirty = false
        withContext(Dispatchers.IO) {
            try {
                categoryDataStore.updateData { CategoryIndex() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear category index", e)
            }
        }
    }
}
