package io.github.pushkar.chorplay.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pushkar.chorplay.data.local.LikedVideosRepository
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.local.PlaylistRepository
import io.github.pushkar.chorplay.data.local.ViewHistory
import io.github.pushkar.chorplay.data.model.toVideo
import io.github.pushkar.chorplay.data.notes.NotesRepository
import io.github.pushkar.chorplay.data.playlist.sortedFor
import io.github.pushkar.chorplay.data.shorts.ShortsContentFilter
import io.github.pushkar.chorplay.data.stats.RecapPeriod
import io.github.pushkar.chorplay.data.stats.RecapReadiness
import io.github.pushkar.chorplay.data.video.VideoDownloadManager
import io.github.pushkar.chorplay.ui.components.library.LIBRARY_SHELF_ITEM_LIMIT
import io.github.pushkar.chorplay.ui.components.library.LibraryMediaItem
import io.github.pushkar.chorplay.ui.components.library.LibrarySection
import io.github.pushkar.chorplay.ui.components.library.toLibraryMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class LibraryCounts(
    val history: Int,
    val videoPlaylists: Int,
    val watchLater: Int,
    val likedVideos: Int,
    val downloadedVideos: Int,
    val savedShorts: Int,
) {
    val isEmpty: Boolean
        get() =
            history == 0 &&
                    videoPlaylists == 0 &&
                    watchLater == 0 &&
                    likedVideos == 0 &&
                    downloadedVideos == 0 &&
                    savedShorts == 0
}

@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    playlistRepository: PlaylistRepository,
    likedVideosRepository: LikedVideosRepository,
    viewHistory: ViewHistory,
    videoDownloadManager: VideoDownloadManager,
    shortsContentFilter: ShortsContentFilter,
    playerPreferences: PlayerPreferences,
    private val recapReadiness: RecapReadiness,
    notesRepository: NotesRepository,
) : ViewModel() {
    private val sharing = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L)

    internal val notesCount: StateFlow<Int?> =
        combine(
            playerPreferences.notesEnabled,
            notesRepository.observeCount()
        ) { enabled, count -> count.takeIf { enabled } }
            .distinctUntilChanged()
            .stateIn(viewModelScope, sharing, null)

    private val _recapReady = MutableStateFlow<RecapPeriod?>(null)

    val recapReady: StateFlow<RecapPeriod?> = _recapReady.asStateFlow()

    init {
        viewModelScope.launch { _recapReady.value = runCatching { recapReadiness.readyPeriod() }.getOrNull() }
    }

    fun onRecapHandled() {
        val period = _recapReady.value ?: return
        _recapReady.value = null
        viewModelScope.launch { recapReadiness.markShown(period) }
    }

    private fun <T> Flow<T>.shared(): StateFlow<T?> {
        val upstream: Flow<T?> = this
        return upstream
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, sharing, null)
    }

    private val allLikes = likedVideosRepository.getAllLikedVideos().shared()
    private val playlistOrder = playerPreferences.playlistListOrder
    private val allVideoPlaylists =
        combine(
            playlistRepository.getAllPlaylistsFlow(),
            playlistOrder
        ) { playlists, order -> playlists.sortedFor(order) }
            .shared()
    private val allWatchLater = playlistRepository.getVideoOnlyWatchLaterFlow().shared()
    private val allSavedShorts = playlistRepository.getVideoOnlySavedShortsFlow().shared()

    private val allDownloads = videoDownloadManager.downloadedVideos.shared()

    internal val history =
        viewHistory
            .getRecentLibraryHistory(LIBRARY_SHELF_ITEM_LIMIT)
            .map { entries -> entries.map { it.toLibraryMediaItem() } }
            .shared()

    internal val likedVideos =
        allLikes
            .map { liked ->
                liked
                    ?.asSequence()
                    ?.take(LIBRARY_SHELF_ITEM_LIMIT)
                    ?.map { it.toVideo() }
                    ?.toList()
            }.shared()

    internal val playlists = allVideoPlaylists.map { it?.take(LIBRARY_SHELF_ITEM_LIMIT) }.shared()

    internal val watchLater = allWatchLater.map { it?.take(LIBRARY_SHELF_ITEM_LIMIT) }.shared()

    internal val savedShorts = allSavedShorts.map { it?.take(LIBRARY_SHELF_ITEM_LIMIT) }.shared()

    internal val downloads =
        allDownloads
            .map { downloaded ->
                downloaded?.map { LibraryMediaItem.DownloadedVideoItem(it) }?.take(LIBRARY_SHELF_ITEM_LIMIT)
            }.shared()

    internal val shortsEnabled =
        shortsContentFilter.enabled
            .stateIn(viewModelScope, sharing, true)

    internal val separatePlaylistKinds = MutableStateFlow(false)

    internal val hiddenSections: StateFlow<Set<LibrarySection>?> =
        playerPreferences.hiddenLibrarySections
            .map(LibrarySection::fromStored)
            .stateIn(viewModelScope, sharing, null)

    internal val shelfPreviewsEnabled =
        playerPreferences.libraryShelfPreviewsEnabled
            .distinctUntilChanged()
            .stateIn(viewModelScope, sharing, true)

    internal val counts: StateFlow<LibraryCounts?> =
        combine(
            viewHistory.getLibraryHistoryCount(),
            allVideoPlaylists,
            combine(allWatchLater, allSavedShorts) { later, shorts ->
                if (later == null || shorts == null) null else later.size to shorts.size
            },
            allLikes,
            allDownloads,
        ) { historyCount, playlists, saved, liked, downloaded ->
            if (playlists == null || saved == null || liked == null || downloaded == null) {
                null
            } else {
                LibraryCounts(
                    history = historyCount,
                    videoPlaylists = playlists.size,
                    watchLater = saved.first,
                    likedVideos = liked.size,
                    downloadedVideos = downloaded.size,
                    savedShorts = saved.second,
                )
            }
        }.distinctUntilChanged()
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, sharing, null)

    internal val activeDownloadCount: StateFlow<Int?> =
        videoDownloadManager.activeDownloads
            .map { it.size }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, sharing, null)

    internal val isLibraryEmpty =
        combine(counts, activeDownloadCount, ::libraryIsEmpty)
            .distinctUntilChanged()
            .stateIn(viewModelScope, sharing, false)
}

internal fun libraryIsEmpty(
    counts: LibraryCounts?,
    activeDownloads: Int?,
): Boolean = counts != null && activeDownloads == 0 && counts.isEmpty

private val LibraryMediaItem.downloadedAt: Long
    get() =
        when (this) {
            is LibraryMediaItem.DownloadedVideoItem -> download.downloadedAt
            else -> 0L
        }
