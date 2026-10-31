package io.github.pushkar.chorplay.ui.screens.home

import io.github.pushkar.chorplay.data.local.VideoHistoryEntry
import io.github.pushkar.chorplay.data.model.Video

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val shorts: List<Video> = emptyList(),
    val continueWatchingVideos: List<VideoHistoryEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMorePages: Boolean = true,
    /** The last load-more found nothing because its requests failed; the feed stays open for a retry. */
    val loadMoreFailed: Boolean = false,
    val error: String? = null,
    val isFlowFeed: Boolean = false,
    val lastRefreshTime: Long = 0L,
    /** Why each card is on Home; channel memory cards also say so under their title. */
    val origins: Map<String, HomeCardOrigin> = emptyMap(),
    /** The new feed held back while the cached one is on screen at launch (#1447). */
    val pendingFeed: HomePendingFeed? = null,
)
