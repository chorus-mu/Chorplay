package io.github.pushkar.chorplay.ui.components.videoplayer.ambient

import androidx.compose.ui.graphics.ImageBitmap
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class AmbientFrameHolderTest {

    @Before
    @After
    fun reset() {
        AmbientFrameHolder.clear()
    }

    @Test
    fun `initial state is null`() {
        assertThat(AmbientFrameHolder.currentFrame.value).isNull()
    }

    @Test
    fun `updateFrame updates the current frame`() {
        val bitmap = ImageBitmap(10, 10)
        AmbientFrameHolder.updateFrame(bitmap)

        assertThat(AmbientFrameHolder.currentFrame.value).isSameInstanceAs(bitmap)
    }

    @Test
    fun `clear resets the frame to null`() {
        val bitmap = ImageBitmap(10, 10)
        AmbientFrameHolder.updateFrame(bitmap)
        assertThat(AmbientFrameHolder.currentFrame.value).isNotNull()

        AmbientFrameHolder.clear()
        assertThat(AmbientFrameHolder.currentFrame.value).isNull()
    }
}

