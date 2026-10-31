package io.github.pushkar.chorplay.innertube.pages

import io.github.pushkar.chorplay.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)
