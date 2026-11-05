package io.github.pushkar.chorplay.ui.components.videoplayer.ambient

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared observable holder for the latest smoothed video ambient frame.
 *
 * Allows downstream player surfaces (such as the under-player ambient glow in the expanded
 * player sheet) to observe the active ambient frame without invasive prop-drilling across
 * the stage layers.
 */
object AmbientFrameHolder {
    private val _currentFrame = MutableStateFlow<ImageBitmap?>(null)
    val currentFrame: StateFlow<ImageBitmap?> = _currentFrame.asStateFlow()

    fun updateFrame(frame: ImageBitmap?) {
        if (_currentFrame.value !== frame) {
            _currentFrame.value = frame
        }
    }

    fun clear() {
        _currentFrame.value = null
    }
}

