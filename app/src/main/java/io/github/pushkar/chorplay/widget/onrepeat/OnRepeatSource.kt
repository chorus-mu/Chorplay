package io.github.pushkar.chorplay.widget.onrepeat

import androidx.glance.appwidget.GlanceAppWidget
import io.github.pushkar.chorplay.data.music.model.MusicTrack
import io.github.pushkar.chorplay.data.recommendation.music.MusicBrainEngine
import io.github.pushkar.chorplay.data.recommendation.music.onRepeatShelf
import io.github.pushkar.chorplay.widget.core.refresh.WidgetContentKey
import io.github.pushkar.chorplay.widget.core.refresh.WidgetContentSource
import javax.inject.Inject
import javax.inject.Singleton

/** The Music page's On Repeat shelf. The brain has no change stream, so a finished listen requests a refresh. */
@Singleton
class OnRepeatSource
    @Inject
    constructor(
        private val musicBrain: MusicBrainEngine,
    ) : WidgetContentSource {
        override val key = WidgetContentKey.ON_REPEAT
        override val widget: GlanceAppWidget get() = OnRepeatWidget()

        suspend fun tracks(): List<MusicTrack> = musicBrain.onRepeatShelf().take(MAX_ITEMS)

        override suspend fun signature(): String = tracks().joinToString(",") { it.videoId }

        companion object {
            const val MAX_ITEMS = 8
        }
    }
