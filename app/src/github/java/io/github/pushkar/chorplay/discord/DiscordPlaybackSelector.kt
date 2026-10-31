package io.github.pushkar.chorplay.discord

class DiscordPlaybackSelector {
    fun select(
        short: PlaybackSnapshot?,
        video: PlaybackSnapshot?,
        music: PlaybackSnapshot?,
    ): PlaybackSnapshot? = sequenceOf(short, video, music)
        .filterNotNull()
        .firstOrNull { snapshot -> snapshot.isPlaying && snapshot.mediaId.isNotBlank() }
}
