package io.github.pushkar.chorplay.data.video.downloader.tags

import androidx.media3.container.MdtaMetadataEntry
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EmbeddedTagsTest {
    private val full =
        DownloadTags(
            schema = 1,
            kind = DownloadKind.VIDEO,
            videoId = "kffacxfA7G4",
            title = "Never Gonna Give You Up",
            channelId = "UCuAXFkgsw1L7xaCfnd5JJOw",
            channelName = "Rick Astley",
            playlistId = "PL123",
            releaseDate = "1987-07-27",
            description = "The official video",
            sourceUrl = "https://www.youtube.com/watch?v=kffacxfA7G4",
            viewCount = 1_500_000_000L,
            likeCount = 17_000_000L,
            thumbnailUrl = "https://i.ytimg.com/vi/kffacxfA7G4/hqdefault.jpg",
        )

    @Test
    fun `mdta entries decode back to the same tags`() {
        val entries =
            ChorplayTagFields.encode(full).map { (field, value) ->
                MdtaMetadataEntry(
                    ChorplayTagFields.mdtaKey(field),
                    value.toByteArray(Charsets.UTF_8),
                    0,
                    MdtaMetadataEntry.TYPE_INDICATOR_STRING,
                )
            }

        val embedded = EmbeddedTags.fromEntries(entries)

        assertThat(embedded.chor).isEqualTo(full)
        assertThat(embedded.title).isEqualTo(full.title)
        assertThat(embedded.artist).isEqualTo(full.displayArtist())
    }

    @Test
    fun `unmatched entries are safely ignored and plain id3 frames fill missing fields`() {
        val entries =
            listOf(
                TextInformationFrame("TIT2", null, listOf("Plain title")),
                TextInformationFrame("TPE1", null, listOf("Plain artist")),
                CommentFrame("eng", "", "https://example.org/watch"),
                VorbisComment("DESCRIPTION", "A nice description"),
            )

        val embedded = EmbeddedTags.fromEntries(entries)

        assertThat(embedded.title).isEqualTo("Plain title")
        assertThat(embedded.artist).isEqualTo("Plain artist")
        assertThat(embedded.comment).isEqualTo("https://example.org/watch")
        assertThat(embedded.description).isEqualTo("A nice description")
    }
}
