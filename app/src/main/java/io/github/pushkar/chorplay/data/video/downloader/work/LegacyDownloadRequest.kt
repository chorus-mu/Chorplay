package io.github.pushkar.chorplay.data.video.downloader.work

import io.github.pushkar.chorplay.data.local.entity.DownloadItemStatus
import io.github.pushkar.chorplay.data.local.entity.DownloadWithItems
import io.github.pushkar.chorplay.data.video.downloader.request.DownloadRequest
import io.github.pushkar.chorplay.data.video.downloader.request.sourceUrlFor
import io.github.pushkar.chorplay.data.video.downloader.tags.DownloadKind
import io.github.pushkar.chorplay.data.video.downloader.tags.DownloadTags
import io.github.pushkar.chorplay.data.video.storage.DownloadFiles
import java.io.File

/**
 * The request behind a download row written before requests were stored, rebuilt from the row:
 * the quality its label named, and the metadata the row kept.
 */
internal object LegacyDownloadRequest {
    private val HEIGHT = Regex("""(\d{3,4})p""")

    fun from(row: DownloadWithItems): DownloadRequest {
        val entity = row.download
        val quality =
            row.items
                .firstOrNull()
                ?.quality
                .orEmpty()
        return DownloadRequest(
            tags =
                DownloadTags(
                    kind = entity.kind,
                    videoId = entity.videoId,
                    title = entity.title,
                    channelId = entity.channelId.takeIf { it.isNotBlank() },
                    channelName = entity.uploader.takeIf { it.isNotBlank() },
                    releaseDate = entity.releaseDate,
                    description = entity.description.takeIf { it.isNotBlank() },
                    sourceUrl = sourceUrlFor(entity.kind, entity.videoId),
                    viewCount = entity.viewCount.takeIf { it > 0 },
                    likeCount = entity.likeCount.takeIf { it > 0 },
                    thumbnailUrl = entity.thumbnailUrl.takeIf { it.isNotBlank() },
                ),
            audioOnly = row.isAudioOnly,
            targetHeight =
                HEIGHT
                    .find(quality)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull(),
            videoCodec = codecIn(quality),
            durationSeconds = entity.duration.toInt(),
        )
    }

    /**
     * The old downloader wrote unfinished files next to their final name in the public folder.
     * A download it left unfinished starts over in staging, so those leftovers go.
     */
    fun discardPartialFiles(row: DownloadWithItems) {
        row.items
            .filter { it.status != DownloadItemStatus.COMPLETED && !DownloadFiles.isDocument(it.filePath) }
            .flatMap { listOf(it.filePath, "${it.filePath}.video.tmp", "${it.filePath}.audio.tmp") }
            .forEach { File(it).delete() }
    }

    private fun codecIn(quality: String): String? {
        val label = quality.lowercase()
        return when {
            "av1" in label -> "av1"
            "vp9" in label -> "vp9"
            "hevc" in label -> "hevc"
            "h264" in label || "avc" in label -> "h264"
            else -> null
        }
    }
}
