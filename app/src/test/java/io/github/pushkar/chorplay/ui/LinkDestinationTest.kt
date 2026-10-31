package io.github.pushkar.chorplay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkDestinationTest {
    private val video = "dQw4w9WgXcQ"
    private val channel = "UCuAXFkgsw1L7xaCfnd5JJOw"

    private fun destinationOf(text: String): LinkDestination? =
        LinkDestinationCoordinator.destinationOf(text)

    private fun page(route: String): LinkDestination = LinkDestination.Page(route)

    @Test
    fun `direct video and short links play their video`() {
        assertEquals(LinkDestination.Video(video), destinationOf("https://www.youtube.com/watch?v=$video"))
        assertEquals(LinkDestination.Video(video), destinationOf("https://youtu.be/$video?si=x"))
        assertEquals(LinkDestination.Video(video), destinationOf("https://www.youtube.com/watch?v=$video&list=RDabc"))
        assertEquals(LinkDestination.Short(video), destinationOf("https://youtube.com/shorts/$video"))
    }

    @Test
    fun `youtube music links play as videos in video player`() {
        assertEquals(LinkDestination.Video(video), destinationOf("https://music.youtube.com/watch?v=$video&si=x"))
        assertEquals(LinkDestination.Page("linkedPlaylist/PLabcdef/$video", playsVideo = true), destinationOf("https://music.youtube.com/watch?v=$video&list=PLabcdef"))
        assertEquals(LinkDestination.Video(video), destinationOf("https://music.youtube.com/watch?v=$video&list=LM"))
    }

    @Test
    fun `a video linked inside a playlist plays with the playlist as its queue`() {
        assertEquals(
            LinkDestination.Page("linkedPlaylist/PLabcdef/$video", playsVideo = true),
            destinationOf("https://www.youtube.com/watch?v=$video&list=PLabcdef&index=3"),
        )
    }

    @Test
    fun `a video linked inside a mix or the account's own lists plays alone`() {
        assertEquals(LinkDestination.Video(video), destinationOf("https://www.youtube.com/watch?v=$video&list=RDMMabc"))
        assertEquals(LinkDestination.Video(video), destinationOf("https://www.youtube.com/watch?v=$video&list=WL"))
        assertEquals(LinkDestination.Video(video), destinationOf("https://www.youtube.com/watch?v=$video&list=LL"))
    }

    @Test
    fun `playlists open their page`() {
        assertEquals(page("playlist/PLabcdef"), destinationOf("https://www.youtube.com/playlist?list=PLabcdef"))
        assertEquals(page("playlist/PLabcdef"), destinationOf("https://music.youtube.com/playlist?list=PLabcdef"))
        assertEquals(page("playlist/OLAK5uy_abc"), destinationOf("https://www.youtube.com/playlist?list=OLAK5uy_abc"))
        assertEquals(page("playlist/RDCLAK5uy_abc"), destinationOf("https://music.youtube.com/playlist?list=RDCLAK5uy_abc"))
        assertEquals(page("playlist/MPREb_abc"), destinationOf("https://music.youtube.com/browse/MPREb_abc"))
    }

    @Test
    fun `the account's own lists open the library copies`() {
        assertEquals(page("playlist/liked_videos"), destinationOf("https://www.youtube.com/playlist?list=LL"))
        assertEquals(page("playlist/watch_later"), destinationOf("https://www.youtube.com/playlist?list=WL"))
        assertNull(destinationOf("https://music.youtube.com/playlist?list=LM"))
    }

    @Test
    fun `a mix on its own and a search have no page`() {
        assertNull(destinationOf("https://www.youtube.com/playlist?list=RDdQw4w9WgXcQ"))
        assertNull(destinationOf("https://www.youtube.com/watch?list=RDMMabc"))
        assertNull(destinationOf("https://www.youtube.com/results?search_query=cats"))
    }

    @Test
    fun `channels open their page`() {
        assertEquals(page("channel/$channel"), destinationOf("https://www.youtube.com/channel/$channel"))
        assertEquals(page("channel/$channel"), destinationOf("https://music.youtube.com/channel/$channel"))
    }
}
