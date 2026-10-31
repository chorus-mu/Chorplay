package io.github.pushkar.chorplay.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import io.github.pushkar.chorplay.player.RestoredMusicQueue
import io.github.pushkar.chorplay.player.musicAudioUri
import io.github.pushkar.chorplay.player.musicMediaItem

/**
 * The saved music queue as Media3 playback resumption wants it, so the system media controls and a
 * headset's play button bring music back after Chorplay was closed or killed. The items are the ones the
 * app itself builds, and the song starts where it stopped.
 */
@OptIn(UnstableApi::class)
internal fun musicResumptionItems(restored: RestoredMusicQueue): MediaSession.MediaItemsWithStartPosition {
    val items = restored.queue.map { track -> musicMediaItem(track, musicAudioUri(track.videoId)) }
    val trackIndex = restored.track?.let { track -> restored.queue.indexOfFirst { it.videoId == track.videoId } } ?: -1
    return MediaSession.MediaItemsWithStartPosition(
        items,
        if (trackIndex >= 0) trackIndex else restored.index,
        restored.resumePositionMs,
    )
}
