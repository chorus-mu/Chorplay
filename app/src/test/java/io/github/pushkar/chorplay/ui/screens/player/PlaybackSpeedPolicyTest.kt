package io.github.pushkar.chorplay.ui.screens.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlaybackSpeedPolicyTest {
    @Test
    fun `with every option off the speed is left alone`() {
        assertThat(PlaybackSpeedPolicy.decide(null, false, 1.5f)).isEqualTo(SpeedDecision.Keep)
    }

    @Test
    fun `remembered speed applies`() {
        assertThat(PlaybackSpeedPolicy.decide(null, true, 1.5f)).isEqualTo(SpeedDecision.Remembered(1.5f))
    }

    @Test
    fun `a channel speed comes first`() {
        assertThat(PlaybackSpeedPolicy.decide(1.25f, true, 2f)).isEqualTo(SpeedDecision.Override(1.25f))
    }
}
