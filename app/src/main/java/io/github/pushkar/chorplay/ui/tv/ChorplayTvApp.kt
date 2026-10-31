package io.github.pushkar.chorplay.ui.tv

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.player.EnhancedMusicPlayerManager
import io.github.pushkar.chorplay.player.GlobalPlayerState
import io.github.pushkar.chorplay.ui.components.layout.navigation.LocalMediaNavigator
import io.github.pushkar.chorplay.ui.components.music.common.ProvideMusicPlaybackState
import io.github.pushkar.chorplay.ui.components.music.sheet.LocalMusicMenus
import io.github.pushkar.chorplay.ui.components.music.sheet.MusicMenuSheets
import io.github.pushkar.chorplay.ui.components.music.sheet.rememberMusicMenus
import io.github.pushkar.chorplay.ui.components.settings.SettingsTarget
import io.github.pushkar.chorplay.ui.components.shared.LocalFlowSheetPresenter
import io.github.pushkar.chorplay.ui.screens.home.HomeViewModel
import io.github.pushkar.chorplay.ui.screens.music.MusicPlayerViewModel
import io.github.pushkar.chorplay.ui.screens.music.MusicViewModel
import io.github.pushkar.chorplay.ui.screens.player.VideoPlayerViewModel
import io.github.pushkar.chorplay.ui.screens.search.SearchViewModel
import io.github.pushkar.chorplay.ui.screens.subscriptions.SubscriptionsViewModel
import io.github.pushkar.chorplay.ui.tv.components.TvSheetPresenter
import io.github.pushkar.chorplay.ui.tv.music.TvMusicNowPlaying
import io.github.pushkar.chorplay.ui.tv.navigation.TvMediaNavigator
import io.github.pushkar.chorplay.ui.tv.player.TvPlayerScreen
import io.github.pushkar.chorplay.ui.tv.theme.ChorplayTvTheme

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ChorplayTvApp(
    deeplinkVideoId: String? = null,
    isShort: Boolean = false,
    onDeeplinkConsumed: () -> Unit = {},
    startSettings: SettingsTarget? = null,
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val homeViewModel: HomeViewModel = hiltViewModel(activity)
    val playerViewModel: VideoPlayerViewModel = hiltViewModel(activity)
    val subscriptionsViewModel: SubscriptionsViewModel = hiltViewModel(activity)
    val searchViewModel: SearchViewModel = hiltViewModel(activity)
    val musicViewModel: MusicViewModel = hiltViewModel(activity)
    val musicPlayerViewModel: MusicPlayerViewModel = hiltViewModel(activity)
    val shellViewModel: TvShellViewModel = hiltViewModel(activity)
    val navigation by shellViewModel.navigation.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val activeVideo by GlobalPlayerState.currentVideo.collectAsStateWithLifecycle()
    val activeMusicTrack by EnhancedMusicPlayerManager.currentTrack.collectAsStateWithLifecycle()
    val musicPlayerState by EnhancedMusicPlayerManager.playerState.collectAsStateWithLifecycle()
    var musicExpanded by rememberSaveable { mutableStateOf(false) }

    // The manager restores the last session's track (paused) at startup. Only
    // surface the mini player once something has actually played this session.
    var musicSessionActive by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(musicPlayerState.isPlaying) {
        if (musicPlayerState.isPlaying) musicSessionActive = true
    }

    fun play(video: Video) {
        EnhancedMusicPlayerManager.pause()
        GlobalPlayerState.setCurrentVideo(video)
        playerViewModel.playVideo(video)
    }

    fun playPlaylist(
        videos: List<Video>,
        startIndex: Int,
        title: String,
        shuffle: Boolean,
    ) {
        val start = videos.getOrNull(startIndex) ?: return
        EnhancedMusicPlayerManager.pause()
        GlobalPlayerState.setCurrentVideo(start)
        playerViewModel.playPlaylist(videos, startIndex, title, shuffle)
    }

    // Metadata is resolved by playVideo -> loadVideoInfo; the stub only seeds the id.
    fun playById(
        videoId: String,
        short: Boolean = false,
    ) = play(
        Video(
            id = videoId,
            title = "",
            channelName = "",
            channelId = "",
            thumbnailUrl = "",
            duration = 0,
            viewCount = 0L,
            uploadDate = "",
            isShort = short,
        ),
    )

    fun closePlayer() {
        playerViewModel.clearVideo()
        GlobalPlayerState.setCurrentVideo(null)
    }

    val mediaNavigator =
        remember(navController) {
            TvMediaNavigator(
                navController = navController,
                beforeNavigate = {
                    musicExpanded = false
                    if (GlobalPlayerState.currentVideo.value != null) closePlayer()
                },
                playVideo = { playById(it) },
            )
        }

    LaunchedEffect(deeplinkVideoId) {
        val videoId = deeplinkVideoId ?: return@LaunchedEffect
        playById(videoId, isShort)
        onDeeplinkConsumed()
    }

    val musicMenus = rememberMusicMenus()

    ChorplayTvTheme {
        CompositionLocalProvider(
            LocalMediaNavigator provides mediaNavigator,
            LocalFlowSheetPresenter provides TvSheetPresenter,
            LocalMusicMenus provides musicMenus,
        ) {
            ProvideMusicPlaybackState(surfacesVisible = !musicExpanded && activeVideo == null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    val video = activeVideo
                    val visibleMusicTrack = activeMusicTrack.takeIf { musicSessionActive }
                    if (video == null && musicExpanded && visibleMusicTrack != null) {
                        TvMusicNowPlaying(
                            track = visibleMusicTrack,
                            onCollapse = { musicExpanded = false },
                        )
                    } else if (video == null) {
                        val tvNavigation = navigation ?: return@Surface
                        TvShell(
                            navigation = tvNavigation,
                            navController = navController,
                            homeViewModel = homeViewModel,
                            musicViewModel = musicViewModel,
                            musicPlayerViewModel = musicPlayerViewModel,
                            subscriptionsViewModel = subscriptionsViewModel,
                            searchViewModel = searchViewModel,
                            onPlayVideo = ::play,
                            onPlayPlaylist = ::playPlaylist,
                            onPlayVideoId = { playById(it) },
                            activeMusicTrack = visibleMusicTrack,
                            onExpandMusic = { musicExpanded = true },
                            startSettings = startSettings,
                            onDismissMusic = {
                                musicExpanded = false
                                musicSessionActive = false
                                EnhancedMusicPlayerManager.clearCurrentTrack()
                            },
                        )
                    } else {
                        TvPlayerScreen(
                            video = video,
                            viewModel = playerViewModel,
                            onClose = ::closePlayer,
                            onOpenChannel = mediaNavigator::openChannel,
                        )
                    }
                }
            }
            MusicMenuSheets(musicMenus)
        }
    }
}
