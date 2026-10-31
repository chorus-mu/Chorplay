package io.github.pushkar.chorplay.widget.core.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.glance.color.ColorProviders
import io.github.pushkar.chorplay.data.local.MusicPlayerBackgroundStyle
import io.github.pushkar.chorplay.ui.components.shared.MediaPalette
import io.github.pushkar.chorplay.ui.components.shared.mediaPaletteOf
import io.github.pushkar.chorplay.widget.core.image.WidgetImageLoader

internal suspend fun artworkColorProviders(
    context: Context,
    artworkUrl: String?,
    style: MusicPlayerBackgroundStyle,
    appColors: ColorProviders,
): ColorProviders? {
    if (style == MusicPlayerBackgroundStyle.DEFAULT) return null
    val bitmap = WidgetImageLoader.load(context, artworkUrl, PALETTE_SAMPLE_PX) ?: return null
    val palette = mediaPaletteOf(bitmap, appColors.surface.getColor(context), appColors.primary.getColor(context))
    val scheme = paletteColorScheme(palette, appColors.error.getColor(context))
    return androidx.glance.material3.ColorProviders(scheme)
}

private fun paletteColorScheme(
    palette: MediaPalette,
    error: Color,
): ColorScheme {
    val surface = palette.base
    val onSurface = palette.onBase
    val primary = palette.accent
    val onPrimary = if (primary.luminance() < 0.45f) Color.White else Color(0xFF161616)
    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        surface = surface,
        onSurface = onSurface,
        background = surface,
        onBackground = onSurface,
        error = error,
    )
}

private const val PALETTE_SAMPLE_PX = 128
