package io.github.pushkar.chorplay.data.video.downloader.tags

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DownloadTagsTest {
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
    fun `defaults populate schema and optional fields as null`() {
        val minimal =
            DownloadTags(
                kind = DownloadKind.VIDEO,
                videoId = "v",
                title = "Title",
            )

        assertThat(minimal.schema).isEqualTo(1)
        assertThat(minimal.channelId).isNull()
        assertThat(minimal.channelName).isNull()
        assertThat(minimal.playlistId).isNull()
    }

    @Test
    fun `display artist falls back to the channel without the topic suffix`() {
        val tags = full.copy(channelName = "Queen - Topic")
        assertThat(tags.displayArtist()).isEqualTo("Queen")
    }

    @Test
    fun `field codec round trips everything`() {
        val fields = ChorplayTagFields.encode(full)

        assertThat(ChorplayTagFields.decode(fields)).isEqualTo(full)
    }

    @Test
    fun `field decoder rejects missing required fields`() {
        val valid = ChorplayTagFields.encode(full)

        assertThat(ChorplayTagFields.decode(valid - ChorplayTagFields.VIDEO_ID)).isNull()
        assertThat(ChorplayTagFields.decode(valid - ChorplayTagFields.KIND)).isNull()
        assertThat(ChorplayTagFields.decode(valid - ChorplayTagFields.TITLE)).isNull()
        assertThat(ChorplayTagFields.decode(valid + (ChorplayTagFields.KIND to "NOT_A_KIND"))).isNull()
    }

    @Test
    fun `text truncation respects unicode surrogate pairs`() {
        val emoji = "Hi \uD83D\uDE00 there"
        assertThat(ChorplayTagFields.truncate(emoji, 5)).isEqualTo("Hi ")
        assertThat(ChorplayTagFields.truncate(emoji, 6)).isEqualTo("Hi \uD83D\uDE00")
        assertThat(ChorplayTagFields.truncate(emoji, 100)).isEqualTo(emoji)
    }
}
