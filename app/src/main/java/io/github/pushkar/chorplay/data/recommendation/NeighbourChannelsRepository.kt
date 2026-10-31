package io.github.pushkar.chorplay.data.recommendation

import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.subscriptions.ChannelUploadsClient
import io.github.pushkar.chorplay.innertube.pages.renderer.FeedItemOwner
import io.github.pushkar.chorplay.utils.PerformanceDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class NeighbourChannel(
    val channelId: String,
    val name: String,
    val pointerNames: List<String>,
)

@Singleton
class NeighbourChannelsRepository
@Inject
constructor(
    private val store: NeuroNeighbourStore,
    private val uploadsClient: ChannelUploadsClient,
) {
    private companion object {
        const val REFRESH_CONCURRENCY = 3
        const val REFRESH_DEADLINE_MS = 15_000L
    }

    suspend fun neighbours(
        subscribed: Map<String, String>,
        now: Long = System.currentTimeMillis(),
    ): List<NeighbourChannel> {
        val dropped = store.entries().filterValues { it.dropped }.keys
        val names = ChorplayNeuroEngine.coWatchChannelNames() + subscribed
        return ChorplayNeuroEngine
            .neighbourChannels(subscribed.keys)
            .filter { it.channelId !in dropped }
            .map { neighbour ->
                NeighbourChannel(
                    channelId = neighbour.channelId,
                    name = names[neighbour.channelId].orEmpty(),
                    pointerNames = neighbour.pointers.mapNotNull { names[it]?.takeIf(String::isNotBlank) },
                )
            }
    }

    suspend fun proven(subscribed: Map<String, String>): List<NeighbourChannel> {
        val entries = store.entries()
        return neighbours(subscribed).filter { (entries[it.channelId]?.fetchedAt ?: 0L) > 0L && it.name.isNotBlank() }
    }

    suspend fun storedUploads(
        neighbours: List<NeighbourChannel>,
        now: Long = System.currentTimeMillis(),
    ): List<Video> {
        val entries = store.entries()
        return neighbours.flatMap { neighbour ->
            entries[neighbour.channelId]
                ?.uploads
                .orEmpty()
                .filter { now - it.publishedAt in 0..NeighbourChannels.UPLOAD_WINDOW_MS }
                .map { it.toVideo(neighbour) }
        }
    }

    suspend fun refresh(
        neighbours: List<NeighbourChannel>,
        now: Long = System.currentTimeMillis(),
    ) {
        val entries = store.entries()
        val queue =
            neighbours
                .filter {
                    it.name.isNotBlank() && now - (entries[it.channelId]?.fetchedAt
                        ?: 0L) >= NeighbourChannels.REFRESH_AFTER_MS
                }
                .take(NeighbourChannels.REFRESH_PER_LOAD)
        if (queue.isEmpty()) return

        val fetched = ConcurrentHashMap<NeighbourChannel, List<Video>>()
        val permits = Semaphore(REFRESH_CONCURRENCY)
        withTimeoutOrNull(REFRESH_DEADLINE_MS) {
            coroutineScope {
                queue
                    .map { neighbour ->
                        async(PerformanceDispatcher.networkIO) {
                            permits.withPermit {
                                uploadsClient
                                    .latest(
                                        FeedItemOwner(id = neighbour.channelId, name = neighbour.name),
                                        NeighbourChannels.UPLOADS_PER_CHANNEL,
                                    ).getOrNull()
                                    ?.let { fetched[neighbour] = it }
                            }
                        }
                    }.awaitAll()
            }
        }
        if (fetched.isEmpty()) return
        store.update { current ->
            current +
                    fetched.map { (neighbour, videos) ->
                        val entry = current[neighbour.channelId] ?: NeuroNeighbourStore.Entry()
                        neighbour.channelId to entry.copy(
                            name = neighbour.name,
                            fetchedAt = now,
                            uploads = videos.toStoredUploads(now)
                        )
                    }
        }
    }

    /** Counts one impression for each of [channelIds]; a neighbour ignored too often is dropped. */
    suspend fun noteShown(channelIds: Collection<String>) {
        if (channelIds.isEmpty()) return
        store.update { current ->
            current +
                    channelIds.distinct().map { channelId ->
                        val entry = current[channelId] ?: NeuroNeighbourStore.Entry()
                        val impressions = entry.impressions + 1
                        channelId to entry.copy(
                            impressions = impressions,
                            dropped = impressions >= NeighbourChannels.IGNORED_IMPRESSIONS
                        )
                    }
        }
    }

    /** Drops neighbours none of whose fetched uploads is on the viewer's subjects: popular channels YouTube puts beside anything. */
    suspend fun dropOffTopic(channelIds: Collection<String>) {
        if (channelIds.isEmpty()) return
        store.update { current ->
            current +
                    channelIds.map { channelId ->
                        channelId to (current[channelId] ?: NeuroNeighbourStore.Entry()).copy(
                            dropped = true
                        )
                    }
        }
    }

    suspend fun clear() = store.clear()

    private fun List<Video>.toStoredUploads(now: Long) =
        asSequence()
            .filter { !it.isShort && !it.isUpcoming && !it.isLive && it.membersOnlyText == null }
            .filter { it.timestamp > 0L && now - it.timestamp in 0..NeighbourChannels.UPLOAD_WINDOW_MS }
            .sortedByDescending { it.timestamp }
            .take(NeighbourChannels.UPLOADS_PER_CHANNEL)
            .map {
                NeuroNeighbourStore.StoredUpload(
                    it.id,
                    it.title,
                    it.thumbnailUrl,
                    it.duration,
                    it.timestamp,
                    it.uploadDate,
                    it.viewCount,
                )
            }.toList()

    private fun NeuroNeighbourStore.StoredUpload.toVideo(neighbour: NeighbourChannel) =
        Video(
            id = id,
            title = title,
            channelName = neighbour.name,
            channelId = neighbour.channelId,
            thumbnailUrl = thumbnailUrl,
            duration = duration,
            viewCount = viewCount,
            uploadDate = uploadDate,
            timestamp = publishedAt,
        )
}
