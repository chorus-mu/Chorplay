package io.github.pushkar.chorplay.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.audio.eq.EqState
import io.github.pushkar.chorplay.data.local.MiniBarSwipeAction
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.shorts.queue.ShortsQueueSource
import io.github.pushkar.chorplay.player.EnhancedPlayerManager
import io.github.pushkar.chorplay.player.GlobalPlayerState
import io.github.pushkar.chorplay.ui.components.equalizer.LocalEqualizerState
import io.github.pushkar.chorplay.ui.components.layout.ChorplayBottomInsets
import io.github.pushkar.chorplay.ui.components.layout.LocalChorplayBottomInsets
import io.github.pushkar.chorplay.ui.components.layout.navigation.MediaNavigator
import io.github.pushkar.chorplay.ui.components.shared.LocalMediaOpenOrigins
import io.github.pushkar.chorplay.ui.components.shared.MediaMiniBarBounds
import io.github.pushkar.chorplay.ui.components.shared.MediaOpenOrigins
import io.github.pushkar.chorplay.ui.components.shared.quickactions.QuickActionsHost
import io.github.pushkar.chorplay.ui.components.shared.quickactions.sharedQuickActionsViewModel
import io.github.pushkar.chorplay.ui.components.videoplayer.BackgroundQueueTransport
import io.github.pushkar.chorplay.ui.components.videoplayer.PlayerDraggableState
import io.github.pushkar.chorplay.ui.components.videoplayer.PlayerSheetValue
import io.github.pushkar.chorplay.ui.components.videoplayer.SheetOpenOrigin
import io.github.pushkar.chorplay.ui.components.videoplayer.VideoBackgroundBar
import io.github.pushkar.chorplay.ui.components.videoplayer.VideoBarSwipeActions
import io.github.pushkar.chorplay.ui.screens.player.VideoPlayerHost
import io.github.pushkar.chorplay.ui.screens.player.VideoPlayerViewModel
import io.github.pushkar.chorplay.ui.screens.player.dialogs.PlayerQueueSheetHost
import io.github.pushkar.chorplay.ui.screens.player.state.VideoPlayerUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Composable
internal fun ChorPlayerSessionEffects(
    playerSheetState: PlayerDraggableState,
    playerViewModel: VideoPlayerViewModel,
    playerUiStateResult: State<VideoPlayerUiState>,
    playerVisibleState: MutableState<Boolean>,
    isInPipMode: Boolean,
    openOrigins: MediaOpenOrigins,
) {
    val activity = LocalContext.current as? ComponentActivity
    val playerUiState by playerUiStateResult
    var playerVisible by playerVisibleState
    val currentIsInPipMode by rememberUpdatedState(isInPipMode)
    val enhancedPlayerManager = remember { EnhancedPlayerManager.getInstance() }
    val hasVideoQueue by enhancedPlayerManager.hasQueue.collectAsStateWithLifecycle(
        initialValue = enhancedPlayerManager.playerState.value.queueTitle != null,
    )
    var keepMiniOnQueueAutoAdvance by remember { mutableStateOf(false) }

    LaunchedEffect(playerSheetState.currentValue, playerSheetState.isDragging) {
        if (!playerSheetState.isDragging) {
            when (playerSheetState.currentValue) {
                PlayerSheetValue.Expanded -> {
                    GlobalPlayerState.expandMiniPlayer()
                }

                PlayerSheetValue.Collapsed -> {
                    if (playerUiState.isBackgroundPlaybackMode) {
                        GlobalPlayerState.hideMiniPlayer()
                    } else {
                        GlobalPlayerState.collapseMiniPlayer()
                    }
                }
            }
        }
    }

    LaunchedEffect(playerUiStateResult) {
        var previousVideoId: String? = null
        snapshotFlow { playerUiStateResult.value.cachedVideo?.id }.collect { videoId ->
            if (videoId != null) {
                if (videoId != previousVideoId) {
                    val wasKeepMini = keepMiniOnQueueAutoAdvance
                    keepMiniOnQueueAutoAdvance = false
                    if (playerSheetState.currentValue == PlayerSheetValue.Collapsed && !wasKeepMini) {
                        playerSheetState.snapTo(PlayerSheetValue.Expanded)
                        GlobalPlayerState.expandMiniPlayer()
                    }
                }
                playerVisible = true
            }
            previousVideoId = videoId
        }
    }

    val currentVideo = playerUiState.cachedVideo
    LaunchedEffect(currentVideo?.id) {
        if (currentVideo == null) {
            playerVisible = false
        } else {
            GlobalPlayerState.setCurrentVideo(currentVideo)
        }
    }

    LaunchedEffect(playerVisible) {
        if (!playerVisible) {
            playerSheetState.snapTo(PlayerSheetValue.Collapsed)
            GlobalPlayerState.hideMiniPlayer()
        }
    }

    LaunchedEffect(currentIsInPipMode) {
        if (currentIsInPipMode) {
            playerVisible = false
        }
    }
}

@Composable
internal fun ChorplayPlayerOverlays(
    mediaNavigator: MediaNavigator,
    navController: NavHostController,
    playerViewModel: VideoPlayerViewModel,
    playerUiStateResult: State<VideoPlayerUiState>,
    playerVisibleState: MutableState<Boolean>,
    playerSheetState: PlayerDraggableState,
    activeVideo: Video?,
    isShortsPlayerRoute: Boolean,
    bottomPadding: Dp,
    startInset: Dp,
    miniPlayerScale: Float,
    miniPlayerShowSkipControls: Boolean,
    miniPlayerShowNextPrevControls: Boolean,
    showVideoBar: Boolean,
    containerWidth: Dp,
    containerHeight: Dp,
    miniBarBounds: MediaMiniBarBounds,
    equalizerState: StateFlow<EqState>,
    bottomInsets: ChorplayBottomInsets,
    openOrigins: MediaOpenOrigins,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
) {
    val density = LocalDensity.current
    val playerUiState by playerUiStateResult
    var playerVisible by playerVisibleState
    val context = LocalContext.current
    val playerPreferences = remember(context) { PlayerPreferences(context) }
    val swipeLeftAction by playerPreferences.miniBarSwipeLeftAction.collectAsState(initial = MiniBarSwipeAction.CLOSE)
    val swipeRightAction by playerPreferences.miniBarSwipeRightAction.collectAsState(initial = MiniBarSwipeAction.CLOSE)
    val quickActions = sharedQuickActionsViewModel()
    val removedFromQueue = stringResource(R.string.removed_from_queue)
    val scope = rememberCoroutineScope()
    val closeVideo: () -> Unit = {
        playerVisible = false
        playerViewModel.clearVideo()
    }
    val videoBarActions =
        remember(quickActions, playerViewModel, scope, removedFromQueue) {
            VideoBarSwipeActions(
                video = { playerViewModel.uiState.value.cachedVideo },
                quickActions = quickActions,
                playerViewModel = playerViewModel,
                scope = scope,
                removedFromQueue = removedFromQueue,
                onClose = closeVideo,
            )
        }

    CompositionLocalProvider(
        *mediaNavigationLocals(mediaNavigator),
        LocalMediaOpenOrigins provides openOrigins,
        LocalEqualizerState provides equalizerState,
        LocalChorplayBottomInsets provides bottomInsets,
    ) {
        VideoPlayerHost(
            video = activeVideo,
            isVisible = playerVisible && !isShortsPlayerRoute,
            playerSheetState = playerSheetState,
            bottomPadding = bottomPadding.coerceAtLeast(0.dp),
            startInset = startInset,
            miniPlayerScale = miniPlayerScale,
            miniPlayerShowSkipControls = miniPlayerShowSkipControls,
            miniPlayerShowNextPrevControls = miniPlayerShowNextPrevControls,
            onClose = {
                playerVisible = false
                if (playerUiState.isRestoredSession) {
                    playerViewModel.dismissContinueWatching()
                }
                playerViewModel.clearVideo()
            },
            onMinimize = {
                playerSheetState.snapTo(PlayerSheetValue.Collapsed)
                GlobalPlayerState.hideMiniPlayer()
                playerVisible = false
            },
            onNavigateToChannel = mediaNavigator::openChannel,
            onNavigateToShorts = { videoId ->
                playerSheetState.collapse()
                navController.openShorts(ShortsQueueSource.SeededFeed(videoId))
            },
        )

        val barVideo = playerUiState.cachedVideo
        var showBarQueue by remember { mutableStateOf(false) }
        var barHandoffId by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(barHandoffId) {
            if (barHandoffId == null) return@LaunchedEffect
            withTimeoutOrNull(BAR_HANDOFF_TIMEOUT_MS) {
                snapshotFlow { playerSheetState.openOrigin != null }.first { it }
                repeat(BAR_HANDOFF_FRAMES) { withFrameNanos { } }
            }
            barHandoffId = null
        }
        if ((showVideoBar || barHandoffId == barVideo?.id) && barVideo != null) {
            VideoBackgroundBar(
                video = barVideo,
                bounds = miniBarBounds,
                containerWidthPx = with(density) { containerWidth.toPx() },
                containerHeightPx = with(density) { containerHeight.toPx() },
                restingBottomPx = { bottomInsets.miniPlayerBaselinePx(density) },
                onRestore = { videoWindowBounds ->
                    openOrigins.holdOriginFor(barVideo.id, videoWindowBounds)
                    barHandoffId = barVideo.id
                    playerViewModel.showVideoPlayer()
                },
                onClose = closeVideo,
                onOpenQueue = { showBarQueue = true },
                swipeLeftAction = swipeLeftAction,
                swipeRightAction = swipeRightAction,
                swipeActions = videoBarActions,
            )
        }
        if (showVideoBar && showBarQueue) {
            PlayerQueueSheetHost(
                asSidePanel = false,
                expandedHeight = null,
                onDismiss = { showBarQueue = false },
                loadStreamsInPlayer = true,
                footer = { BackgroundQueueTransport(videoBarActions) },
            )
        }
        LaunchedEffect(showVideoBar) { if (!showVideoBar) showBarQueue = false }

        QuickActionsHost(snackbarHostState)
    }
}

private const val BAR_HANDOFF_TIMEOUT_MS = 2_500L
private const val BAR_HANDOFF_FRAMES = 2
