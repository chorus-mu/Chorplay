package io.github.pushkar.chorplay.ui.screens.settings.index

import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.ui.components.settings.SettingAvailability
import io.github.pushkar.chorplay.ui.components.settings.SettingEntry
import io.github.pushkar.chorplay.ui.components.settings.SettingsDestination

/** Options that live directly on the settings list rather than on a page of their own. */
internal object HomeIndex {
    private val home = SettingsDestination.HOME

    val persona =
        SettingEntry(
            key = "home.persona",
            title = R.string.taste_title,
            summary = R.string.taste_summary,
            section = R.string.settings_chor_engine_header,
            destination = home,
        )
    val deepChor =
        SettingEntry(
            key = "home.deep_chor",
            title = R.string.deep_chor_mode_title,
            summary = R.string.deep_chor_mode_subtitle,
            section = R.string.settings_chor_engine_header,
            destination = home,
        )
    val deepChorDuration =
        SettingEntry(
            key = "home.deep_chor_duration",
            title = R.string.deep_chor_expire_duration_title,
            section = R.string.settings_chor_engine_header,
            destination = home,
        )
    val deepChorHistory =
        SettingEntry(
            key = "home.deep_chor_history",
            title = R.string.deep_chor_save_history_title,
            summary = R.string.deep_chor_save_history_subtitle,
            section = R.string.settings_chor_engine_header,
            destination = home,
        )
    val deepChorOnSearch =
        SettingEntry(
            key = "home.deep_chor_on_search",
            title = R.string.deep_chor_on_search_title,
            summary = R.string.deep_chor_on_search_subtitle,
            section = R.string.settings_chor_engine_header,
            destination = home,
        )
    val checkForUpdates =
        SettingEntry(
            key = "home.updates",
            title = R.string.check_for_updates,
            summary = R.string.check_for_updates_subtitle,
            section = R.string.settings_header_about,
            destination = home,
            availability = SettingAvailability.GithubOnly,
        )
    val support =
        SettingEntry(
            key = "home.support",
            title = R.string.settings_item_support,
            summary = R.string.settings_item_support_subtitle,
            section = R.string.settings_header_about,
            destination = home,
        )

    /** The persona card is found through Your taste's own destination entry, not listed twice. */
    val all = listOf(deepChor, deepChorDuration, deepChorHistory, deepChorOnSearch, checkForUpdates, support)
}
