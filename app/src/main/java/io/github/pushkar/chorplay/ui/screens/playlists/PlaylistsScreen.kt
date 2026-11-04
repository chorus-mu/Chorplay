package io.github.pushkar.chorplay.ui.screens.playlists

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.model.PlaylistInfo
import io.github.pushkar.chorplay.data.playlist.PlaylistImport
import io.github.pushkar.chorplay.data.playlist.PlaylistListOrder
import io.github.pushkar.chorplay.ui.components.layout.LocalChorplayBottomInsets
import io.github.pushkar.chorplay.ui.components.layout.floatAboveBottomChrome
import io.github.pushkar.chorplay.ui.components.layout.topbar.ChorplayTopBar
import io.github.pushkar.chorplay.ui.components.library.PlaylistCreationFabMenu
import io.github.pushkar.chorplay.ui.components.library.PlaylistCreationTarget
import io.github.pushkar.chorplay.ui.components.library.PlaylistLibraryFilterRow
import io.github.pushkar.chorplay.ui.components.library.PlaylistOwnershipFilter
import io.github.pushkar.chorplay.ui.components.library.message
import io.github.pushkar.chorplay.ui.components.shared.CollectionEditDialog
import io.github.pushkar.chorplay.ui.components.shared.DeleteCollectionDialog
import io.github.pushkar.chorplay.ui.components.shared.MediaKind
import io.github.pushkar.chorplay.ui.components.shared.quickactions.sharedQuickActionsViewModel
import io.github.pushkar.chorplay.utils.PLAYLIST_FILE_MIME_TYPE
import kotlinx.coroutines.launch

private val FabMenuPadding = 16.dp

// Some file pickers label a .json file as plain text or a generic binary.
private val PlaylistFileTypes = arrayOf(PLAYLIST_FILE_MIME_TYPE, "text/plain", "application/octet-stream")

/** Opens the playlists page, video-only. [fixedKind] is kept for navigation compatibility but always treated as [MediaKind.Videos]. */
@Composable
fun PlaylistsScreen(
    onBackClick: () -> Unit,
    onVideoPlaylistClick: (PlaylistInfo) -> Unit,
    modifier: Modifier = Modifier,
    fixedKind: MediaKind? = null,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val videoState by viewModel.uiState.collectAsStateWithLifecycle()
    val listOrder by viewModel.listOrder.collectAsStateWithLifecycle()
    val compact by viewModel.compactLayout.collectAsStateWithLifecycle()
    val contentKind: MediaKind = MediaKind.Videos
    var ownershipFilter by rememberSaveable { mutableStateOf(PlaylistOwnershipFilter.All) }
    var reordering by rememberSaveable { mutableStateOf(false) }
    var creationTarget by remember { mutableStateOf<PlaylistCreationTarget?>(null) }
    var videoToDelete by remember { mutableStateOf<PlaylistInfo?>(null) }

    val ownedPlaylists = videoState.playlists
    val savedPlaylists = videoState.savedPlaylists
    val visiblePlaylists =
        remember(ownedPlaylists, savedPlaylists, ownershipFilter, listOrder) {
            orderedPlaylists(ownedPlaylists, savedPlaylists, ownershipFilter, listOrder)
        }
    val isLoading = videoState.isLoading
    val isReordering = reordering && listOrder == PlaylistListOrder.CUSTOM

    val context = LocalContext.current
    val quickActions = sharedQuickActionsViewModel()
    val scope = rememberCoroutineScope()
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val result = viewModel.importPlaylist(uri, context.getString(R.string.imported_playlist_default_name))
                quickActions.announce(result.message(context))
                if (result is PlaylistImport.Imported) {
                    val info =
                        PlaylistInfo(
                            id = result.playlistId,
                            name = result.name,
                            description = "",
                            videoCount = result.videoCount,
                            thumbnailUrl = "",
                            isPrivate = true,
                            createdAt = System.currentTimeMillis(),
                        )
                    onVideoPlaylistClick(info)
                }
            }
        }

    BackHandler(enabled = isReordering) { reordering = false }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ChorplayTopBar(
                title =
                    stringResource(
                        when {
                            isReordering -> R.string.playlists_reorder
                            fixedKind == MediaKind.Videos -> R.string.library_video_playlists
                            else -> R.string.library_playlists_label
                        },
                    ),
                onBack = if (isReordering) ({ reordering = false }) else onBackClick,
                actions = {
                    if (isReordering) {
                        TextButton(onClick = { reordering = false }) { Text(stringResource(R.string.done)) }
                    } else {
                        if (listOrder == PlaylistListOrder.CUSTOM) {
                            IconButton(onClick = { reordering = true }) {
                                Icon(Icons.Rounded.SwapVert, contentDescription = stringResource(R.string.playlists_reorder))
                            }
                        }
                        IconButton(onClick = { viewModel.setCompactLayout(!compact) }) {
                            Icon(
                                imageVector = if (compact) Icons.Rounded.GridView else Icons.Rounded.ViewAgenda,
                                contentDescription =
                                    stringResource(if (compact) R.string.playlists_show_grid else R.string.playlists_show_list),
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isReordering) {
                    PlaylistLibraryFilterRow(
                        selectedKind = contentKind,
                        onKindSelected = {},
                        showKinds = false,
                        selectedOwnership = ownershipFilter,
                        onOwnershipSelected = { ownershipFilter = it },
                        selectedOrder = listOrder,
                        onOrderSelected = viewModel::setListOrder,
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        isReordering -> {
                            PlaylistsReorderList(
                                playlists =
                                    remember(ownedPlaylists, savedPlaylists) {
                                        orderedPlaylists(
                                            ownedPlaylists,
                                            savedPlaylists,
                                            PlaylistOwnershipFilter.All,
                                            PlaylistListOrder.CUSTOM,
                                        )
                                    },
                                isMusic = false,
                                onOrderChanged = viewModel::reorderPlaylists,
                            )
                        }

                        else -> {
                            PlaylistsGrid(
                                kind = contentKind,
                                playlists = visiblePlaylists,
                                compact = compact,
                                actions =
                                    remember(onVideoPlaylistClick) {
                                        PlaylistsGridActions(
                                            onVideoClick = onVideoPlaylistClick,
                                            onVideoDelete = { videoToDelete = it },
                                        )
                                    },
                            )
                        }
                    }
                }
            }

            if (!isReordering) {
                PlaylistCreationFabMenu(
                    onCreateVideo = { creationTarget = PlaylistCreationTarget.Video },
                    onImport = { importLauncher.launch(PlaylistFileTypes) },
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .floatAboveBottomChrome(LocalChorplayBottomInsets.current)
                            .padding(FabMenuPadding),
                )
            }
        }
    }

    creationTarget?.let { target ->
        CollectionEditDialog(
            title = stringResource(R.string.create_new_playlist),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { creationTarget = null },
            onConfirm = { name, description ->
                viewModel.createPlaylist(name, description)
                creationTarget = null
            },
        )
    }

    videoToDelete?.let { playlist ->
        DeleteCollectionDialog(
            collectionName = playlist.name,
            onDismiss = { videoToDelete = null },
            onConfirm = {
                viewModel.deletePlaylist(playlist.id)
                videoToDelete = null
            },
        )
    }
}
