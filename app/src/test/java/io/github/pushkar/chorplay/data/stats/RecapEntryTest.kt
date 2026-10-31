package io.github.pushkar.chorplay.data.stats

import com.google.common.truth.Truth.assertThat
import io.github.pushkar.chorplay.data.local.PrivacyGate
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RecapEntryTest {
    private fun gate(
        deepChor: Boolean,
        historyPaused: Boolean,
    ) = PrivacyGate(
        deepChorActive = { deepChor },
        watchHistoryPaused = { historyPaused },
    )

    @Test
    fun `everything is recorded when nothing is paused`() =
        runTest {
            RecapEntry.entries.forEach { assertThat(it.allowed(gate(deepChor = false, historyPaused = false))).isTrue() }
        }

    @Test
    fun `Deep Chorplay keeps searches, actions and views out`() =
        runTest {
            val gate = gate(deepChor = true, historyPaused = false)

            assertThat(RecapEntry.ACTIVITY.allowed(gate)).isFalse()
            assertThat(RecapEntry.VIEW.allowed(gate)).isFalse()
        }

    @Test
    fun `paused watch history keeps views out but still counts searches`() =
        runTest {
            val gate = gate(deepChor = false, historyPaused = true)

            assertThat(RecapEntry.VIEW.allowed(gate)).isFalse()
            assertThat(RecapEntry.ACTIVITY.allowed(gate)).isTrue()
        }

    @Test
    fun `forgetting stored data always goes through`() =
        runTest {
            assertThat(RecapEntry.FORGET.allowed(gate(deepChor = true, historyPaused = true))).isTrue()
        }
}
