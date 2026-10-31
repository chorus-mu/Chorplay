package io.github.pushkar.chorplay.ui.screens.recap

import com.google.common.truth.Truth.assertThat
import io.github.pushkar.chorplay.data.stats.RankedItem
import io.github.pushkar.chorplay.data.stats.RankedKind
import io.github.pushkar.chorplay.data.stats.RecapPeriod
import io.github.pushkar.chorplay.utils.YouTubeLink
import org.junit.Test

class RecapLinksTest {
    private fun item(
        id: String,
        kind: RankedKind,
    ) = RankedItem(id = id, name = id, count = 1, kind = kind)

    @Test
    fun `each kind opens its own page`() {
        assertThat(item("UCchan", RankedKind.CHANNEL).link()).isEqualTo(YouTubeLink.Channel("UCchan", isMusic = false))
        assertThat(item("vid", RankedKind.VIDEO).link()).isEqualTo(YouTubeLink.Video("vid", isMusic = false))
        assertThat(item("UCartist", RankedKind.ARTIST).link()).isEqualTo(YouTubeLink.Channel("UCartist", isMusic = true))
        assertThat(item("song", RankedKind.TRACK).link()).isEqualTo(YouTubeLink.Video("song", isMusic = true))
        assertThat(item("MPREb_x", RankedKind.ALBUM).link()).isEqualTo(YouTubeLink.Album("MPREb_x"))
    }

    @Test
    fun `rows with nowhere to go stay plain`() {
        assertThat(item("some artist", RankedKind.ARTIST).link()).isNull()
        assertThat(item("local_1", RankedKind.TRACK).link()).isNull()
        assertThat(item("music", RankedKind.OTHER).link()).isNull()
    }

    @Test
    fun `the story route carries the tab it was played from`() {
        val route = RecapRoutes.story(RecapPeriod.Year(2026), RecapSource.VIDEO)

        assertThat(route).isEqualTo("recap/story/2026?source=VIDEO")
        assertThat(RecapRoutes.decodeSource("VIDEO")).isEqualTo(RecapSource.VIDEO)
        assertThat(RecapRoutes.decodeSource(null)).isEqualTo(RecapSource.ALL)
    }
}
