package io.github.pushkar.chorplay.ui.components.layout.navigation

import io.github.pushkar.chorplay.data.local.DEFAULT_NAV_TAB_ORDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChorplayTabTest {
    @Test
    fun hiddenHomeFallsBackToFirstVisibleDestination() {
        val visibility = NavigationVisibility(home = false, shorts = false)

        val resolved =
            resolveDefaultChorplayTab(
                preferredId = ChorplayTab.Home.id,
                order = listOf(0, 4, 3, 1, 5, 6, 7),
                visibility = visibility,
            )

        assertEquals(ChorplayTab.Library, resolved)
        assertFalse(visibleChorplayTabs(listOf(0, 4, 3), visibility).contains(ChorplayTab.Home))
    }

    @Test
    fun reEnabledHomeRestoresAHomeDefault() {
        val resolved =
            resolveDefaultChorplayTab(
                preferredId = ChorplayTab.Home.id,
                order = listOf(3, 0, 4),
                visibility = NavigationVisibility(home = true),
            )

        assertEquals(ChorplayTab.Home, resolved)
    }

    @Test
    fun tabsFollowTheSavedOrder() {
        val tabs =
            visibleChorplayTabs(
                order = listOf(4, 3, 1, 0),
                visibility = NavigationVisibility(),
            )

        assertEquals(
            listOf(ChorplayTab.Library, ChorplayTab.Subscriptions, ChorplayTab.Shorts, ChorplayTab.Home),
            tabs,
        )
    }

    @Test
    fun aSavedOrderMissingNewerTabsAppendsThemInDefaultOrder() {
        val tabs =
            visibleChorplayTabs(
                order = listOf(3, 0),
                visibility = NavigationVisibility(search = true, categories = true),
            )

        assertEquals(
            listOf(
                ChorplayTab.Subscriptions,
                ChorplayTab.Home,
                ChorplayTab.Shorts,
                ChorplayTab.Library,
                ChorplayTab.Search,
                ChorplayTab.Explore,
            ),
            tabs,
        )
    }

    @Test
    fun unknownPersistedIdsAreIgnored() {
        val tabs = visibleChorplayTabs(order = listOf(42, 4, -1, 3), visibility = NavigationVisibility())

        assertEquals(ChorplayTab.Library, tabs.first())
        assertEquals(tabs.distinct(), tabs)
    }

    @Test
    fun persistedIdsNeverChange() {
        assertEquals(DEFAULT_NAV_TAB_ORDER, ChorplayTab.entries.map(ChorplayTab::id))
        ChorplayTab.entries.forEach { tab -> assertEquals(tab, ChorplayTab.fromId(tab.id)) }
    }

    /**
     * Settings and Notifications live in the top bar of every root destination, which only works if
     * a root destination always exists. Subscriptions and Library are unconditional in
     * [visibleChorplayTabs]; this pins that down so hiding tabs can never orphan those screens.
     */
    @Test
    fun everyVisibilityCombinationKeepsAnUnhideableRootDestination() {
        val orders =
            listOf(
                DEFAULT_NAV_TAB_ORDER,
                listOf(6, 5, 4, 3, 1, 0),
                listOf(4, 3),
                emptyList(),
            )

        for (bits in 0 until 16) {
            val visibility =
                NavigationVisibility(
                    home = bits and 1 != 0,
                    shorts = bits and 2 != 0,
                    search = bits and 4 != 0,
                    categories = bits and 8 != 0,
                )

            for (order in orders) {
                val visible = visibleChorplayTabs(order, visibility)
                assertTrue(
                    "no unhideable root destination for $visibility / $order",
                    visible.contains(ChorplayTab.Subscriptions) || visible.contains(ChorplayTab.Library),
                )

                val resolved = resolveDefaultChorplayTab(ChorplayTab.Home.id, order, visibility)
                assertTrue(
                    "resolved default $resolved is not visible for $visibility / $order",
                    visible.contains(resolved),
                )
            }
        }
    }

    @Test
    fun theDownloadsTabIsOffUntilTurnedOn() {
        assertFalse(ChorplayTab.Downloads in visibleChorplayTabs(DEFAULT_NAV_TAB_ORDER, NavigationVisibility()))
        assertEquals(
            ChorplayTab.Downloads,
            visibleChorplayTabs(listOf(3, 0), NavigationVisibility(downloads = true)).last(),
        )
    }
}
