package io.github.pushkar.chorplay.data.video.downloader.tags

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.InternalFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment

@OptIn(UnstableApi::class)
class EmbeddedTags(
    val chor: DownloadTags?,
    val title: String?,
    val artist: String?,
    val cover: ByteArray?,
    val description: String? = null,
    val comment: String? = null,
) {
    fun withFallback(
        title: String?,
        artist: String?,
        cover: ByteArray?,
    ): EmbeddedTags =
        EmbeddedTags(
            chor = chor,
            title = this.title ?: title,
            artist = this.artist ?: artist,
            cover = this.cover ?: cover,
            description = description,
            comment = comment,
        )

    companion object {
        fun fromEntries(entries: List<Metadata.Entry>): EmbeddedTags {
            val fields = linkedMapOf<String, String>()
            val frames = linkedMapOf<String, String>()
            var comment: String? = null
            val vorbis = linkedMapOf<String, String>()
            entries.forEach { entry ->
                when (entry) {
                    is MdtaMetadataEntry -> {
                        if (entry.typeIndicator == MdtaMetadataEntry.TYPE_INDICATOR_STRING) {
                            ChorplayTagFields.fieldFromMdtaKey(entry.key)
                                ?.let { fields.putIfAbsent(it, String(entry.value, Charsets.UTF_8)) }
                        }
                    }

                    is InternalFrame -> {
                        if (entry.domain == ChorplayTagFields.NAMESPACE) fields.putIfAbsent(
                            entry.description,
                            entry.text
                        )
                    }

                    is TextInformationFrame -> {
                        entry.values.firstOrNull()?.let { frames.putIfAbsent(entry.id, it) }
                    }

                    is CommentFrame -> {
                        if (comment == null) comment = entry.text
                    }

                    is VorbisComment -> {
                        vorbis.putIfAbsent(entry.key.uppercase(), entry.value)
                    }
                }
            }
            val title = frames[FRAME_TITLE] ?: fields[ChorplayTagFields.TITLE]
            if (title != null) fields.putIfAbsent(ChorplayTagFields.TITLE, title)
            val chor = ChorplayTagFields.decode(fields)?.fillFromFrames(frames, comment)
            return EmbeddedTags(
                chor = chor,
                title = title,
                artist = frames[FRAME_ARTIST] ?: chor?.displayArtist(),
                cover = entries.coverPicture(),
                description = (chor?.description ?: vorbis[VORBIS_DESCRIPTION])?.takeIf(String::isNotBlank),
                comment = (comment ?: vorbis[VORBIS_COMMENT])?.takeIf(String::isNotBlank),
            )
        }

        private fun DownloadTags.fillFromFrames(
            frames: Map<String, String>,
            comment: String?,
        ): DownloadTags =
            copy(
                channelName = channelName ?: frames[FRAME_ARTIST],
                releaseDate = releaseDate ?: frames[FRAME_DATE],
                sourceUrl = sourceUrl ?: comment,
            )

        private const val FRAME_TITLE = "TIT2"
        private const val FRAME_ARTIST = "TPE1"
        private const val FRAME_DATE = "TDRC"
        private const val VORBIS_DESCRIPTION = "DESCRIPTION"
        private const val VORBIS_COMMENT = "COMMENT"
    }
}
