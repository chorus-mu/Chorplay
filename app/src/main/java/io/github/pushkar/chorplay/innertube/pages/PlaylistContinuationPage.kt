package io.github.pushkar.chorplay.innertube.pages

import io.github.pushkar.chorplay.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
