package io.github.pushkar.chorplay.ui.screens.recap

import io.github.pushkar.chorplay.data.localmedia.LocalMediaIds
import io.github.pushkar.chorplay.data.stats.RankedItem
import io.github.pushkar.chorplay.data.stats.RankedKind
import io.github.pushkar.chorplay.utils.YouTubeLink

/** Where tapping a ranked row goes; null for rows with no page, such as topics or name-only artists. */
internal fun RankedItem.link(): YouTubeLink? {
    if (id.isBlank() || LocalMediaIds.isLocal(id)) return null
    return when (kind) {
        RankedKind.CHANNEL -> YouTubeLink.Channel(id, isMusic = false)
        RankedKind.VIDEO -> YouTubeLink.Video(id, isMusic = false)
        RankedKind.ARTIST -> YouTubeLink.Channel(id, isMusic = true).takeIf { id.startsWith(BROWSE_ID_PREFIX) }
        RankedKind.TRACK -> YouTubeLink.Video(id, isMusic = true)
        RankedKind.ALBUM -> YouTubeLink.Album(id)
        RankedKind.OTHER -> null
    }
}

private const val BROWSE_ID_PREFIX = "UC"
