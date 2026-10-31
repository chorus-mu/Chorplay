package io.github.pushkar.chorplay.ui

import io.github.pushkar.chorplay.ui.components.layout.navigation.ChorplayTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDestinationsTest {
    @Test
    fun tabRootsMapToTheirTab() {
        ChorplayTab.entries.filter { it != ChorplayTab.Shorts }.forEach { tab ->
            assertEquals(tab, chorTabForDestination(tab.route, shortsSourceArg = null))
        }
    }

    @Test
    fun onlyTheShortsFeedIsTheShortsTab() {
        assertEquals(ChorplayTab.Shorts, chorTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = null))
        assertEquals(ChorplayTab.Shorts, chorTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "feed"))
        assertNull(chorTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "feed:dQw4w9WgXcQ"))
        assertNull(chorTabForDestination(SHORTS_ROUTE_PATTERN, shortsSourceArg = "saved:"))
    }

    @Test
    fun aVideoOpenedFromShortsPlaysOverTheFirstOtherTab() {
        assertEquals("music", shortsExitRoute(listOf(ChorplayTab.Shorts, ChorplayTab.Music, ChorplayTab.Library)))
        assertEquals("home", shortsExitRoute(listOf(ChorplayTab.Home, ChorplayTab.Shorts)))
        assertEquals("home", shortsExitRoute(listOf(ChorplayTab.Shorts)))
        assertEquals("home", shortsExitRoute(emptyList()))
    }

    @Test
    fun detailScreensAreNotTabRoots() {
        listOf("settings", "settings/content", "playlists", "playlist/{playlistId}", "onboarding", null).forEach { route ->
            assertNull(chorTabForDestination(route, shortsSourceArg = null))
        }
    }

    @Test
    fun routesFromOutsideTheGraphResolveToTheirTab() {
        assertEquals(ChorplayTab.Search, chorTabForRoute("search", downloadsTab = false))
        assertEquals(ChorplayTab.Music, chorTabForRoute("music", downloadsTab = false))
        listOf("downloads", "history", "musicRecognize", "musicPlayer/abc", "settings").forEach { route ->
            assertNull(chorTabForRoute(route, downloadsTab = false))
        }
    }

    @Test
    fun theDownloadsPageOpensAsTheTabOnlyWhenTheBarHasIt() {
        assertEquals(ChorplayTab.Downloads, chorTabForRoute("downloads", downloadsTab = true))
        assertNull(chorTabForRoute("downloads", downloadsTab = false))
        assertNull(chorTabForDestination("downloads", shortsSourceArg = null))
    }

    @Test
    fun searchIsTheOnlyTabWithoutTheBar() {
        ChorplayTab.entries.forEach { tab -> assertEquals(tab != ChorplayTab.Search, tab.showsNavigationBar()) }
        assertFalse((null as ChorplayTab?).showsNavigationBar())
        assertTrue(ChorplayTab.Home.showsNavigationBar())
    }

    @Test
    fun onlyChannelIdsAreBrowsedDirectly() {
        assertEquals("UCXuqSBlHAE6Xw-yeJA0Tunw", youtubeChannelBrowseId("UCXuqSBlHAE6Xw-yeJA0Tunw"))
        assertEquals("UCXuqSBlHAE6Xw-yeJA0Tunw", youtubeChannelBrowseId("https://m.youtube.com/channel/UCXuqSBlHAE6Xw-yeJA0Tunw?si=abc"))
        assertNull(youtubeChannelBrowseId("@LinusTechTips"))
        assertNull(youtubeChannelBrowseId("https://www.youtube.com/@LinusTechTips"))
        assertNull(youtubeChannelBrowseId("https://www.youtube.com/c/LinusTechTips"))
        assertNull(youtubeChannelBrowseId(" "))
    }

    @Test
    fun channelHandlesUseHandleUrls() {
        assertEquals("https://www.youtube.com/@chorplay", youtubeChannelUrl("@chorplay"))
        assertEquals("https://www.youtube.com/@chorplay", youtubeChannelUrl("chorplay"))
        assertEquals(
            "https://www.youtube.com/channel/UC123",
            youtubeChannelUrl("UC123"),
        )
    }

    @Test
    fun malformedHandleChannelUrlsAreRepaired() {
        assertEquals(
            "https://www.youtube.com/@chorplay",
            youtubeChannelUrl("https://youtube.com/channel/@chorplay"),
        )
        assertEquals(
            "https://www.youtube.com/@chorplay",
            youtubeChannelUrl("https://m.youtube.com/@chorplay/videos"),
        )
    }

    @Test
    fun channelRoutesEncodeCanonicalUrls() {
        assertEquals(
            "channel?url=https%3A%2F%2Fwww.youtube.com%2F%40chorplay",
            youtubeChannelRoute("@chorplay"),
        )
    }
}
