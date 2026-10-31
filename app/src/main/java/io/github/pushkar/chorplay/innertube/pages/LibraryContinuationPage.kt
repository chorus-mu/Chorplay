package io.github.pushkar.chorplay.innertube.pages

import io.github.pushkar.chorplay.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
