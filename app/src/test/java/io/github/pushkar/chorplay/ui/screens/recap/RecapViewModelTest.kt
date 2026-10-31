package io.github.pushkar.chorplay.ui.screens.recap

import com.google.common.truth.Truth.assertThat
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.local.SearchHistoryRepository
import io.github.pushkar.chorplay.data.local.dao.WatchHistoryDao
import io.github.pushkar.chorplay.data.stats.RecapImageResolver
import io.github.pushkar.chorplay.data.stats.RecapPeriod
import io.github.pushkar.chorplay.data.stats.VideoMonthRecord
import io.github.pushkar.chorplay.data.stats.VideoStatsRecorder
import io.github.pushkar.chorplay.data.stats.VideoStatsSnapshot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class RecapViewModelTest {
    private val august = YearMonth.of(2026, 8)
    private val september = YearMonth.of(2026, 9)
    private val gate = CompletableDeferred<Unit>()
    private val videoStats: VideoStatsRecorder = mockk()
    private val watchHistoryDao: WatchHistoryDao = mockk(relaxed = true)
    private val searchHistory: SearchHistoryRepository = mockk()
    private val images: RecapImageResolver = mockk()
    private val preferences: PlayerPreferences = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val month = VideoMonthRecord(views = 3, watchedMs = 60_000L)
        coEvery { videoStats.snapshot() } coAnswers {
            gate.await()
            VideoStatsSnapshot(months = mapOf("2026-08" to month, "2026-09" to month))
        }
        every { searchHistory.isAutoDeleteHistoryEnabledFlow() } returns flowOf(false)
        coEvery { images.localImages() } returns emptyMap()
        coEvery { images.fetchMissing(any()) } returns emptyMap()
        every { preferences.recapSource } returns flowOf(null)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = RecapViewModel(videoStats, watchHistoryDao, searchHistory, images, preferences)

    @Test
    fun `initial state is loading until ledgers finish`() =
        runBlocking {
            val vm = viewModel()
            assertThat(vm.state.value.loading).isTrue()

            gate.complete(Unit)
            val state = withTimeout(1_000) { vm.state.first { !it.loading } }

            assertThat(state.months).containsExactly(september, august).inOrder()
            assertThat(state.period).isEqualTo(RecapPeriod.Month(september))
            assertThat(state.summary).isNotNull()
        }

    @Test
    fun `switching period updates summary immediately`() =
        runBlocking {
            gate.complete(Unit)
            val vm = viewModel()
            withTimeout(1_000) { vm.state.first { !it.loading } }

            vm.selectPeriod(RecapPeriod.Month(august))
            assertThat(vm.state.value.period).isEqualTo(RecapPeriod.Month(august))
            assertThat(vm.state.value.summary?.period).isEqualTo(RecapPeriod.Month(august))
        }

    @Test
    fun `switching source writes preference and updates state`() =
        runBlocking {
            gate.complete(Unit)
            val vm = viewModel()
            withTimeout(1_000) { vm.state.first { !it.loading } }

            vm.selectSource(RecapSource.VIDEO)
            assertThat(vm.state.value.source).isEqualTo(RecapSource.VIDEO)
        }
}
