package io.github.pushkar.chorplay.data.video.downloader.request

import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.video.downloader.tags.DownloadKind
import io.github.pushkar.chorplay.data.video.downloader.tags.DownloadTags

/** The request for a video, from what the card or player already knows about it. */
fun Video.toDownloadRequest(
    audioOnly: Boolean = false,
    targetHeight: Int? = null,
    videoCodec: String? = null,
    videoItag: Int? = null,
    audioItag: Int? = null,
    audioTrackId: String? = null,
    audioLanguage: String? = null,
    collectionId: String? = null,
    threads: Int? = null,
    subtitle: DownloadSubtitle? = null,
): DownloadRequest {
    val kind =
        when {
            isShort -> DownloadKind.SHORT
            else -> DownloadKind.VIDEO
        }
    return DownloadRequest(
        tags =
            DownloadTags(
                kind = kind,
                videoId = id,
                title = title,
                channelId = channelId.takeIf { it.isNotBlank() },
                channelName = channelName.takeIf { it.isNotBlank() },
                description = description.takeIf { it.isNotBlank() },
                sourceUrl = sourceUrlFor(kind, id),
                viewCount = viewCount.takeIf { it > 0 },
                likeCount = likeCount.takeIf { it > 0 },
                thumbnailUrl = thumbnailUrl.takeIf { it.isNotBlank() },
            ),
        audioOnly = audioOnly,
        targetHeight = targetHeight,
        videoCodec = videoCodec,
        videoItag = videoItag,
        audioItag = audioItag,
        audioTrackId = audioTrackId,
        audioLanguage = audioLanguage,
        collectionId = collectionId,
        threads = threads,
        durationSeconds = duration,
        subtitle = subtitle,
    )
}

internal fun sourceUrlFor(
    kind: DownloadKind,
    videoId: String,
): String =
    when (kind) {
        DownloadKind.SHORT -> "https://www.youtube.com/shorts/$videoId"
        DownloadKind.VIDEO -> "https://www.youtube.com/watch?v=$videoId"
    }
