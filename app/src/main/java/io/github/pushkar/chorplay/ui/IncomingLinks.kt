package io.github.pushkar.chorplay.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import io.github.pushkar.chorplay.utils.parseYouTubeLink

internal class IncomingLinks {
    private val _videoId = mutableStateOf<String?>(null)
    val videoId: State<String?> = _videoId

    private val _isShort = mutableStateOf(false)
    val isShort: State<Boolean> = _isShort

    private val _isShared = mutableStateOf(false)
    val isShared: State<Boolean> = _isShared


    private val _route = mutableStateOf<String?>(null)
    val route: State<String?> = _route

    private val _downloadVideoId = mutableStateOf<String?>(null)
    val downloadVideoId: State<String?> = _downloadVideoId

    fun openVideo(
        videoId: String,
        isShort: Boolean = false,
        isShared: Boolean = false,
    ) {
        _isShort.value = isShort
        _isShared.value = isShared
        _videoId.value = videoId
    }

    fun openRoute(route: String?) {
        _route.value = route
    }


    fun consumeVideo() {
        _videoId.value = null
        _isShort.value = false
        _isShared.value = false
    }

    fun consumeRoute() {
        _route.value = null
    }

    fun consumeDownload() {
        _downloadVideoId.value = null
    }

    fun openDownload(text: String): Boolean {
        _downloadVideoId.value =
            when (val destination = parseYouTubeLink(text)?.let(::linkDestination)) {
                is LinkDestination.Video -> destination.videoId
                is LinkDestination.Short -> destination.videoId
                is LinkDestination.Page, null -> return false
            }
        return true
    }

    fun openLink(
        text: String,
        isShared: Boolean,
        isRestoring: Boolean,
    ): Boolean {
        when (val destination = parseYouTubeLink(text)?.let(::linkDestination)) {
            is LinkDestination.Video -> if (!(isShared && isRestoring)) openVideo(
                destination.videoId,
                isShared = isShared
            )

            is LinkDestination.Short -> openVideo(destination.videoId, isShort = true)
            is LinkDestination.Page -> if (!isRestoring) openRoute(destination.route)
            null -> return false
        }
        return true
    }
}
