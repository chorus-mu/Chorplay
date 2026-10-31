package io.github.pushkar.chorplay.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pushkar.chorplay.data.local.ViewHistory
import io.github.pushkar.chorplay.data.localmedia.LocalLibrary
import io.github.pushkar.chorplay.data.localmedia.LocalMediaItem
import io.github.pushkar.chorplay.data.localmedia.LocalMediaPreferences
import io.github.pushkar.chorplay.data.localmedia.LocalMediaRepository
import io.github.pushkar.chorplay.data.localmedia.LocalMediaSettings
import io.github.pushkar.chorplay.data.localmedia.hiddenReason
import io.github.pushkar.chorplay.ui.components.shared.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val SHARING_TIMEOUT_MS = 5_000L

private fun stateClockMs(): Long = Instant.now().truncatedTo(ChronoUnit.MINUTES).toEpochMilli()

enum class LocalView { ALL, FOLDERS }

data class LocalMediaSelection(
    val kind: MediaKind = MediaKind.Videos,
    val view: LocalView = LocalView.ALL,
    val openFolderId: String? = null,
    val filters: LocalFilters = LocalFilters(),
)

data class LocalMediaUiState(
    val isLoading: Boolean = true,
    val failed: Boolean = false,
    val selection: LocalMediaSelection = LocalMediaSelection(),
    val settings: LocalMediaSettings = LocalMediaSettings(),
    val items: List<LocalMediaItem> = emptyList(),
    val totalCount: Int = 0,
    val folders: List<LocalFolder> = emptyList(),
    val openFolder: LocalFolder? = null,
    val listsFolders: Boolean = false,
    val continueWatching: List<LocalMediaItem> = emptyList(),
    val hiddenCount: Int = 0,
    val playback: LocalPlayback = LocalPlayback(),
    val nowMs: Long = 0L,
)

@HiltViewModel
class LocalMediaViewModel
@Inject
constructor(
    private val repository: LocalMediaRepository,
    private val preferences: LocalMediaPreferences,
    viewHistory: ViewHistory,
) : ViewModel() {
    private val selection = MutableStateFlow(LocalMediaSelection())

    private val playback =
        viewHistory
            .getLocalHistoryFlow()
            .map { entries ->
                LocalPlayback(
                    fraction = entries.filter { it.duration > 0 }
                        .associate { it.videoId to (it.position.toFloat() / it.duration) },
                    lastPlayedMs = entries.associate { it.videoId to it.timestamp },
                )
            }.distinctUntilChanged()

    val uiState: StateFlow<LocalMediaUiState> =
        combine(repository.library, preferences.settings, playback, selection) { library, settings, played, chosen ->
            buildState(library, settings, played, chosen, stateClockMs())
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SHARING_TIMEOUT_MS), LocalMediaUiState())

    val isRefreshing: StateFlow<Boolean> = repository.refreshing

    fun refresh() = repository.refresh()

    fun selectKind(kind: MediaKind) = selection.update { it.copy(kind = kind, openFolderId = null) }

    fun selectView(view: LocalView) = selection.update { it.copy(view = view, openFolderId = null) }

    fun openFolder(folderId: String?) = selection.update { it.copy(openFolderId = folderId) }

    fun updateFilters(change: (LocalFilters) -> LocalFilters) =
        selection.update { it.copy(filters = change(it.filters)) }

    fun hideFolder(folderId: String) {
        viewModelScope.launch { preferences.setFolderHidden(folderId, hidden = true) }
        selection.update { if (it.openFolderId == folderId) it.copy(openFolderId = null) else it }
    }

    fun setVideosAsGrid(grid: Boolean) {
        viewModelScope.launch { preferences.setVideosAsGrid(grid) }
    }
}

internal fun buildState(
    library: LocalLibrary,
    settings: LocalMediaSettings,
    playback: LocalPlayback,
    selection: LocalMediaSelection,
    nowMs: Long,
): LocalMediaUiState {
    val all = library.videos
    val (hidden, shown) = all.partition { it.hiddenReason(settings) != null }
    val folders = shown.folders()
    val openFolder = selection.openFolderId?.let { id -> folders.firstOrNull { it.id == id } }
    val source = openFolder?.items ?: shown
    return LocalMediaUiState(
        isLoading = false,
        failed = library.failed,
        selection = selection,
        settings = settings,
        items = source.applyLocalFilters(selection.filters, playback, nowMs),
        totalCount = shown.size,
        folders = folders,
        openFolder = openFolder,
        listsFolders = selection.view == LocalView.FOLDERS && openFolder == null && selection.filters.query.isBlank(),
        continueWatching = shown.continueWatching(playback, nowMs),
        hiddenCount = hidden.size,
        playback = playback,
        nowMs = nowMs,
    )
}
