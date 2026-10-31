package io.github.pushkar.chorplay.service

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.pushkar.chorplay.player.LifecyclePlaybackPreferences

/** [VideoPlayerService] is not a Hilt entry point, so the singletons it reads come through here. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlaybackServiceEntryPoint {
    fun lifecyclePlaybackPreferences(): LifecyclePlaybackPreferences
}
