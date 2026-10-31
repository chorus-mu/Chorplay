package io.github.pushkar.chorplay.widget.core.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.pushkar.chorplay.MainActivity

object WidgetDeepLink {
    const val EXTRA_WIDGET_ROUTE = "widget_route"
    const val ROUTE_SEARCH = "search"
    const val ROUTE_DOWNLOADS = "downloads"
    const val ROUTE_HISTORY = "history"
    const val ROUTE_SHORTS = "shorts"
    const val ROUTE_SUBSCRIPTIONS = "subscriptions"
    const val ROUTE_LIBRARY = "library"

    fun openApp(context: Context): Intent = base(context, "open")

    fun openRoute(
        context: Context,
        route: String,
    ): Intent = base(context, "route/$route").putExtra(EXTRA_WIDGET_ROUTE, route)

    fun playPlaylist(
        context: Context,
        playlistId: String,
        shuffle: Boolean,
        startVideoId: String? = null,
    ): Intent = openRoute(context, widgetPlaylistRoute(playlistId, shuffle, startVideoId))

    fun openPlaylist(context: Context, playlistId: String): Intent = openRoute(context, "playlist/$playlistId")

    fun playVideo(
        context: Context,
        videoId: String,
    ): Intent = base(context, "video/$videoId").putExtra("video_id", videoId)

    fun playShort(
        context: Context,
        videoId: String,
    ): Intent =
        base(context, "short/$videoId")
            .putExtra("video_id", videoId)
            .putExtra("is_short", true)

    private fun base(
        context: Context,
        path: String,
    ): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = "io.github.pushkar.chorplay.widget.OPEN"
            data = Uri.parse("chorplay://widget/$path")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
