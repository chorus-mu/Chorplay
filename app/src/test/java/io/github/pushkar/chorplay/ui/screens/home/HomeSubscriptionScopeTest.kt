/*
 * Copyright: Pushkar (2026) under GPLv3
 *
 * This file is part of Chorplay (https://github.com/chorus-mu/Chorplay).
 */

package io.github.pushkar.chorplay.ui.screens.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeSubscriptionScopeTest {
    private val subs = setOf("UCa", "UCb")

    @Test
    fun `with subscriptions on Home they are boosted and nothing is hidden`() {
        val scope = HomeSubscriptionScope.of(subs, showOnHome = true)

        assertThat(scope.boosted).isEqualTo(subs)
        assertThat(scope.hidden).isEmpty()
    }

    @Test
    fun `with subscriptions off Home hides them and boosts nothing`() {
        val scope = HomeSubscriptionScope.of(subs, showOnHome = false)

        assertThat(scope.boosted).isEmpty()
        assertThat(scope.hidden).isEqualTo(subs)
    }
}
