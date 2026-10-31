package io.github.pushkar.chorplay.ui.screens.settings.index

import io.github.pushkar.chorplay.ui.components.settings.SettingEntry

internal object SettingsIndex {
    val all: List<SettingEntry> by lazy {
        HomeIndex.all +
                TasteIndex.all +
                DestinationIndex.all +
                AppearanceIndex.all +
                ThemeIndex.all +
                CustomThemeIndex.all +
                FontIndex.all +
                NavigationBarIndex.all +
                DateTimeIndex.all +
                PlayerAppearanceIndex.all +
                LanguageRegionIndex.all +
                PlaybackIndex.all +
                BufferIndex.all +
                QualityIndex.all +
                ContentIndex.all +

                LibrarySectionsIndex.all +
                TopicsIndex.all +
                IntegrationsIndex.all +

                ProfilesIndex.all +
                BackupIndex.all +
                DownloadsIndex.all +
                LocalMediaIndex.all +
                NotificationsIndex.all +
                NetworkIndex.all +
                HistoryIndex.all +
                WellbeingIndex.all +
                WidgetsIndex.all +
                DiagnosticsIndex.all +
                AboutIndex.all
    }
}
