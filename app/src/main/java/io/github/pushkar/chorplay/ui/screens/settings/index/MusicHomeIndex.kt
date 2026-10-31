package io.github.pushkar.chorplay.ui.screens.settings.index

import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.ui.components.music.section.MusicHomeShelf
import io.github.pushkar.chorplay.ui.components.settings.SettingEntry
import io.github.pushkar.chorplay.ui.components.settings.SettingsDestination

internal object MusicHomeIndex {
    private val entries: Map<MusicHomeShelf, SettingEntry> =
        MusicHomeShelf.entries.associateWith { shelf ->
            SettingEntry(
                key = "music_home.${shelf.name.lowercase()}",
                title = shelf.labelRes,
                section = R.string.settings_music_home_sections,
                destination = SettingsDestination.MUSIC_HOME,
            )
        }

    fun entry(shelf: MusicHomeShelf): SettingEntry = entries.getValue(shelf)

    val all: List<SettingEntry> = MusicHomeShelf.entries.map(::entry)
}
