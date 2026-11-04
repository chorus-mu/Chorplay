package io.github.pushkar.chorplay.ui.tv.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.PlaylistRepository
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.video.DownloadedVideo
import io.github.pushkar.chorplay.ui.components.layout.chorBottomContentPadding
import io.github.pushkar.chorplay.ui.components.library.LibrarySection
import io.github.pushkar.chorplay.ui.components.shared.ChorplayEmptyState
import io.github.pushkar.chorplay.ui.components.shared.MediaKind
import io.github.pushkar.chorplay.ui.screens.library.LibraryViewModel
import io.github.pushkar.chorplay.ui.screens.library.libraryShelves
import io.github.pushkar.chorplay.ui.screens.library.visibleContentSections
import io.github.pushkar.chorplay.ui.tv.components.TvScreenFrame

private val NOT_ON_TV =
    setOf(
        LibrarySection.LOCAL_MEDIA,
        LibrarySection.SAVED_SHORTS,
    )

private val ShelfSpacing = 24.dp

@Composable
fun TvLibraryScreen(
    onVideoClick: (Video) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPlaylists: (MediaKind?) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenDownloads: () -> Unit,
    onDownloadedVideoClick: (List<DownloadedVideo>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val separatePlaylistKinds by viewModel.separatePlaylistKinds.collectAsStateWithLifecycle()
    val isLibraryEmpty by viewModel.isLibraryEmpty.collectAsStateWithLifecycle()
    val hiddenSections by viewModel.hiddenSections.collectAsStateWithLifecycle()

    TvScreenFrame(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = chorBottomContentPadding(extra = 24.dp)),
            verticalArrangement = Arrangement.spacedBy(ShelfSpacing),
        ) {
            val hidden = hiddenSections ?: return@LazyColumn
            if (isLibraryEmpty) {
                item(key = "library-empty", contentType = "empty") {
                    ChorplayEmptyState(
                        title = stringResource(R.string.library_empty_title),
                        subtitle = stringResource(R.string.library_empty_body),
                        icon = Icons.Outlined.VideoLibrary,
                    )
                }
                return@LazyColumn
            }
            libraryShelves(
                viewModel = viewModel,
                sections = visibleContentSections(
                    hidden,
                    separatePlaylistKinds,
                    shortsEnabled = false
                ).filterNot { it in NOT_ON_TV },
                onOpenSection = { section ->
                    when (section) {
                        LibrarySection.HISTORY -> onOpenHistory()
                        LibrarySection.PLAYLISTS -> onOpenPlaylists(null)
                        LibrarySection.VIDEO_PLAYLISTS -> onOpenPlaylists(MediaKind.Videos)
                        LibrarySection.WATCH_LATER -> onOpenPlaylist(PlaylistRepository.WATCH_LATER_ID)
                        LibrarySection.LIKED_VIDEOS -> onOpenPlaylist(PlaylistRepository.LIKED_VIDEOS_ID)
                        LibrarySection.DOWNLOADS -> onOpenDownloads()
                        else -> Unit
                    }
                },
                onVideoClick = onVideoClick,
                onPlaylistClick = onOpenPlaylist,
                onDownloadedVideoClick = onDownloadedVideoClick,
                onSavedShortClick = onVideoClick,
            )
        }
    }
}
