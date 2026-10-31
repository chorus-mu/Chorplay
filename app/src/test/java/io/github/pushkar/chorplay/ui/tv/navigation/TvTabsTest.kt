package io.github.pushkar.chorplay.ui.tv.navigation

import com.google.common.truth.Truth.assertThat
import io.github.pushkar.chorplay.ui.components.layout.navigation.ChorplayTab
import io.github.pushkar.chorplay.ui.components.layout.navigation.NavigationVisibility
import org.junit.Test

class TvTabsTest {
    private val allOn = NavigationVisibility(home = true, shorts = true, music = true, search = true, categories = true)

    @Test
    fun `follows the user's order and leaves Shorts and Explore off`() {
        val order = listOf(ChorplayTab.Library, ChorplayTab.Shorts, ChorplayTab.Music, ChorplayTab.Explore, ChorplayTab.Home).map(ChorplayTab::id)

        val tabs = tvNavigation(order, defaultTabId = ChorplayTab.Home.id, visibility = allOn).tabs

        assertThat(tabs.take(3)).containsExactly(ChorplayTab.Library, ChorplayTab.Music, ChorplayTab.Home).inOrder()
        assertThat(tabs).containsNoneOf(ChorplayTab.Shorts, ChorplayTab.Explore)
    }

    @Test
    fun `always shows Search, which TV has nowhere else to put`() {
        val tabs = tvNavigation(emptyList(), ChorplayTab.Home.id, NavigationVisibility(search = false)).tabs

        assertThat(tabs).contains(ChorplayTab.Search)
    }

    @Test
    fun `hides the tabs the user hid`() {
        val tabs = tvNavigation(emptyList(), ChorplayTab.Home.id, NavigationVisibility(home = false, music = false)).tabs

        assertThat(tabs).containsNoneOf(ChorplayTab.Home, ChorplayTab.Music)
    }

    @Test
    fun `opens on the user's default tab, or the first tab when TV leaves it off`() {
        assertThat(tvNavigation(emptyList(), ChorplayTab.Library.id, allOn).startTab).isEqualTo(ChorplayTab.Library)

        val fromShorts = tvNavigation(emptyList(), ChorplayTab.Shorts.id, allOn)
        assertThat(fromShorts.startTab).isEqualTo(fromShorts.tabs.first())
    }

    @Test
    fun `roots are the tabs plus Settings`() {
        val navigation = tvNavigation(emptyList(), ChorplayTab.Home.id, allOn)

        assertThat(navigation.rootRoutes).containsExactlyElementsIn(navigation.tabs.map(ChorplayTab::route) + TV_SETTINGS_ROUTE)
    }
}
