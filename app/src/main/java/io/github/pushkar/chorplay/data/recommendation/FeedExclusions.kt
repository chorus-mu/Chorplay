package io.github.pushkar.chorplay.data.recommendation

import io.github.pushkar.chorplay.data.model.Video

class FeedExclusions(
    val suppressedVideoIds: Set<String> = emptySet(),
    val blockedChannelIds: Set<String> = emptySet(),
    val suppressedChannelIds: Set<String> = emptySet(),
    private val blockedText: (title: String, channelName: String) -> Boolean = { _, _ -> false },
) {
    val isEmpty: Boolean
        get() = suppressedVideoIds.isEmpty() && blockedChannelIds.isEmpty() && suppressedChannelIds.isEmpty()

    fun hides(video: Video): Boolean = video.id in suppressedVideoIds || hidesChannel(video.channelId)

    fun hidesChannel(channelId: String): Boolean = channelId.isNotBlank() && channelId in blockedChannelIds

    fun hidesFromRecommendations(video: Video): Boolean =
        hides(video) ||
                (video.channelId.isNotBlank() && video.channelId in suppressedChannelIds) ||
                blockedText(video.title, video.channelName)

    fun hidingChannels(channelIds: Set<String>): FeedExclusions =
        if (channelIds.isEmpty()) {
            this
        } else {
            FeedExclusions(suppressedVideoIds, blockedChannelIds + channelIds, suppressedChannelIds, blockedText)
        }

    companion object {
        val NONE = FeedExclusions()
    }
}

internal fun suppressVideo(
    suppressed: Map<String, Long>,
    videoId: String,
    now: Long,
    max: Int = ChorplayNeuroEngine.MAX_SUPPRESSED_VIDEOS,
): Map<String, Long> {
    val updated = suppressed + (videoId to now)
    if (updated.size <= max) return updated
    return updated.entries
        .sortedByDescending { it.value }
        .take(max)
        .associate { it.key to it.value }
}

internal fun UserBrain.keepingHiddenContent(): UserBrain =
    UserBrain(
        blockedChannels = blockedChannels,
        blockedTopics = blockedTopics,
        suppressedVideoIds = suppressedVideoIds,
        suppressedChannels = suppressedChannels,
    )
