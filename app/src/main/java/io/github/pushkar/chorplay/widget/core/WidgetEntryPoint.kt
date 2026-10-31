package io.github.pushkar.chorplay.widget.core

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.pushkar.chorplay.widget.core.refresh.WidgetContentSync
import io.github.pushkar.chorplay.widget.downloads.DownloadsSource

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun downloadsSource(): DownloadsSource

    fun widgetContentSync(): WidgetContentSync
}

fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
