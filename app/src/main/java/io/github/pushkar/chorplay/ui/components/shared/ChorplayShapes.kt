/*
 * Copyright: Pushkar (2026) under GPLv3
 *
 * This file is part of Chorplay (https://github.com/chorus-mu/Chorplay).
 */

package io.github.pushkar.chorplay.ui.components.shared

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape

/**
 * Every artist portrait wears this shape, so an artist is recognisable as
 * one before the label is read. Albums and playlists keep the theme's rounded rectangles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun chorplayArtistShape(): Shape = MaterialShapes.Cookie9Sided.toShape()

/**
 * The shape behind a shuffle or radio action, distinct from both artists and collections.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun chorplayActionShape(): Shape = MaterialShapes.Cookie12Sided.toShape()
