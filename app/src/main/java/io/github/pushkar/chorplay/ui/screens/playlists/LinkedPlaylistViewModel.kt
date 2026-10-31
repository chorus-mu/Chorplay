package io.github.pushkar.chorplay.ui.screens.playlists

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.repository.RemotePlaylistPage
import io.github.pushkar.chorplay.data.repository.YouTubePlaylistRepository
import io.github.pushkar.chorplay.data.repository.walkPlaylistPages
import javax.inject.Inject

internal data class LinkedQueue<T>(
    val items: List<T>,
    val startIndex: Int,
    val title: String?,
)

@HiltViewModel
internal class LinkedPlaylistViewModel
@Inject
constructor(
    private val playlists: YouTubePlaylistRepository,
) : ViewModel() {
    suspend fun videoQueue(
        playlistId: String,
        startVideoId: String,
    ): LinkedQueue<Video>? {
        playlists.cachedComplete(playlistId)?.let { cached ->
            return linkedQueue(cached.videos, startVideoId, cached.title, Video::id)
        }
        val first = playlists.firstPage(playlistId) ?: return null
        return findLinkedVideoQueue(first, startVideoId) { token -> playlists.nextPage(playlistId, token) }
    }
}

internal fun <T> linkedQueue(
    items: List<T>,
    startId: String,
    title: String?,
    idOf: (T) -> String,
): LinkedQueue<T>? {
    val index = items.indexOfFirst { idOf(it) == startId }.takeIf { it >= 0 } ?: return null
    return LinkedQueue(items, index, title)
}

internal suspend fun findLinkedVideoQueue(
    firstPage: RemotePlaylistPage,
    videoId: String,
    fetchNext: suspend (String) -> RemotePlaylistPage?,
): LinkedQueue<Video>? {
    val accumulated = ArrayList<Video>(firstPage.videos)
    var targetIndex = accumulated.indexOfFirst { it.id == videoId }
    if (targetIndex >= 0) return LinkedQueue(accumulated, targetIndex, firstPage.title)

    walkPlaylistPages(firstPage.continuationToken, fetchNext) { page ->
        val startOffset = accumulated.size
        accumulated.addAll(page.videos)
        val found = page.videos.indexOfFirst { it.id == videoId }
        if (found >= 0) {
            targetIndex = startOffset + found
            return@walkPlaylistPages false
        }
        true
    }

    return if (targetIndex >= 0) LinkedQueue(accumulated, targetIndex, firstPage.title) else null
}
