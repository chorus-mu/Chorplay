package io.github.pushkar.chorplay.data.recommendation.snapshot

import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.recommendation.NeuroCategoryStore
import io.github.pushkar.chorplay.data.recommendation.NeuroCoWatchStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class HomeSnapshot(
    val format: String = FORMAT,
    val appVersion: String,
    val keySpec: Int,
    val createdAt: Long,
    val brain: JsonElement,
    val categories: NeuroCategoryStore.CategoryIndex,
    val coWatch: NeuroCoWatchStore.CoWatchIndex,
    val subscriptions: List<String>,
    val watched: List<String>,
    val discovery: List<SnapshotSearch>,
    val seeds: List<SnapshotSeed>,
    val related: List<SnapshotRelated>,
    val subscriptionFeed: List<SnapshotVideo>,
    val memory: List<SnapshotVideo>,
    val page: List<SnapshotCard>,
    val shown: List<SnapshotShown> = emptyList(),
) {
    companion object {
        const val FORMAT = "chorplay-home-snapshot/1"
    }
}

@Serializable
internal data class SnapshotSearch(
    val query: String,
    val videos: List<SnapshotVideo>,
)

@Serializable
internal data class SnapshotSeed(
    val id: String,
    val title: String,
)

@Serializable
internal data class SnapshotRelated(
    val video: SnapshotVideo,
    val seedId: String,
    val seedScore: Double,
    val graphRank: Int,
    val seedCluster: String,
    val seedResultCount: Int,
    val hitCount: Int = 1,
)

@Serializable
internal data class SnapshotCard(
    val id: String,
    val source: String?,
    val padding: Boolean = false,
)

@Serializable
internal data class SnapshotShown(
    val video: SnapshotVideo,
    val origin: String?,
    val padding: Boolean = false,
    val detail: String? = null,
)

@Serializable
internal data class SnapshotVideo(
    val id: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val duration: Int,
    val views: Long,
    val uploadDate: String,
    val timestamp: Long,
    val isShort: Boolean = false,
    val isLive: Boolean = false,
    val isUpcoming: Boolean = false,
    val verified: Boolean = false,
    val badges: List<String> = emptyList(),
    val category: String? = null,
) {
    fun toVideo(shiftMs: Long = 0L): Video =
        Video(
            id = id,
            title = title,
            channelName = channelName,
            channelId = channelId,
            thumbnailUrl = "",
            duration = duration,
            viewCount = views,
            uploadDate = uploadDate,
            timestamp = timestamp + shiftMs,
            category = category,
            isLive = isLive,
            isShort = isShort,
            isUpcoming = isUpcoming,
            isVerifiedChannel = verified,
            badges = badges,
        )

    companion object {
        fun of(video: Video) =
            SnapshotVideo(
                id = video.id,
                title = video.title,
                channelId = video.channelId,
                channelName = video.channelName,
                duration = video.duration,
                views = video.viewCount,
                uploadDate = video.uploadDate,
                timestamp = video.timestamp,
                isShort = video.isShort,
                isLive = video.isLive,
                isUpcoming = video.isUpcoming,
                verified = video.isVerifiedChannel,
                badges = video.badges,
                category = video.category,
            )
    }
}
