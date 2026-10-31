package io.github.pushkar.chorplay.ui.tv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.ui.components.layout.navigation.NavigationVisibility
import io.github.pushkar.chorplay.ui.tv.navigation.TvNavigation
import io.github.pushkar.chorplay.ui.tv.navigation.tvNavigation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TvShellViewModel
@Inject
constructor(
    preferences: PlayerPreferences,
) : ViewModel() {
    val navigation: StateFlow<TvNavigation?> =
        combine(
            preferences.navTabOrder,
            preferences.defaultNavTabIndex,
            preferences.homeNavigationEnabled,
        ) { order, defaultTabId, home ->
            tvNavigation(order, defaultTabId, NavigationVisibility(home = home))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
