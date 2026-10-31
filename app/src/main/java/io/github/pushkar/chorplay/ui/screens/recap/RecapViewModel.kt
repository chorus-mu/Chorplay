package io.github.pushkar.chorplay.ui.screens.recap

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.local.SearchHistoryRepository
import io.github.pushkar.chorplay.data.local.dao.ChannelVideoCount
import io.github.pushkar.chorplay.data.local.dao.WatchHistoryDao
import io.github.pushkar.chorplay.data.stats.RecapAggregates
import io.github.pushkar.chorplay.data.stats.RecapImageResolver
import io.github.pushkar.chorplay.data.stats.RecapPeriod
import io.github.pushkar.chorplay.data.stats.RecapSummary
import io.github.pushkar.chorplay.data.stats.TrackAlbum
import io.github.pushkar.chorplay.data.stats.VideoStatsRecorder
import io.github.pushkar.chorplay.data.stats.VideoStatsSnapshot
import io.github.pushkar.chorplay.data.stats.withImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class RecapSource { ALL, VIDEO }

@Immutable
data class RecapUiState(
    val loading: Boolean = true,
    val months: List<YearMonth> = emptyList(),
    val period: RecapPeriod = RecapPeriod.Month(YearMonth.now()),
    val source: RecapSource = RecapSource.VIDEO,
    val summary: RecapSummary? = null,
    /** Distinct videos per channel from watch history, shown only for all time. */
    val history: List<ChannelVideoCount> = emptyList(),
) {
    val years: List<Int> get() = months.map { it.year }.distinct()
    val isEmpty: Boolean get() = !loading && months.isEmpty()
}

@HiltViewModel
class RecapViewModel
@Inject
constructor(
    private val videoStats: VideoStatsRecorder,
    private val watchHistoryDao: WatchHistoryDao,
    private val searchHistory: SearchHistoryRepository,
    private val images: RecapImageResolver,
    private val preferences: PlayerPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(RecapUiState())
    val state: StateFlow<RecapUiState> = _state.asStateFlow()

    private var video = VideoStatsSnapshot()
    private var queriesSince: YearMonth? = null
    private var portraits: Map<String, String> = emptyMap()
    private var trackAlbums: Map<String, TrackAlbum> = emptyMap()
    private var historyLoaded = false
    private var loaded = false
    private var requested: Pair<RecapPeriod, RecapSource>? = null
    private var foldJob: Job? = null

    init {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                video = runCatching { videoStats.snapshot() }.getOrDefault(VideoStatsSnapshot())
                queriesSince = searchQueryCutoff()
                portraits = runCatching { images.localImages() }.getOrDefault(emptyMap())
            }
            val rememberedSource = RecapSource.VIDEO
            val months = RecapAggregates.availableMonths(video)
            val newest = months.firstOrNull()?.let { RecapPeriod.Month(it) } ?: RecapPeriod.Month(YearMonth.now())
            _state.update { it.copy(months = months) }
            loaded = true
            val (period, source) = requested ?: (newest to rememberedSource)
            show(period, source)
        }
    }

    fun selectPeriod(period: RecapPeriod) = show(period, _state.value.source)

    fun selectSource(source: RecapSource) {
        viewModelScope.launch { preferences.setRecapSource(source.name) }
        show(_state.value.period, source)
    }

    fun openAt(
        period: RecapPeriod,
        source: RecapSource = _state.value.source,
    ) {
        requested = period to source
        if (loaded) show(period, source)
    }

    private fun show(
        period: RecapPeriod,
        source: RecapSource,
    ) {
        foldJob?.cancel()
        foldJob =
            viewModelScope.launch {
                val summary =
                    withContext(Dispatchers.Default) {
                        RecapAggregates
                            .summarize(period, video, queriesSince, trackAlbums)
                            .withImages(portraits)
                    }
                val history =
                    if (period == RecapPeriod.AllTime) channelHistory() else emptyList()
                _state.update {
                    it.copy(
                        loading = false,
                        period = period,
                        source = source,
                        summary = summary,
                        history = history
                    )
                }
                fillMissingPortraits(summary)
            }
    }

    private suspend fun channelHistory(): List<ChannelVideoCount> {
        if (historyLoaded) return _state.value.history
        historyLoaded = true
        return withContext(Dispatchers.IO) {
            runCatching { watchHistoryDao.getChannelVideoCounts(HISTORY_CHANNELS) }.getOrDefault(emptyList())
        }
    }

    private suspend fun fillMissingPortraits(summary: RecapSummary) {
        val found = runCatching { images.fetchMissing(summary) }.getOrDefault(emptyMap())
        if (found.isEmpty()) return
        portraits = portraits + found
        _state.update { state -> state.summary?.let { state.copy(summary = it.withImages(found)) } ?: state }
    }

    private suspend fun searchQueryCutoff(): YearMonth? {
        if (!searchHistory.isAutoDeleteHistoryEnabledFlow().first()) return null
        val days = searchHistory.getHistoryRetentionDaysFlow().first()
        return YearMonth.from(LocalDate.now().minusDays(days.toLong()))
    }

    private companion object {
        const val HISTORY_CHANNELS = 10
    }
}
