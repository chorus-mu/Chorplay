package io.github.pushkar.chorplay.ui

import androidx.navigation.NavHostController

internal fun NavHostController.navigateToYoutubeChannel(channelIdOrHandle: String) {
    youtubeChannelRoute(channelIdOrHandle)?.let(::openPage)
}
