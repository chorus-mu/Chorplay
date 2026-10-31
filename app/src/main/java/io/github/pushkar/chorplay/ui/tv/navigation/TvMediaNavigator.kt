package io.github.pushkar.chorplay.ui.tv.navigation

import androidx.navigation.NavHostController
import io.github.pushkar.chorplay.ui.components.layout.navigation.MediaNavigator
import io.github.pushkar.chorplay.utils.YouTubeLink
import java.net.URLDecoder

internal class TvMediaNavigator(
    private val navController: NavHostController,
    private val beforeNavigate: () -> Unit,
    private val playVideo: (videoId: String) -> Unit,
) : MediaNavigator {
    override fun openChannel(channelId: String) =
        open(channelId, TvRoutes.CHANNEL, TvRoutes.CHANNEL_ARG, TvRoutes::channel)

    override fun openArtist(artistId: String) = openChannel(artistId)

    override fun openAlbum(albumId: String) =
        open(albumId, TvRoutes.PLAYLIST, TvRoutes.PLAYLIST_ARG, TvRoutes::playlist)

    override fun openMusicPlaylist(playlistId: String) =
        open(playlistId, TvRoutes.PLAYLIST, TvRoutes.PLAYLIST_ARG, TvRoutes::playlist)

    override fun openEqualizer() = Unit

    override fun openLink(link: YouTubeLink): Boolean {
        when (link) {
            is YouTubeLink.Video -> {
                playVideo(link.id)
            }

            is YouTubeLink.Short -> {
                playVideo(link.id)
            }

            is YouTubeLink.Playlist -> {
                open(link.id, TvRoutes.PLAYLIST, TvRoutes.PLAYLIST_ARG, TvRoutes::playlist)
            }

            is YouTubeLink.Album -> {
                open(link.browseId, TvRoutes.PLAYLIST, TvRoutes.PLAYLIST_ARG, TvRoutes::playlist)
            }

            is YouTubeLink.Channel -> {
                openChannel(link.id)
            }

            is YouTubeLink.ChannelHandle -> {
                openChannel("https://www.youtube.com/${link.handle}")
            }

            is YouTubeLink.LegacyChannel -> {
                openChannel(link.url)
            }

            is YouTubeLink.Search -> {
                return false
            }
        }
        return true
    }

    private fun open(
        id: String,
        pattern: String,
        argName: String,
        route: (String) -> String,
    ) {
        val key = id.trim().takeIf(String::isNotEmpty) ?: return
        val current = navController.currentBackStackEntry ?: return
        val openId = current.arguments?.getString(argName)?.let { URLDecoder.decode(it, "UTF-8") }
        beforeNavigate()
        if (current.destination.route == pattern && openId == key) return
        navController.navigate(route(key))
    }
}
