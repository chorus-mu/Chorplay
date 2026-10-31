package io.github.pushkar.chorplay.ui

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.withFrameNanos
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.pushkar.chorplay.ui.screens.player.VideoPlayerViewModel
import io.github.pushkar.chorplay.ui.screens.widgets.WidgetPlaybackViewModel

private const val WIDGET_PLAYLIST_ROUTE = "widgetPlaylist/{playlistId}?shuffle={shuffle}&start={start}"

internal fun widgetPlaylistRoute(
    playlistId: String,
    shuffle: Boolean,
    startVideoId: String? = null,
): String =
    "widgetPlaylist/${Uri.encode(playlistId)}?shuffle=$shuffle" +
        startVideoId?.let { "&start=${Uri.encode(it)}" }.orEmpty()

/** Routes a home-screen widget opens to start playback; each plays, then leaves at once. */
internal fun NavGraphBuilder.widgetPlaybackRoutes(
    navController: NavHostController,
    currentRoute: MutableState<String>,
    defaultStartRoute: String,
    playerViewModel: VideoPlayerViewModel,
) {
    composable(
        route = WIDGET_PLAYLIST_ROUTE,
        arguments =
            listOf(
                navArgument("playlistId") { type = NavType.StringType },
                navArgument("shuffle") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("start") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { entry ->
        val playlistId = entry.arguments?.getString("playlistId").orEmpty()
        val shuffle = entry.arguments?.getBoolean("shuffle") ?: false
        val startVideoId = entry.arguments?.getString("start")
        val playlists: WidgetPlaybackViewModel = hiltViewModel()
        LaunchedEffect(playlistId, shuffle, startVideoId) {
            val (playlist, videos) = playlists.playlist(playlistId)
            if (playlist != null && videos.isNotEmpty()) {
                val start = videos.indexOfFirst { it.id == startVideoId }.coerceAtLeast(0)
                playerViewModel.playPlaylist(videos, start, playlist.name, shuffle)
            }
            withFrameNanos { }
            navController.popTransientRouteOrNavigateStart(defaultStartRoute)
        }
    }
}

internal fun NavHostController.popTransientRouteOrNavigateStart(defaultStartRoute: String) {
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        navigate(defaultStartRoute) {
            launchSingleTop = true
        }
    }
}
