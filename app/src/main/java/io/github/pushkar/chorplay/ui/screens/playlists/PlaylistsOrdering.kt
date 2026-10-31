package io.github.pushkar.chorplay.ui.screens.playlists

import io.github.pushkar.chorplay.data.model.PlaylistInfo
import io.github.pushkar.chorplay.data.playlist.PlaylistListOrder
import io.github.pushkar.chorplay.data.playlist.sortedFor
import io.github.pushkar.chorplay.ui.components.library.PlaylistOwnershipFilter

/**
 * Owned playlists lead saved ones in every order except [PlaylistListOrder.CUSTOM], where the
 * viewer's own arrangement decides.
 */
internal fun orderedPlaylists(
    owned: List<PlaylistInfo>,
    saved: List<PlaylistInfo>,
    filter: PlaylistOwnershipFilter,
    order: PlaylistListOrder,
): List<PlaylistInfo> =
    if (order == PlaylistListOrder.CUSTOM) {
        filter.select(owned, saved).sortedFor(order)
    } else {
        filter.select(owned.sortedFor(order), saved.sortedFor(order))
    }
