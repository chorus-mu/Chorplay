package io.github.pushkar.chorplay.ui

import android.content.Intent
import io.github.pushkar.chorplay.data.local.PlaylistRepository
import io.github.pushkar.chorplay.utils.YouTubeLink

internal sealed interface LinkDestination {
    data class Video(
        val videoId: String,
    ) : LinkDestination

    data class Short(
        val videoId: String,
    ) : LinkDestination

    data class Page(
        val route: String,
        val playsVideo: Boolean = false,
    ) : LinkDestination
}

internal fun linkTextOf(intent: Intent): String? =
    when (intent.action) {
        Intent.ACTION_VIEW -> intent.dataString
        Intent.ACTION_SEND -> intent.takeIf { it.type == "text/plain" }?.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }

internal fun linkDestination(link: YouTubeLink): LinkDestination? =
    when (link) {
        is YouTubeLink.Video -> videoDestination(link)
        is YouTubeLink.Short -> LinkDestination.Short(link.id)
        else -> pageRoute(link)?.let(LinkDestination::Page)
    }

private fun videoDestination(link: YouTubeLink.Video): LinkDestination {
    val playlistId = link.playlistId?.takeUnless { it in ACCOUNT_LISTS }
    return when {
        playlistId == null || isMix(playlistId) -> LinkDestination.Video(link.id)
        else -> LinkDestination.Page(linkedPlaylistRoute(playlistId, link.id), playsVideo = true)
    }
}

private fun pageRoute(link: YouTubeLink): String? =
    when (link) {
        is YouTubeLink.Video, is YouTubeLink.Short, is YouTubeLink.Search -> null
        is YouTubeLink.Playlist -> playlistRoute(link)
        is YouTubeLink.Album -> "playlist/${link.browseId}"
        is YouTubeLink.Channel -> youtubeChannelRoute(link.id)
        is YouTubeLink.ChannelHandle -> youtubeChannelRoute(link.handle)
        is YouTubeLink.LegacyChannel -> youtubeChannelRoute(link.url)
    }

private fun playlistRoute(link: YouTubeLink.Playlist): String? =
    when {
        link.id == "LL" -> "playlist/${PlaylistRepository.LIKED_VIDEOS_ID}"
        link.id == "WL" -> "playlist/${PlaylistRepository.WATCH_LATER_ID}"
        link.id == "LM" -> null
        isMix(link.id) -> null
        else -> "playlist/${link.id}"
    }

internal fun isMix(playlistId: String): Boolean = playlistId.startsWith("RD") && !playlistId.startsWith("RDCLAK")

private val ACCOUNT_LISTS = setOf("LL", "WL", "LM")
