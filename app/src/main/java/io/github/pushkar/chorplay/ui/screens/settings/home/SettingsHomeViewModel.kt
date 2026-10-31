package io.github.pushkar.chorplay.ui.screens.settings.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.BuildConfig
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.recommendation.ChorplayNeuroEngine
import io.github.pushkar.chorplay.data.recommendation.ChorPersona
import io.github.pushkar.chorplay.data.update.AppRelease
import io.github.pushkar.chorplay.data.update.UpdateRepository
import io.github.pushkar.chorplay.player.DeepChorplayManager
import io.github.pushkar.chorplay.ui.components.shared.DeepChorplayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState

    data object Checking : UpdateCheckState

    data object UpToDate : UpdateCheckState

    data object Failed : UpdateCheckState

    data class Available(
        val release: AppRelease,
    ) : UpdateCheckState
}

@HiltViewModel
class SettingsHomeViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val playerPreferences: PlayerPreferences,
    private val updates: UpdateRepository,
) : ViewModel() {
    val deepChor: StateFlow<DeepChorplayState> =
        combine(
            playerPreferences.deepChorActive,
            playerPreferences.deepChorActivatedAt,
            playerPreferences.deepChorExpireHours,
            playerPreferences.deepChorSaveToHistory,
        ) { active, activatedAt, expireHours, saveToHistory ->
            DeepChorplayState(active, activatedAt, expireHours, saveToHistory)
        }.combine(playerPreferences.deepChorOnSearch) { state, onSearch ->
            state.copy(showOnSearch = onSearch)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DeepChorplayState())

    private val _persona = MutableStateFlow<ChorPersona?>(null)
    val persona: StateFlow<ChorPersona?> = _persona.asStateFlow()

    private val _updateCheck = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheck: StateFlow<UpdateCheckState> = _updateCheck.asStateFlow()

    init {
        refreshPersona()
    }

    fun refreshPersona() {
        viewModelScope.launch {
            _persona.value =
                runCatching { ChorplayNeuroEngine.getPersona(ChorplayNeuroEngine.getBrainSnapshot()) }.getOrNull()
        }
    }

    fun setDeepChorplayEnabled(enabled: Boolean) {
        viewModelScope.launch { DeepChorplayManager.setEnabled(context, enabled) }
    }

    fun setDeepChorplayExpireHours(hours: Int) {
        viewModelScope.launch { playerPreferences.setDeepChorplayExpireHours(hours) }
    }

    fun setDeepChorplaySaveToHistory(enabled: Boolean) {
        viewModelScope.launch { playerPreferences.setDeepChorplaySaveToHistory(enabled) }
    }

    fun setDeepChorplayOnSearch(enabled: Boolean) {
        viewModelScope.launch { playerPreferences.setDeepChorplayOnSearch(enabled) }
    }

    fun checkForUpdates() {
        if (!BuildConfig.UPDATER_ENABLED || _updateCheck.value == UpdateCheckState.Checking) return
        _updateCheck.value = UpdateCheckState.Checking
        viewModelScope.launch {
            val result =
                runCatching { updates.fetch() }
                    .fold(
                        onSuccess = { info ->
                            info?.let { UpdateCheckState.Available(it) } ?: UpdateCheckState.UpToDate
                        },
                        onFailure = { UpdateCheckState.Failed },
                    )
            _updateCheck.value = result
        }
    }

    fun consumeUpdateCheck() {
        _updateCheck.update { if (it == UpdateCheckState.Checking) it else UpdateCheckState.Idle }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
