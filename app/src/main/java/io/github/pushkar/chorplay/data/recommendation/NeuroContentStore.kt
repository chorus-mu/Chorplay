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

internal class NeuroContentStore(
    private val appContext: Context,
) {
    companion object {
        private const val TAG = "ChorplayNeuroEngine"
        private const val MAX_ENTRIES = 1500
        private const val TOPICS_PER_ENTRY = 12
    }

    @Serializable
    data class StoredVector(
        val topics: Map<String, Double> = emptyMap(),
        val at: Long = 0L,
    )

    @Serializable
    data class ContentIndex(
        val vectors: Map<String, StoredVector> = emptyMap(),
        val keySpec: Int = 0,
    )

    private object ContentSerializer : Serializer<ContentIndex> {
        override val defaultValue: ContentIndex = ContentIndex()

        override suspend fun readFrom(input: InputStream): ContentIndex =
            try {
                val text = input.bufferedReader().readText()
                if (text.isBlank()) {
                    defaultValue
                } else {
                    Json { ignoreUnknownKeys = true }.decodeFromString<ContentIndex>(text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read content index", e)
                defaultValue
            }

        override suspend fun writeTo(
            t: ContentIndex,
            output: OutputStream,
        ) {
            output.write(Json.encodeToString(t).toByteArray())
        }
    }

    private val contentDataStore: DataStore<ContentIndex> by lazy {
        DataStoreFactory.create(
            serializer = ContentSerializer,
            produceFile = {
                ProfileFiles.dataStore(
                    appContext,
                    StoreScope.PROFILE_PRIVATE,
                    "chorplay_neuro_content_v1.json"
                )
            },
        )
    }

    @Volatile
    private var vectors: Map<String, StoredVector> = emptyMap()

    @Volatile
    private var dirty = false

    suspend fun load() {
        withContext(Dispatchers.IO) {
            try {
                val index = contentDataStore.data.first()
                if (index.keySpec < NeuroKeySpec.VERSION) {
                    vectors = emptyMap()
                    dirty = index.vectors.isNotEmpty()
                } else {
                    vectors = index.vectors
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load content index", e)
            }
        }
    }

    fun topicsFor(videoId: String): Map<String, Double>? = vectors[videoId]?.topics
    fun size(): Int = vectors.size
    fun put(
        videoId: String,
        topics: Map<String, Double>,
        now: Long = System.currentTimeMillis(),
    ) {
        if (videoId.isBlank() || topics.isEmpty()) return
        val trimmed =
            topics.entries
                .sortedByDescending { it.value }
                .take(TOPICS_PER_ENTRY)
                .associate { it.key to it.value }
        val updated = vectors.toMutableMap()
        updated[videoId] = StoredVector(trimmed, now)
        vectors =
            if (updated.size > MAX_ENTRIES) {
                updated.entries
                    .sortedByDescending { it.value.at }
                    .take(MAX_ENTRIES)
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
                val snapshot = vectors
                contentDataStore.updateData { ContentIndex(snapshot, NeuroKeySpec.VERSION) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist content index", e)
            }
        }
    }

    suspend fun clear() {
        vectors = emptyMap()
        dirty = false
        withContext(Dispatchers.IO) {
            try {
                contentDataStore.updateData { ContentIndex() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear content index", e)
            }
        }
    }
}
