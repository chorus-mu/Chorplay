package io.github.pushkar.chorplay.ui.screens.settings.index

import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.ui.components.settings.SettingEntry
import io.github.pushkar.chorplay.ui.components.settings.SettingsDestination
import io.github.pushkar.chorplay.widget.core.ChorWidgetEntry
import io.github.pushkar.chorplay.widget.core.ChorplayWidgets

internal object WidgetsIndex {
    fun entryFor(widget: ChorWidgetEntry) =
        SettingEntry(
            key = "widgets.${widget.id}",
            title = widget.label,
            summary = widget.description,
            section = R.string.settings_widgets_add_header,
            keywords = R.string.settings_keywords_widgets,
            destination = SettingsDestination.WIDGETS,
        )

    val all = ChorplayWidgets.catalog.map(::entryFor)
}
