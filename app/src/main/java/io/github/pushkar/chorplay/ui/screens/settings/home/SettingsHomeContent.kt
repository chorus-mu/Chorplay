package io.github.pushkar.chorplay.ui.screens.settings.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import io.github.pushkar.chorplay.BuildConfig
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.recommendation.ChorPersona
import io.github.pushkar.chorplay.ui.components.settings.SettingsDestination
import io.github.pushkar.chorplay.ui.components.settings.SettingsGroupScope
import io.github.pushkar.chorplay.ui.components.settings.SettingsListScope
import io.github.pushkar.chorplay.ui.components.settings.SettingsTarget
import io.github.pushkar.chorplay.ui.components.settings.nav
import io.github.pushkar.chorplay.ui.components.settings.switch
import io.github.pushkar.chorplay.ui.components.shared.DeepChorplayState
import io.github.pushkar.chorplay.ui.components.shared.ChorNavRow
import io.github.pushkar.chorplay.ui.components.shared.ChorplaySwitchRow
import io.github.pushkar.chorplay.ui.components.shared.deepChorStatus
import io.github.pushkar.chorplay.ui.screens.settings.index.DestinationIndex
import io.github.pushkar.chorplay.ui.screens.settings.index.HomeIndex

@Immutable
internal data class SettingsHomeState(
    val selected: SettingsDestination?,
    val persona: ChorPersona?,
    val deepChor: DeepChorplayState,
    val updateCheckingLabel: String?,
    val selectedRowFocus: FocusRequester? = null,
)

internal class SettingsHomeActions(
    val onOpen: (SettingsTarget) -> Unit,
    val onOpenDonations: () -> Unit,
    val onDeepChorplayChange: (Boolean) -> Unit,
    val onDurationClick: () -> Unit,
    val onSaveHistoryChange: (Boolean) -> Unit,
    val onShowOnSearchChange: (Boolean) -> Unit,
    val onCheckForUpdates: () -> Unit,
)

internal fun SettingsListScope.homeContent(
    state: SettingsHomeState,
    actions: SettingsHomeActions,
) {
    item(HomeIndex.persona.key) {
        PersonaEntryCard(
            persona = state.persona,
            selected = state.selected == SettingsDestination.TASTE,
            onOpen = { actions.onOpen(SettingsTarget(SettingsDestination.TASTE)) },
            modifier = state.selectedFocusModifier(SettingsDestination.TASTE),
        )
    }

    fun SettingsGroupScope.page(destination: SettingsDestination) =
        nav(
            entry = DestinationIndex.entry(destination),
            onClick = { actions.onOpen(SettingsTarget(destination)) },
            icon = destinationIcon(destination),
            selected = state.selected == destination,
            showChevron = state.selected == null,
            modifier = state.selectedFocusModifier(destination),
        )

    val deepChorHint = R.string.deep_chor_long_press_hint.takeUnless { onTv }
    group(key = "home.engine", header = R.string.settings_chor_engine_header, footer = deepChorHint) {
        row(HomeIndex.deepChor.key) { shape ->
            ChorplaySwitchRow(
                title = stringResource(HomeIndex.deepChor.title),
                supportingText = deepChorStatus(state.deepChor) ?: stringResource(R.string.deep_chor_mode_subtitle),
                checked = state.deepChor.active,
                onCheckedChange = actions.onDeepChorplayChange,
                leadingIcon = HomeRowIcons.DeepChorplay,
                shape = shape,
            )
        }
        row(HomeIndex.deepChorDuration.key) { shape ->
            ChorNavRow(
                title = stringResource(HomeIndex.deepChorDuration.title),
                supportingText = deepChorDurationLabel(state.deepChor.expireHours),
                onClick = actions.onDurationClick,
                leadingIcon = HomeRowIcons.DeepChorplayDuration,
                shape = shape,
            )
        }
        switch(
            HomeIndex.deepChorHistory,
            checked = state.deepChor.saveToHistory,
            onCheckedChange = actions.onSaveHistoryChange,
            icon = HomeRowIcons.DeepChorplayHistory,
        )
        switch(
            HomeIndex.deepChorOnSearch,
            checked = state.deepChor.showOnSearch,
            onCheckedChange = actions.onShowOnSearchChange,
            icon = HomeRowIcons.DeepChorplayOnSearch,
        )
        page(SettingsDestination.TOPICS)
    }
    group(key = "home.look", header = R.string.settings_group_look) {
        page(SettingsDestination.APPEARANCE)
        page(SettingsDestination.LANGUAGE_REGION)
        page(SettingsDestination.WIDGETS)
    }
    group(key = "home.watching", header = R.string.settings_group_watching) {
        page(SettingsDestination.PLAYBACK)
        page(SettingsDestination.QUALITY)
        page(SettingsDestination.CONTENT)
        page(SettingsDestination.INTEGRATIONS)
    }
    group(key = "home.data", header = R.string.settings_group_data) {
        page(SettingsDestination.PROFILES)
        page(SettingsDestination.BACKUP)
        page(SettingsDestination.SYNC)
        page(SettingsDestination.HISTORY)
        page(SettingsDestination.DOWNLOADS)
    }
    group(key = "home.system", header = R.string.settings_group_system) {
        page(SettingsDestination.NOTIFICATIONS)
        page(SettingsDestination.NETWORK)
        page(SettingsDestination.WELLBEING)
    }
    group(key = "home.about", header = R.string.settings_header_about) {
        page(SettingsDestination.ABOUT)
        page(SettingsDestination.DIAGNOSTICS)
        if (BuildConfig.UPDATER_ENABLED) {
            nav(
                HomeIndex.checkForUpdates,
                onClick = actions.onCheckForUpdates,
                value = state.updateCheckingLabel,
                enabled = state.updateCheckingLabel == null,
                showChevron = false,
                icon = HomeRowIcons.Updates,
            )
        }
        nav(HomeIndex.support, onClick = actions.onOpenDonations, icon = HomeRowIcons.Support)
    }
}

private fun SettingsHomeState.selectedFocusModifier(destination: SettingsDestination): Modifier =
    selectedRowFocus?.takeIf { selected == destination }?.let { Modifier.focusRequester(it) } ?: Modifier
