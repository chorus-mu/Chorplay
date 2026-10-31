/*
 * Copyright: Pushkar (2026) under GPLv3
 *
 * This file is part of Chorplay (https://github.com/chorus-mu/Chorplay).
 */

package io.github.pushkar.chorplay.ui.components.music.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Mood buttons colour themselves by position, so a row of them never repeats a container until
 * all three have been used.
 */
class MusicMoodToneTest {
    @Test
    fun moodTonesCycleThroughTheThreeContainers() {
        assertThat(MusicMoodTone.forIndex(0)).isEqualTo(MusicMoodTone.Primary)
        assertThat(MusicMoodTone.forIndex(1)).isEqualTo(MusicMoodTone.Secondary)
        assertThat(MusicMoodTone.forIndex(2)).isEqualTo(MusicMoodTone.Tertiary)
        assertThat(MusicMoodTone.forIndex(3)).isEqualTo(MusicMoodTone.Primary)
    }
}
