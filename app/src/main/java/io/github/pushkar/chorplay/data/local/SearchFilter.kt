package io.github.pushkar.chorplay.data.local

data class SearchFilter(
    val contentType: ContentType = ContentType.ALL,
    val duration: Duration = Duration.ANY,
    val uploadDate: UploadDate = UploadDate.ANY,
    val sortType: SortType = SortType.RELEVANCE,
    val features: Set<SearchFeature> = emptySet(),
    val watchState: WatchState = WatchState.ANY,
) {
    val isDefault: Boolean
        get() = this == DEFAULT

    val activeCount: Int
        get() =
            listOf(
                contentType != ContentType.ALL,
                duration != Duration.ANY,
                uploadDate != UploadDate.ANY,
                sortType != SortType.RELEVANCE,
                watchState != WatchState.ANY,
            ).count { it } + features.size

    companion object {
        val DEFAULT = SearchFilter()
    }
}

enum class ContentType {
    ALL,
    VIDEOS,
    SHORTS,
    CHANNELS,
    PLAYLISTS,
    MOVIES,
    LIVE,
}

enum class SortType {
    RELEVANCE,
    VIEW_COUNT,
}

enum class Duration {
    ANY,
    UNDER_3_MINUTES,
    THREE_TO_20_MINUTES,
    OVER_20_MINUTES,
}

enum class UploadDate {
    ANY,
    LAST_HOUR,
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
}

enum class WatchState {
    ANY,
    UNWATCHED,
    WATCHED,
}

enum class SearchFeature {
    HD,
    FOUR_K,
    HDR,
    SUBTITLES,
    CREATIVE_COMMONS,
    THREE_SIXTY,
    VR180,
    THREE_D,
    LOCATION,
    PURCHASED,
}
