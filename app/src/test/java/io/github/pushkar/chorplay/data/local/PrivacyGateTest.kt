package io.github.pushkar.chorplay.data.local

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrivacyGateTest {
    private fun gate(
        deepFlow: Boolean,
        historyPaused: Boolean = false,
        scrobbleDuringDeepChorplay: Boolean = false,
    ) = PrivacyGate(
        deepFlowActive = { deepFlow },
        watchHistoryPaused = { historyPaused },
        scrobbleDuringDeepChorplay = { scrobbleDuringDeepChorplay },
    )

    @Test
    fun `scrobbling runs while Deep Chorplay is off`() =
        runTest {
            assertThat(gate(deepFlow = false).allowsScrobbling()).isTrue()
        }

    @Test
    fun `Deep Chorplay stops scrobbling by default`() =
        runTest {
            assertThat(gate(deepFlow = true).allowsScrobbling()).isFalse()
        }

    @Test
    fun `Deep Chorplay scrobbles when the user chose to`() =
        runTest {
            assertThat(gate(deepFlow = true, scrobbleDuringDeepChorplay = true).allowsScrobbling()).isTrue()
        }

    @Test
    fun `the gate reports both settings as they are`() =
        runTest {
            val gate = gate(deepFlow = true, historyPaused = true)

            assertThat(gate.isDeepChorplayActive()).isTrue()
            assertThat(gate.isWatchHistoryPaused()).isTrue()
        }
}
