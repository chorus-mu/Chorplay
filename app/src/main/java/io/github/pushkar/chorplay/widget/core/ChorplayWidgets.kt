package io.github.pushkar.chorplay.widget.core

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import io.github.pushkar.chorplay.BuildConfig
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.widget.core.theme.lastWidgetPreviewStamp
import io.github.pushkar.chorplay.widget.core.theme.lastWidgetThemeSignature
import io.github.pushkar.chorplay.widget.core.theme.widgetThemeSignatureFlow
import io.github.pushkar.chorplay.widget.core.theme.writeWidgetPreviewStamp
import io.github.pushkar.chorplay.widget.core.theme.writeWidgetThemeSignature
import io.github.pushkar.chorplay.widget.downloads.DownloadsWidget
import io.github.pushkar.chorplay.widget.downloads.DownloadsWidgetReceiver
import kotlin.reflect.KClass

/** Registry of every Chorplay widget: re-renders them on theme changes, publishes previews, lists them in Settings. */
object ChorplayWidgets {
    /**
     * Re-renders every placed widget whenever the app's palette actually changes.
     */
    suspend fun observeThemeChanges(context: Context) {
        val appContext = context.applicationContext
        var lastSignature = appContext.lastWidgetThemeSignature()
        widgetThemeSignatureFlow(appContext).collect { signature ->
            val persisted = signature.persistedForm()
            publishPreviewsIfStale(appContext, persisted)
            if (persisted == lastSignature) return@collect
            lastSignature = persisted
            appContext.writeWidgetThemeSignature(persisted)
            updateAll(appContext)
        }
    }

    suspend fun updateAll(
        context: Context,
        except: Set<KClass<out GlanceAppWidget>> = emptySet(),
    ) {
        catalog.map { it.widget() }.filterNot { it::class in except }.forEach { it.updateAll(context) }
    }

    private suspend fun publishPreviewsIfStale(
        context: Context,
        themeSignature: String,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val stamp = "${BuildConfig.VERSION_CODE}|$themeSignature"
        if (context.lastWidgetPreviewStamp() == stamp) return
        val manager = GlanceAppWidgetManager(context)
        val published =
            try {
                catalog.all { entry ->
                    manager.setWidgetPreviews(entry.receiver) == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS
                }
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Widget previews rejected: ${e.message}")
                false
            }
        Log.i(TAG, "Widget previews published: $published")
        if (published) context.writeWidgetPreviewStamp(stamp)
    }

    private const val TAG = "ChorplayWidgets"

    val catalog: List<ChorWidgetEntry> =
        listOf(
            ChorWidgetEntry(
                id = "downloads",
                receiver = DownloadsWidgetReceiver::class,
                widget = ::DownloadsWidget,
                label = R.string.widget_downloads_label,
                description = R.string.widget_downloads_description,
                icon = R.drawable.ic_widget_download,
            ),
        )
}

class ChorWidgetEntry(
    val id: String,
    val receiver: KClass<out GlanceAppWidgetReceiver>,
    val widget: () -> GlanceAppWidget,
    @StringRes val label: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
)
