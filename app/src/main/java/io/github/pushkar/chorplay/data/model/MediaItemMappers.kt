package io.github.pushkar.chorplay.data.model

import io.github.pushkar.chorplay.data.local.LikedVideoInfo
import io.github.pushkar.chorplay.data.local.VideoHistoryEntry

private const val MILLIS_PER_SECOND = 1000L

fun VideoHistoryEntry.toVideo(): Video =
    Video(
        id = videoId,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnailUrl = thumbnailUrl,
        duration = (duration / MILLIS_PER_SECOND).toInt(),
        viewCount = -1L,
        uploadDate = "",
        timestamp = timestamp,
        isShort = isShort,
    )

fun LikedVideoInfo.toVideo(): Video =
    Video(
        id = videoId,
        title = title,
        channelName = channelName,
        channelId = channelId.orEmpty(),
        thumbnailUrl = thumbnail,
        duration = durationSeconds,
        viewCount = -1L,
        uploadDate = "",
        timestamp = likedAt,
        isMusic = isMusic,
    )
