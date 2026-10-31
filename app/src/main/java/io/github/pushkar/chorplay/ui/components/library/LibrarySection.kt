package io.github.pushkar.chorplay.ui.components.library

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import io.github.pushkar.chorplay.R

internal enum class LibrarySection(
    @StringRes val titleRes: Int,
) {
    HISTORY(R.string.library_history_label),
    PLAYLISTS(R.string.library_playlists_label),
    VIDEO_PLAYLISTS(R.string.library_video_playlists),
    WATCH_LATER(R.string.library_watch_later_label),
    LIKED_VIDEOS(R.string.liked_videos_playlist),
    DOWNLOADS(R.string.library_downloads_label),
    SAVED_SHORTS(R.string.library_saved_shorts_label),
    LOCAL_MEDIA(R.string.library_local_media_label),
    NOTES(R.string.notes_title),
    SETTINGS(R.string.settings),
    ;

    val icon: ImageVector
        @Composable get() =
            when (this) {
                HISTORY -> Icons.Outlined.History
                PLAYLISTS, VIDEO_PLAYLISTS -> Icons.AutoMirrored.Outlined.PlaylistPlay
                WATCH_LATER -> Icons.Outlined.WatchLater
                LIKED_VIDEOS -> Icons.Outlined.ThumbUp
                DOWNLOADS -> Icons.Outlined.Download
                SAVED_SHORTS -> ImageVector.vectorResource(R.drawable.ic_shorts)
                LOCAL_MEDIA -> Icons.Outlined.PermMedia
                NOTES -> Icons.Outlined.StickyNote2
                SETTINGS -> Icons.Outlined.Settings
            }

    val title: String
        @Composable get() = stringResource(titleRes)

    companion object {
        fun content(separatePlaylistKinds: Boolean = false): List<LibrarySection> =
            listOf(HISTORY, PLAYLISTS, WATCH_LATER, LIKED_VIDEOS, DOWNLOADS, SAVED_SHORTS)

        val settingsAndData: List<LibrarySection> = listOf(LOCAL_MEDIA, NOTES, SETTINGS)

        fun fromStored(names: Set<String>): Set<LibrarySection> = entries.filterTo(HashSet()) { it.name in names }
    }
}
