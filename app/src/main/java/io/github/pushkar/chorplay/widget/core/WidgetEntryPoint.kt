package io.github.pushkar.chorplay.widget.core

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.pushkar.chorplay.widget.core.refresh.WidgetContentSync
import io.github.pushkar.chorplay.widget.downloads.DownloadsSource
import io.github.pushkar.chorplay.widget.onrepeat.OnRepeatSource
import io.github.pushkar.chorplay.widget.playlist.PlaylistWidgetSource
import io.github.pushkar.chorplay.widget.recent.RecentlyPlayedSource
import io.github.pushkar.chorplay.widget.week.WeekSource

/**
 * Glance widgets can't use constructor injection (the framework instantiates them),
 * so Hilt singletons are reached through this entry point.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun recentlyPlayedSource(): RecentlyPlayedSource

    fun downloadsSource(): DownloadsSource

    fun onRepeatSource(): OnRepeatSource

    fun playlistWidgetSource(): PlaylistWidgetSource

    fun weekSource(): WeekSource

    fun widgetContentSync(): WidgetContentSync
}

fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
