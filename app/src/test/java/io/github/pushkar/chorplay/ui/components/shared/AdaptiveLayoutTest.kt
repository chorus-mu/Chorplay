/*
 * Copyright: Pushkar (2026) under GPLv3
 *
 * This file is part of Chorplay (https://github.com/chorus-mu/Chorplay).
 */

package io.github.pushkar.chorplay.ui.components.shared

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Every adaptive surface sizes itself from the window, so a tablet never gets a phone layout
 * blown up to twice its size. These pin the breakpoints and the width maths those surfaces share.
 */
class AdaptiveLayoutTest {
    @Test
    fun laneItemsFillAPhoneWithTheNextItemPeeking() {
        val width = chorLaneItemWidthFor(windowWidth = 360.dp, maxWidth = 360.dp, peek = 48.dp)
        assertThat(width).isEqualTo(360.dp - 24.dp - 48.dp)
    }

    @Test
    fun laneItemsStopGrowingOnWideWindows() {
        val width = chorLaneItemWidthFor(windowWidth = 1200.dp, maxWidth = 360.dp, peek = 48.dp)
        assertThat(width).isEqualTo(360.dp)
    }

    @Test
    fun laneItemsNeverCollapseInANarrowWindow() {
        val width = chorLaneItemWidthFor(windowWidth = 240.dp, maxWidth = 360.dp, peek = 48.dp)
        assertThat(width).isEqualTo(240.dp)
    }

    @Test
    fun gridCellsShareTheWindowMinusPaddingAndGaps() {
        val width = chorGridCellWidthFor(windowWidth = 400.dp, columns = 2, gap = 8.dp)
        assertThat(width).isEqualTo((400.dp - 24.dp - 8.dp) / 2)
    }

    @Test
    fun heroArtworkIsCappedOnTablets() {
        assertThat(chorHeroArtworkSizeFor(360.dp)).isEqualTo(198.dp)
        assertThat(chorHeroArtworkSizeFor(1200.dp)).isEqualTo(240.dp)
        assertThat(chorHeroArtworkSizeFor(300.dp)).isEqualTo(180.dp)
    }
}
