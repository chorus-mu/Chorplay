package io.github.pushkar.chorplay.ui

internal enum class ActiveMiniPlayer { None, Video, VideoBar }

internal fun resolveActiveMiniPlayer(
    hasVideo: Boolean,
    videoVisible: Boolean,
    videoInBackground: Boolean,
    onShortsPlayer: Boolean,
): ActiveMiniPlayer =
    when {
        hasVideo && onShortsPlayer -> ActiveMiniPlayer.None
        hasVideo && videoInBackground -> ActiveMiniPlayer.VideoBar
        hasVideo -> if (videoVisible) ActiveMiniPlayer.Video else ActiveMiniPlayer.None
        else -> ActiveMiniPlayer.None
    }
