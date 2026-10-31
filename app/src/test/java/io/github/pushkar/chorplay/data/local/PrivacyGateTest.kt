package io.github.pushkar.chorplay.data.local

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrivacyGateTest {
    private fun gate(
        deepChor: Boolean,
        historyPaused: Boolean = false,
    ) = PrivacyGate(
        deepChorActive = { deepChor },
        watchHistoryPaused = { historyPaused },
    )

    @Test
    fun `the gate reports both settings as they are`() =
        runTest {
            val gate = gate(deepChor = true, historyPaused = true)

            assertThat(gate.isDeepChorplayActive()).isTrue()
            assertThat(gate.isWatchHistoryPaused()).isTrue()
        }

    @Test
    fun `the gate reports default settings when off`() =
        runTest {
            val gate = gate(deepChor = false, historyPaused = false)

            assertThat(gate.isDeepChorplayActive()).isFalse()
            assertThat(gate.isWatchHistoryPaused()).isFalse()
        }
}
