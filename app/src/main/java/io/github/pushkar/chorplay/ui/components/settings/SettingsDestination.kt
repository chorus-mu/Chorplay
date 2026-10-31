package io.github.pushkar.chorplay.ui.components.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import io.github.pushkar.chorplay.R

enum class SettingsDestination(
    val id: String,
    @StringRes val titleRes: Int,
    val parent: SettingsDestination? = null,
) {

    HOME("home", R.string.settings_title),
    TASTE("taste", R.string.taste_title),
    HIDDEN_CONTENT("hidden_content", R.string.taste_hidden_title, TASTE),

    DISCOVER_CHANNELS("discover_channels", R.string.discover_channels_title, TASTE),
    APPEARANCE("appearance", R.string.appearance_title),
    THEME("theme", R.string.settings_item_theme, APPEARANCE),
    FONT("font", R.string.settings_font_title, APPEARANCE),
    CUSTOM_THEME("custom_theme", R.string.settings_custom_themes_title, THEME),
    CUSTOM_THEME_EDIT("custom_theme_edit", R.string.settings_custom_theme_editor_title, CUSTOM_THEME),
    NAVIGATION_BAR("navigation_bar", R.string.settings_navigation_bar_title, APPEARANCE),
    DATE_TIME("date_time", R.string.settings_item_datetime, APPEARANCE),
    PLAYER_APPEARANCE("player_appearance", R.string.player_appearance_title, APPEARANCE),
    LANGUAGE_REGION("language_region", R.string.settings_language_region_title),
    WIDGETS("widgets", R.string.settings_widgets_title),
    PLAYBACK("playback", R.string.settings_playback_title),
    BUFFER("buffer", R.string.buffer_settings_title, PLAYBACK),
    EQUALIZER("equalizer", R.string.equalizer, PLAYBACK),
    QUALITY("quality", R.string.quality),
    CONTENT("content", R.string.settings_content_title),

    LIBRARY("library", R.string.library, CONTENT),
    TOPICS("topics", R.string.settings_topics_title),
    INTEGRATIONS("integrations", R.string.settings_integrations_title),

    BACKUP("backup", R.string.settings_backup_title),
    SYNC("sync", R.string.sync_devices_title),
    PROFILES("profiles", R.string.profiles_title),
    PROFILE_EDIT("profile_edit", R.string.profiles_edit_title, PROFILES),
    HISTORY("history", R.string.settings_history_title),
    DOWNLOADS("downloads", R.string.settings_downloads_title),
    LOCAL_MEDIA("local_media", R.string.settings_local_media_title, DOWNLOADS),
    NOTIFICATIONS("notifications", R.string.settings_header_notifications),
    NETWORK("network", R.string.settings_network_title),
    WELLBEING("wellbeing", R.string.settings_wellbeing_title),
    ABOUT("about", R.string.settings_item_about_chorplay),
    REMOTE_GUIDE("remote_guide", R.string.tv_remote_guide_title, ABOUT),
    DIAGNOSTICS("diagnostics", R.string.settings_item_diagnostics),
    ;

    val root: SettingsDestination
        get() = parent?.root ?: this

    val path: List<SettingsDestination>
        get() = (parent?.path ?: emptyList()) + this

    companion object {
        fun fromId(id: String): SettingsDestination? = entries.firstOrNull { it.id == id }
    }
}

object SettingsTabs {
    const val QUALITY_VIDEO = "video"
    const val QUALITY_SHORTS = "shorts"

    const val BACKUP_EXPORT = "export"
    const val BACKUP_IMPORT = "import"
    const val BACKUP_AUTO = "auto"
    const val TOPICS_INTERESTS = "interests"
    const val TOPICS_BLOCKED = "blocked"
}

@Immutable
data class SettingsTarget(
    val destination: SettingsDestination,
    val tab: String? = null,
    val highlight: String? = null,
) {
    fun encode(): String =
        buildString {
            append(destination.id)
            if (tab != null) append(TAB_SEPARATOR).append(tab)
            if (highlight != null) append(HIGHLIGHT_SEPARATOR).append(highlight)
        }

    companion object {
        private const val TAB_SEPARATOR = '~'
        private const val HIGHLIGHT_SEPARATOR = '#'

        fun decode(value: String?): SettingsTarget? {
            if (value.isNullOrBlank()) return null
            val highlight = value.substringAfter(HIGHLIGHT_SEPARATOR, "").ifBlank { null }
            val head = value.substringBefore(HIGHLIGHT_SEPARATOR)
            val tab = head.substringAfter(TAB_SEPARATOR, "").ifBlank { null }
            val destination = SettingsDestination.fromId(head.substringBefore(TAB_SEPARATOR)) ?: return null
            return SettingsTarget(destination, tab, highlight)
        }
    }
}
