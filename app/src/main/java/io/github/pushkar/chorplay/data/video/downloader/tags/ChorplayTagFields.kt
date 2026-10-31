package io.github.pushkar.chorplay.data.video.downloader.tags

/**
 * The flat key/value form of [DownloadTags] that is written twice into a file: as MP4 `mdta`
 * entries keyed `io.github.pushkar.chorplay.<field>` and as iTunes `----` atoms with mean
 * [NAMESPACE] and name `<field>`.
 */
object ChorplayTagFields {
    const val NAMESPACE = "io.github.pushkar.chorplay"
    const val DESCRIPTION_MAX_CHARS = 2_000

    const val SCHEMA = "schema"
    const val KIND = "kind"
    const val VIDEO_ID = "videoId"
    const val TITLE = "title"
    const val CHANNEL_ID = "channelId"
    const val CHANNEL_NAME = "channelName"
    const val PLAYLIST_ID = "playlistId"
    const val RELEASE_DATE = "releaseDate"
    const val DESCRIPTION = "description"
    const val SOURCE_URL = "sourceUrl"
    const val VIEW_COUNT = "viewCount"
    const val LIKE_COUNT = "likeCount"
    const val THUMBNAIL_URL = "thumbnailUrl"

    fun mdtaKey(field: String): String = "$NAMESPACE.$field"

    fun fieldFromMdtaKey(key: String): String? = key.removePrefix("$NAMESPACE.").takeIf { it != key && it.isNotEmpty() }

    fun encode(tags: DownloadTags): Map<String, String> =
        buildMap {
            put(SCHEMA, tags.schema.toString())
            put(KIND, tags.kind.name)
            put(VIDEO_ID, tags.videoId)
            put(TITLE, tags.title)
            putIfPresent(CHANNEL_ID, tags.channelId)
            putIfPresent(CHANNEL_NAME, tags.channelName)
            putIfPresent(PLAYLIST_ID, tags.playlistId)
            putIfPresent(RELEASE_DATE, tags.releaseDate)
            putIfPresent(DESCRIPTION, tags.description?.let { truncate(it, DESCRIPTION_MAX_CHARS) })
            putIfPresent(SOURCE_URL, tags.sourceUrl)
            putIfPresent(VIEW_COUNT, tags.viewCount?.toString())
            putIfPresent(LIKE_COUNT, tags.likeCount?.toString())
            putIfPresent(THUMBNAIL_URL, tags.thumbnailUrl)
        }

    /** Rebuilds tags from [fields]; null when the video id, kind or title is missing. */
    fun decode(fields: Map<String, String>): DownloadTags? {
        val videoId = fields[VIDEO_ID]?.takeIf { it.isNotBlank() } ?: return null
        val kind = fields[KIND]?.let { name -> DownloadKind.entries.firstOrNull { it.name == name } } ?: return null
        val title = fields[TITLE]?.takeIf { it.isNotBlank() } ?: return null
        return DownloadTags(
            schema = fields[SCHEMA]?.toIntOrNull() ?: DownloadTags.SCHEMA_VERSION,
            kind = kind,
            videoId = videoId,
            title = title,
            channelId = fields[CHANNEL_ID],
            channelName = fields[CHANNEL_NAME],
            playlistId = fields[PLAYLIST_ID],
            releaseDate = fields[RELEASE_DATE],
            description = fields[DESCRIPTION],
            sourceUrl = fields[SOURCE_URL],
            viewCount = fields[VIEW_COUNT]?.toLongOrNull(),
            likeCount = fields[LIKE_COUNT]?.toLongOrNull(),
            thumbnailUrl = fields[THUMBNAIL_URL],
        )
    }

    /** Cuts [text] to at most [maxChars] UTF-16 units without splitting a surrogate pair. */
    fun truncate(
        text: String,
        maxChars: Int,
    ): String {
        if (text.length <= maxChars) return text
        val end = if (Character.isHighSurrogate(text[maxChars - 1])) maxChars - 1 else maxChars
        return text.substring(0, end)
    }

    private fun MutableMap<String, String>.putIfPresent(
        key: String,
        value: String?,
    ) {
        if (!value.isNullOrEmpty()) put(key, value)
    }
}
