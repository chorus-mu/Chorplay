package io.github.pushkar.chorplay.data.video.downloader.tags

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadKind { VIDEO, SHORT }

@Serializable
data class DownloadTags(
    val schema: Int = SCHEMA_VERSION,
    val kind: DownloadKind,
    val videoId: String,
    val title: String,
    val channelId: String? = null,
    val channelName: String? = null,
    val playlistId: String? = null,
    val releaseDate: String? = null,
    val description: String? = null,
    val sourceUrl: String? = null,
    val viewCount: Long? = null,
    val likeCount: Long? = null,
    val thumbnailUrl: String? = null,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

private val TopicSuffix = Regex("""(^|\s)-\s*Topic$""")

fun DownloadTags.displayArtist(): String? =
    channelName
        ?.trim()
        ?.replace(TopicSuffix, "")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
