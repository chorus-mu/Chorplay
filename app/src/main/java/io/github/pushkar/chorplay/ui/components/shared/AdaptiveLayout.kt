/*
 * Copyright: Pushkar (2026) under GPLv3
 *
 * This file is part of Chorplay (https://github.com/chorus-mu/Chorplay).
 */

package io.github.pushkar.chorplay.ui.components.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pushkar.chorplay.ui.theme.Dimensions
import io.github.pushkar.chorplay.ui.utils.LocalWindowSizeClass
import io.github.pushkar.chorplay.ui.utils.isExpandedWidth
import io.github.pushkar.chorplay.ui.utils.isMediumWidth

/** Widest a single column of rows grows, so it stays readable on a tablet or an unfolded foldable. */
val ChorMaxContentWidth: Dp = 720.dp

private val MinLaneItemWidth = 240.dp
private val MinHeroArtworkSize = 180.dp
private val MaxHeroArtworkSize = 240.dp
private const val HERO_ARTWORK_FRACTION = 0.55f

/**
 * Width available to an adaptive section, measured from the window rather than the display so
 * split-screen and freeform windows lay out like the narrow devices they behave as.
 */
@Composable
fun chorWindowWidth(): Dp {
    val density = LocalDensity.current
    val width = LocalWindowInfo.current.containerSize.width
    return with(density) { width.toDp() }
}

/**
 * Width of one card in a horizontally scrolling lane of wide items: fills a phone with [peek] of
 * the next card showing, and stops growing at [maxWidth] on wider windows.
 */
@Composable
fun chorLaneItemWidth(
    maxWidth: Dp,
    peek: Dp = 40.dp,
): Dp = chorLaneItemWidthFor(chorWindowWidth(), maxWidth, peek)

fun chorLaneItemWidthFor(
    windowWidth: Dp,
    maxWidth: Dp,
    peek: Dp,
): Dp = (windowWidth - Dimensions.ContentPaddingHorizontal * 2 - peek).coerceIn(MinLaneItemWidth, maxWidth)

@Composable
fun chorGridColumns(
    compact: Int,
    medium: Int,
    expanded: Int,
): Int {
    val windowSizeClass = LocalWindowSizeClass.current
    return when {
        windowSizeClass.isExpandedWidth -> expanded
        windowSizeClass.isMediumWidth -> medium
        else -> compact
    }
}

@Composable
fun chorGridCellWidth(
    columns: Int,
    gap: Dp = Dimensions.ItemSpacing,
): Dp = chorGridCellWidthFor(chorWindowWidth(), columns, gap)

fun chorGridCellWidthFor(
    windowWidth: Dp,
    columns: Int,
    gap: Dp,
): Dp = (windowWidth - Dimensions.ContentPaddingHorizontal * 2 - gap * (columns - 1)) / columns

/**
 * Artwork size for a collection or artist header: over half a phone, capped on wider windows so a
 * tablet header stays a header rather than a poster.
 */
@Composable
fun chorHeroArtworkSize(): Dp = chorHeroArtworkSizeFor(chorWindowWidth())

fun chorHeroArtworkSizeFor(windowWidth: Dp): Dp = (windowWidth * HERO_ARTWORK_FRACTION).coerceIn(MinHeroArtworkSize, MaxHeroArtworkSize)
