package io.github.pushkar.chorplay.data.local

internal fun String.searchHistoryKey(): String = trim().lowercase()

internal fun List<SearchHistoryItem>.withSearch(
    query: String,
    type: SearchType,
    filters: SearchFilter?,
    maxSize: Int,
    now: Long,
): List<SearchHistoryItem> {
    val key = query.searchHistoryKey()
    val entry = SearchHistoryItem(query = query, timestamp = now, type = type, filters = filters)
    return (listOf(entry) + filterNot { it.query.searchHistoryKey() == key }).take(maxSize)
}

internal fun SearchHistoryItem.sanitized(): SearchHistoryItem = copy(type = type.orDefault(SearchType.TEXT), filters = filters?.sanitized())

internal fun SearchFilter.sanitized(): SearchFilter =
    SearchFilter(
        contentType = contentType.orDefault(ContentType.ALL),
        duration = duration.orDefault(Duration.ANY),
        uploadDate = uploadDate.orDefault(UploadDate.ANY),
        sortType = sortType.orDefault(SortType.RELEVANCE),
        features = features.orDefault(emptySet()).filterNotNull().toSet(),
        watchState = watchState.orDefault(WatchState.ANY),
    )

fun SearchFilter.availableWith(shortsEnabled: Boolean): SearchFilter =
    if (!shortsEnabled && contentType == ContentType.SHORTS) copy(contentType = ContentType.ALL) else this

private fun <T : Any> T?.orDefault(default: T): T = this ?: default
