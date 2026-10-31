package io.github.pushkar.chorplay.ui

import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.player.GlobalPlayerState
import io.github.pushkar.chorplay.ui.components.layout.navigation.DOWNLOADS_ROUTE
import io.github.pushkar.chorplay.ui.components.layout.navigation.DOWNLOADS_TAB_ROUTE
import io.github.pushkar.chorplay.ui.components.layout.navigation.MediaNavigator
import io.github.pushkar.chorplay.ui.screens.library.DownloadsScreen
import io.github.pushkar.chorplay.ui.screens.player.VideoPlayerViewModel

internal fun NavGraphBuilder.downloadsRoutes(
    navController: NavHostController,
    mediaNavigator: MediaNavigator,
    currentRoute: MutableState<String>,
    playerViewModel: VideoPlayerViewModel,
) {
    listOf(DOWNLOADS_ROUTE, DOWNLOADS_TAB_ROUTE).forEach { route ->
        composable(route) {
            currentRoute.value = route
            val downloadsTitle = stringResource(R.string.library_downloads_label)
            DownloadsScreen(
                onBackClick = if (route == DOWNLOADS_ROUTE) ({ navController.popBackStack() }) else null,
                onVideoClick = { videos, index ->
                    val videoList = videos.map { it.video }
                    playerViewModel.playPlaylist(videoList, index, downloadsTitle)
                    GlobalPlayerState.setCurrentVideo(videoList[index])
                },
                onOpenCollection = { summary ->
                    navController.navigate("playlist/${summary.collection.id}")
                },
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
            )
        }
    }
}
