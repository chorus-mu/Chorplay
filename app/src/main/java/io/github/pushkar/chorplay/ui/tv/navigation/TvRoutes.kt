package io.github.pushkar.chorplay.ui.tv.navigation

import android.net.Uri
import io.github.pushkar.chorplay.ui.components.settings.SettingsTarget
import io.github.pushkar.chorplay.ui.components.shared.MediaKind

object TvRoutes {
    const val CHANNEL_ARG = "channelRef"
    const val CHANNEL = "channel?ref={$CHANNEL_ARG}"

    const val SETTINGS_PAGE_ARG = "target"
    const val SETTINGS_PAGE = "settingsPage/{$SETTINGS_PAGE_ARG}"

    const val PLAYLIST_ARG = "playlistId"
    const val PLAYLIST = "playlist/{$PLAYLIST_ARG}"

    const val HISTORY = "history"

    const val DOWNLOADS = "downloads"

    const val PLAYLISTS_KIND_ARG = "kind"
    const val PLAYLISTS = "playlists?kind={$PLAYLISTS_KIND_ARG}"

    fun channel(channelRef: String): String = "channel?ref=${Uri.encode(channelRef)}"

    fun playlists(kind: MediaKind?): String = if (kind == null) "playlists" else "playlists?kind=${kind.name}"

    fun playlist(playlistId: String): String = "playlist/${Uri.encode(playlistId)}"

    fun settingsPage(target: SettingsTarget): String = "settingsPage/${Uri.encode(target.encode())}"
}
