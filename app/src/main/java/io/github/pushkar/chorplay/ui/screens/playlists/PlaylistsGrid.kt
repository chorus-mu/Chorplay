package io.github.pushkar.chorplay.ui.screens.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.model.PlaylistInfo
import io.github.pushkar.chorplay.ui.components.PlaylistCard
import io.github.pushkar.chorplay.ui.components.PlaylistCardLayout
import io.github.pushkar.chorplay.ui.components.layout.chorBottomContentPadding
import io.github.pushkar.chorplay.ui.components.library.libraryGridLayoutFor
import io.github.pushkar.chorplay.ui.components.shared.ChorplayEmptyState
import io.github.pushkar.chorplay.ui.components.shared.MediaKind

private val GridCellMinWidth = 160.dp
private val CompactCellMinWidth = 360.dp
private val GridSpacing = 16.dp
private val GridSidePadding = 16.dp
private val GridTopPadding = 8.dp

internal class PlaylistsGridActions(
    val onVideoClick: (PlaylistInfo) -> Unit,
    val onVideoDelete: (PlaylistInfo) -> Unit,
)

@Composable
internal fun PlaylistsGrid(
    kind: MediaKind,
    playlists: List<PlaylistInfo>,
    compact: Boolean,
    actions: PlaylistsGridActions,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val videoGrid = remember(maxWidth) { libraryGridLayoutFor(maxWidth) }
        val videoCards = !compact && kind == MediaKind.Videos && videoGrid.isGrid
        val sidePadding =
            when {
                compact -> 0.dp
                videoCards -> videoGrid.padding
                else -> GridSidePadding
            }
        val partialRows = remember(playlists.size, videoGrid) { videoGrid.partialRows(playlists.size) }
        LazyVerticalGrid(
            columns =
                when {
                    compact -> GridCells.Adaptive(CompactCellMinWidth)
                    videoCards -> GridCells.Fixed(videoGrid.columns)
                    else -> GridCells.Adaptive(GridCellMinWidth)
                },
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = sidePadding,
                    top = GridTopPadding,
                    end = sidePadding,
                    bottom = chorBottomContentPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else GridSpacing),
            horizontalArrangement = Arrangement.spacedBy(if (videoCards) videoGrid.spacing else GridSpacing),
        ) {
            when {
                playlists.isEmpty() -> emptyPlaylists()
                compact -> compactPlaylists(playlists, actions)
                else -> videoPlaylistCards(playlists, videoCards, partialRows, actions)
            }
        }
    }
}

private fun LazyGridScope.emptyPlaylists() {
    item(
        key = "empty-video-playlists",
        span = { GridItemSpan(maxLineSpan) },
        contentType = "empty",
    ) {
        ChorplayEmptyState(
            title = stringResource(R.string.no_playlists_found),
            icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
        )
    }
}

private fun LazyGridScope.compactPlaylists(
    playlists: List<PlaylistInfo>,
    actions: PlaylistsGridActions,
) {
    items(
        items = playlists,
        key = { "compact-${it.id}" },
        contentType = { "compact-playlist" },
    ) { playlist ->
        PlaylistCompactRow(
            playlist = playlist,
            isMusic = false,
            onClick = { actions.onVideoClick(playlist) },
            trailing = {
                PlaylistRowMenuButton(
                    onDownload = null,
                    onRename = null,
                    onDelete = { actions.onVideoDelete(playlist) },
                )
            },
        )
    }
}

private fun LazyGridScope.videoPlaylistCards(
    playlists: List<PlaylistInfo>,
    videoCards: Boolean,
    partialRows: Collection<Int>,
    actions: PlaylistsGridActions,
) {
    itemsIndexed(
        items = playlists,
        key = { _, playlist -> "video-${playlist.id}" },
        contentType = { _, _ -> "video-playlist" },
        span = { index, _ ->
            GridItemSpan(if (videoCards && index !in partialRows) 1 else maxLineSpan)
        },
    ) { index, playlist ->
        PlaylistCard(
            playlist = playlist,
            onClick = { actions.onVideoClick(playlist) },
            onDeleteClick = { actions.onVideoDelete(playlist) },
            layout =
                if (videoCards && index !in partialRows) {
                    PlaylistCardLayout.SHELF
                } else {
                    PlaylistCardLayout.LIST
                },
            useInternalPadding = false,
        )
    }
}
