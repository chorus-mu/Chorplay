package io.github.pushkar.chorplay.ui.tv.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.video.DownloadedVideo
import io.github.pushkar.chorplay.ui.components.layout.navigation.ChorplayTab
import io.github.pushkar.chorplay.ui.components.shared.MediaKind
import io.github.pushkar.chorplay.ui.screens.channel.ChannelScreen
import io.github.pushkar.chorplay.ui.screens.history.HistoryScreen
import io.github.pushkar.chorplay.ui.screens.home.HomeViewModel
import io.github.pushkar.chorplay.ui.screens.library.DownloadsScreen
import io.github.pushkar.chorplay.ui.screens.playlists.PlaylistDetailScreen
import io.github.pushkar.chorplay.ui.screens.playlists.PlaylistsScreen
import io.github.pushkar.chorplay.ui.screens.search.SearchViewModel
import io.github.pushkar.chorplay.ui.screens.subscriptions.SubscriptionsViewModel
import io.github.pushkar.chorplay.ui.tv.components.TvScreenFrame
import io.github.pushkar.chorplay.ui.tv.screens.TvHomeScreen
import io.github.pushkar.chorplay.ui.tv.screens.TvLibraryScreen
import io.github.pushkar.chorplay.ui.tv.screens.TvSearchScreen
import io.github.pushkar.chorplay.ui.tv.screens.TvSubscriptionsScreen

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TvNavHost(
    navController: NavHostController,
    startRoute: String,
    homeViewModel: HomeViewModel,
    subscriptionsViewModel: SubscriptionsViewModel,
    searchViewModel: SearchViewModel,
    onPlayVideo: (Video) -> Unit,
    onPlayPlaylist: (videos: List<Video>, startIndex: Int, title: String, shuffle: Boolean) -> Unit,
    onPlayVideoId: (String) -> Unit,
    onSelectRoot: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openChannel: (String) -> Unit = { channelRef ->
        navController.navigate(TvRoutes.channel(channelRef))
    }
    val downloadsTitle = stringResource(R.string.library_downloads_label)
    val playDownloadedVideos: (List<DownloadedVideo>, Int) -> Unit = { videos, index ->
        onPlayPlaylist(videos.map { it.video }, index, downloadsTitle, false)
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        modifier = modifier,
    ) {
        composable(ChorplayTab.Home.route) {
            TvHomeScreen(
                viewModel = homeViewModel,
                onVideoClick = onPlayVideo,
                onPlayPlaylist = { videos, title -> onPlayPlaylist(videos, 0, title, false) },
                onOpenLibrary = { onSelectRoot(ChorplayTab.Library.route) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(ChorplayTab.Subscriptions.route) {
            TvSubscriptionsScreen(
                viewModel = subscriptionsViewModel,
                onVideoClick = onPlayVideo,
                onChannelClick = { channel -> openChannel(channel.url.ifBlank { channel.id }) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(ChorplayTab.Search.route) {
            TvSearchScreen(
                viewModel = searchViewModel,
                onVideoClick = onPlayVideo,
                onChannelClick = { channel -> openChannel(channel.url.ifBlank { channel.id }) },
                onPlaylistClick = { playlist -> navController.navigate(TvRoutes.playlist(playlist.id)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(ChorplayTab.Library.route) {
            TvLibraryScreen(
                onVideoClick = onPlayVideo,
                onOpenHistory = { navController.navigate(TvRoutes.HISTORY) },
                onOpenPlaylists = { kind -> navController.navigate(TvRoutes.playlists(kind)) },
                onOpenPlaylist = { navController.navigate(TvRoutes.playlist(it)) },
                onOpenDownloads = { navController.navigate(TvRoutes.DOWNLOADS) },
                onDownloadedVideoClick = playDownloadedVideos,
                modifier = Modifier.fillMaxSize(),
            )
        }
        composable(TvRoutes.DOWNLOADS) {
            TvScreenFrame {
                DownloadsScreen(
                    onBackClick = { navController.popBackStack() },
                    onVideoClick = playDownloadedVideos,
                    onOpenCollection = { summary ->
                        navController.navigate(TvRoutes.playlist(summary.collection.id))
                    },
                    onHomeClick = { onSelectRoot(startRoute) },
                )
            }
        }
        composable(TvRoutes.HISTORY) {
            TvScreenFrame {
                HistoryScreen(
                    onVideoClick = onPlayVideo,
                    onBackClick = { navController.popBackStack() },
                    onShortsQueue = {},
                )
            }
        }
        composable(
            route = TvRoutes.PLAYLISTS,
            arguments =
                listOf(
                    navArgument(TvRoutes.PLAYLISTS_KIND_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { entry ->
            val kind =
                entry.arguments?.getString(TvRoutes.PLAYLISTS_KIND_ARG)?.let { name ->
                    MediaKind.entries.firstOrNull {
                        it.name == name
                    }
                }
            TvScreenFrame {
                PlaylistsScreen(
                    fixedKind = kind,
                    onBackClick = { navController.popBackStack() },
                    onVideoPlaylistClick = { navController.navigate(TvRoutes.playlist(it.id)) },
                )
            }
        }
        composable(
            route = TvRoutes.CHANNEL,
            arguments =
                listOf(
                    navArgument(TvRoutes.CHANNEL_ARG) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
        ) { entry ->
            val channelRef =
                entry.arguments
                    ?.getString(TvRoutes.CHANNEL_ARG)
                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                    .orEmpty()
            TvScreenFrame {
                ChannelScreen(
                    channelUrl = channelRef,
                    onVideoClick = onPlayVideo,
                    onChannelClick = openChannel,
                    onShortClick = { videoId, _ -> onPlayVideoId(videoId) },
                    onPlaylistClick = { navController.navigate(TvRoutes.playlist(it)) },
                    onBackClick = { navController.popBackStack() },
                    showShorts = false,
                )
            }
        }
        composable(
            route = TvRoutes.PLAYLIST,
            arguments = listOf(navArgument(TvRoutes.PLAYLIST_ARG) { type = NavType.StringType }),
        ) {
            TvScreenFrame {
                PlaylistDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlayPlaylist = { videos, index, shuffle, title ->
                        onPlayPlaylist(videos, index, title, shuffle)
                    },
                )
            }
        }
        tvSettingsRoutes(navController = navController)
    }
}
