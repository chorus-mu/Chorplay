package io.github.pushkar.chorplay.ui.screens.library

import com.google.common.truth.Truth.assertThat
import io.github.pushkar.chorplay.ui.components.library.LibrarySection
import org.junit.Test

class LibrarySectionVisibilityTest {
    @Test
    fun `nothing hidden shows every content section in order`() {
        assertThat(visibleContentSections(emptySet(), separatePlaylistKinds = false, shortsEnabled = true))
            .containsExactly(
                LibrarySection.HISTORY,
                LibrarySection.PLAYLISTS,
                LibrarySection.WATCH_LATER,
                LibrarySection.LIKED_VIDEOS,
                LibrarySection.DOWNLOADS,
                LibrarySection.SAVED_SHORTS,
            ).inOrder()
    }

    @Test
    fun `a hidden section is left out`() {
        assertThat(visibleContentSections(setOf(LibrarySection.LIKED_VIDEOS), separatePlaylistKinds = false, shortsEnabled = true))
            .doesNotContain(LibrarySection.LIKED_VIDEOS)
    }

    @Test
    fun `playlists section can be hidden`() {
        val visible =
            visibleContentSections(setOf(LibrarySection.PLAYLISTS), separatePlaylistKinds = false, shortsEnabled = true)

        assertThat(visible).doesNotContain(LibrarySection.PLAYLISTS)
    }

    @Test
    fun `saved shorts follows the shorts content switch`() {
        assertThat(visibleContentSections(emptySet(), separatePlaylistKinds = false, shortsEnabled = false))
            .doesNotContain(LibrarySection.SAVED_SHORTS)
    }

    @Test
    fun `notes follows the notes switch and every settings row can be hidden`() {
        assertThat(visibleSettingsSections(emptySet(), notesEnabled = false))
            .containsExactly(LibrarySection.LOCAL_MEDIA, LibrarySection.SETTINGS)
            .inOrder()
        assertThat(visibleSettingsSections(LibrarySection.settingsAndData.toSet(), notesEnabled = true)).isEmpty()
    }

    @Test
    fun `stored names that no longer exist are ignored`() {
        assertThat(LibrarySection.fromStored(setOf("LIKED_VIDEOS", "GONE"))).containsExactly(LibrarySection.LIKED_VIDEOS)
    }
}
