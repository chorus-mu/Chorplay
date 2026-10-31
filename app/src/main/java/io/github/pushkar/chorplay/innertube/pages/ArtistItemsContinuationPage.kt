package io.github.pushkar.chorplay.innertube.pages

import io.github.pushkar.chorplay.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
