package io.github.pushkar.chorplay.ui.components.library

import io.github.pushkar.chorplay.data.local.LikedVideoInfo
import io.github.pushkar.chorplay.data.local.VideoHistoryEntry
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.model.toVideo
import io.github.pushkar.chorplay.data.video.DownloadedVideo

internal const val LIBRARY_SHELF_ITEM_LIMIT = 20

internal sealed interface LibraryMediaItem {
    val key: String

    data class VideoItem(
        val video: Video,
    ) : LibraryMediaItem {
        override val key: String = "video:${video.id}"
    }

    data class DownloadedVideoItem(
        val download: DownloadedVideo,
    ) : LibraryMediaItem {
        override val key: String = "downloaded-video:${download.video.id}"
    }
}

internal fun VideoHistoryEntry.toLibraryMediaItem(): LibraryMediaItem =
    LibraryMediaItem.VideoItem(toVideo())

internal fun LikedVideoInfo.toLibraryMediaItem(): LibraryMediaItem =
    LibraryMediaItem.VideoItem(toVideo())
