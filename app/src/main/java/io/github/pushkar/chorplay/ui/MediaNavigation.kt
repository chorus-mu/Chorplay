package io.github.pushkar.chorplay.ui

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import io.github.pushkar.chorplay.data.shorts.queue.ShortsQueueSource
import io.github.pushkar.chorplay.ui.components.layout.navigation.MediaNavigator
import io.github.pushkar.chorplay.ui.components.settings.SettingsTarget
import io.github.pushkar.chorplay.utils.YouTubeLink

internal const val EQUALIZER_ROUTE = "equalizer"

internal enum class PageOpen { PUSH, STAY, RETURN }

internal fun <T> pageOpen(
    existing: T?,
    current: T?,
): PageOpen =
    when {
        existing == null -> PageOpen.PUSH
        existing == current -> PageOpen.STAY
        else -> PageOpen.RETURN
    }

internal fun NavController.openPage(route: String) {
    val existing = runCatching { getBackStackEntry(route) }.getOrNull()
    when (pageOpen(existing, currentBackStackEntry)) {
        PageOpen.PUSH -> navigate(route)
        PageOpen.STAY -> Unit
        PageOpen.RETURN -> popBackStack(route, inclusive = false)
    }
}

internal class ChorMediaNavigator(
    private val navController: NavHostController,
    private val beforeNavigate: () -> Unit,
    private val shortsExitRoute: () -> String,
) : MediaNavigator {
    override fun openChannel(channelId: String) {
        if (channelId.isBlank()) return
        beforeNavigate()
        navController.navigateToYoutubeChannel(channelId)
    }

    override fun openArtist(artistId: String) {
        if (artistId.isNotBlank()) openChannel(artistId)
    }

    override fun openAlbum(albumId: String) {
        if (albumId.isNotBlank()) open("playlist/${Uri.encode(albumId)}")
    }

    override fun openMusicPlaylist(playlistId: String) {
        if (playlistId.isNotBlank()) open("playlist/${Uri.encode(playlistId)}")
    }

    override fun openSettings(target: SettingsTarget) {
        beforeNavigate()
        navController.navigate("settings?target=${target.encode()}")
    }

    override fun openEqualizer() {
        beforeNavigate()
        if (navController.currentBackStackEntry?.destination?.route == EQUALIZER_ROUTE) return
        navController.navigate(EQUALIZER_ROUTE)
    }

    override fun openLink(link: YouTubeLink): Boolean {
        val destination = linkDestination(link) ?: return false
        when {
            destination is LinkDestination.Video -> {
                openPlayer { navController.navigateToPlayer(destination.videoId) }
            }

            destination is LinkDestination.Page && destination.playsVideo -> {
                openPlayer { navController.navigate(destination.route) }
            }

            destination is LinkDestination.Short -> {
                beforeNavigate()
                navController.openShorts(ShortsQueueSource.SeededFeed(destination.videoId))
            }

            destination is LinkDestination.Page -> {
                beforeNavigate()
                navController.openPage(destination.route)
            }
        }
        return true
    }

    private fun openPlayer(navigate: () -> Unit) {
        while (isShortsOnTop() && navController.previousBackStackEntry != null) navController.popBackStack()
        if (isShortsOnTop()) {
            navController.navigate(shortsExitRoute()) {
                popUpTo(SHORTS_ROUTE_PATTERN) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        navigate()
    }

    private fun isShortsOnTop() = navController.currentBackStackEntry?.destination?.route == SHORTS_ROUTE_PATTERN

    private fun open(route: String?) {
        if (route == null) return
        beforeNavigate()
        navController.openPage(route)
    }
}
