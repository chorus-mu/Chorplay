package io.github.pushkar.chorplay.player.audio

internal fun shouldHandleAudioFocus(playDuringCalls: Boolean): Boolean = !playDuringCalls
