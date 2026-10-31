package io.github.pushkar.chorplay.ui.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.ui.components.library.LibrarySection

@Composable
internal fun LibrarySection.subtitle(counts: LibraryCounts?): String? =
    when (this) {
        LibrarySection.LOCAL_MEDIA -> {
            stringResource(R.string.library_local_media_subtitle)
        }

        LibrarySection.SETTINGS -> {
            stringResource(R.string.library_settings_subtitle)
        }

        LibrarySection.NOTES -> {
            stringResource(R.string.notes_library_subtitle)
        }

        LibrarySection.HISTORY -> {
            counts?.let { itemsSubtitle(it.history) }
        }

        LibrarySection.PLAYLISTS,
        LibrarySection.VIDEO_PLAYLISTS -> {
            counts?.let { playlistsSubtitle(it.videoPlaylists) }
        }

        LibrarySection.WATCH_LATER -> {
            counts?.let {
                pluralStringResource(R.plurals.videos_count_template, it.watchLater, it.watchLater)
            }
        }

        LibrarySection.LIKED_VIDEOS -> {
            counts?.let {
                pluralStringResource(R.plurals.videos_count_template, it.likedVideos, it.likedVideos)
            }
        }

        LibrarySection.SAVED_SHORTS -> {
            counts?.let {
                pluralStringResource(R.plurals.shorts_count_template, it.savedShorts, it.savedShorts)
            }
        }

        LibrarySection.DOWNLOADS -> {
            counts?.let { downloadsSubtitle(it) }
        }
    }

@Composable
private fun playlistsSubtitle(count: Int): String =
    pluralStringResource(R.plurals.playlists_count_template, count, count)

@Composable
private fun itemsSubtitle(count: Int): String = pluralStringResource(R.plurals.library_items_count, count, count)

@Composable
private fun downloadsSubtitle(counts: LibraryCounts): String {
    val videos = counts.downloadedVideos
    if (videos == 0) return stringResource(R.string.empty_downloads)
    return pluralStringResource(R.plurals.videos_count_template, videos, videos)
}
