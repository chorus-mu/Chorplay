/*
 * Copyright: Pushkar (2026) under GPLv3
 *
 * This file is part of Chorplay (https://github.com/chorus-mu/Chorplay).
 */

package io.github.pushkar.chorplay.ui.components.music.section

import com.google.common.truth.Truth.assertThat
import io.github.pushkar.chorplay.data.music.model.MusicItemType
import io.github.pushkar.chorplay.data.music.model.MusicTrack
import org.junit.Test

/**
 * InnerTube returns albums, playlists and artists inside track lanes. Tapping one must open the
 * collection or artist rather than start playback — a rule that used to be written out at three
 * separate call sites on the home feed and now lives once in the shelf.
 */
class MusicShelfRoutingTest {
    private fun entry(type: MusicItemType) =
        MusicTrack(
            videoId = "id",
            title = "Title",
            artist = "Artist",
            thumbnailUrl = "",
            duration = 0,
            itemType = type,
        )

    @Test
    fun albumsPlaylistsAndArtistsRouteToTheCollectionHandler() {
        assertThat(entry(MusicItemType.ALBUM).isCollection).isTrue()
        assertThat(entry(MusicItemType.PLAYLIST).isCollection).isTrue()
        assertThat(entry(MusicItemType.ARTIST).isCollection).isTrue()
    }

    @Test
    fun onlySongsStartPlayback() {
        assertThat(entry(MusicItemType.SONG).isCollection).isFalse()
    }

    @Test
    fun theDefaultItemTypeIsATrack() {
        val plain =
            MusicTrack(
                videoId = "id",
                title = "Title",
                artist = "Artist",
                thumbnailUrl = "",
                duration = 0,
            )
        assertThat(plain.isCollection).isFalse()
    }
}
