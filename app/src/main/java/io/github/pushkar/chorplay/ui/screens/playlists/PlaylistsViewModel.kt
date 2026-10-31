package io.github.pushkar.chorplay.ui.screens.playlists

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.local.PlaylistRepository
import io.github.pushkar.chorplay.data.model.PlaylistInfo
import io.github.pushkar.chorplay.data.playlist.PlaylistImport
import io.github.pushkar.chorplay.data.playlist.PlaylistListOrder
import io.github.pushkar.chorplay.data.playlist.PlaylistTransfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel
@Inject
constructor(
    private val repository: PlaylistRepository,
    private val transfer: PlaylistTransfer,
    private val preferences: PlayerPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PlaylistsUiState(isLoading = true))
    val uiState: StateFlow<PlaylistsUiState> = _uiState.asStateFlow()

    private val sharing = SharingStarted.WhileSubscribed(5_000)

    val listOrder: StateFlow<PlaylistListOrder> =
        preferences.playlistListOrder.stateIn(viewModelScope, sharing, PlaylistListOrder.NEWEST)

    val compactLayout: StateFlow<Boolean> = preferences.playlistsCompactLayout.stateIn(viewModelScope, sharing, false)

    init {
        observePlaylists()
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            launch {
                repository.getUserCreatedVideoPlaylistsFlow().collect { playlists ->
                    _uiState.update { it.copy(isLoading = false, playlists = playlists) }
                }
            }
            launch {
                repository.getSavedVideoPlaylistsFlow().collect { savedPlaylists ->
                    _uiState.update { it.copy(savedPlaylists = savedPlaylists) }
                }
            }
        }
    }

    fun createPlaylist(
        name: String,
        description: String,
    ) {
        viewModelScope.launch {
            repository.createPlaylist(
                playlistId = System.currentTimeMillis().toString(),
                name = name,
                description = description,
                isPrivate = true,
            )
        }
    }

    suspend fun importPlaylist(
        source: Uri,
        fallbackName: String,
    ): PlaylistImport = transfer.import(source, fallbackName)

    fun setListOrder(order: PlaylistListOrder) {
        viewModelScope.launch { preferences.setPlaylistListOrder(order) }
    }

    fun setCompactLayout(compact: Boolean) {
        viewModelScope.launch { preferences.setPlaylistsCompactLayout(compact) }
    }

    fun reorderPlaylists(orderedIds: List<String>) {
        viewModelScope.launch { repository.reorderPlaylists(orderedIds) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }
}

data class PlaylistsUiState(
    val isLoading: Boolean = false,
    val playlists: List<PlaylistInfo> = emptyList(),
    val savedPlaylists: List<PlaylistInfo> = emptyList(),
)
