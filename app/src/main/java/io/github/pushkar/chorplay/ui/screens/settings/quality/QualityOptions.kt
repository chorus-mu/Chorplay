package io.github.pushkar.chorplay.ui.screens.settings.quality

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.pushkar.chorplay.R

import io.github.pushkar.chorplay.data.local.ThumbnailQuality
import io.github.pushkar.chorplay.data.local.VideoCodec
import io.github.pushkar.chorplay.data.local.VideoQuality

internal val VideoQualities =
    listOf(
        VideoQuality.AUTO,
        VideoQuality.Q_2160P,
        VideoQuality.Q_1440P,
        VideoQuality.Q_1080P,
        VideoQuality.Q_720P,
        VideoQuality.Q_480P,
        VideoQuality.Q_360P,
        VideoQuality.Q_240P,
        VideoQuality.Q_144P,
    )


internal val ThumbnailQualities = ThumbnailQuality.entries.toList()

@StringRes
internal fun thumbnailQualityLabel(quality: ThumbnailQuality): Int =
    when (quality) {
        ThumbnailQuality.HIGH -> R.string.thumbnail_quality_high
        ThumbnailQuality.MEDIUM -> R.string.thumbnail_quality_medium
        ThumbnailQuality.LOW -> R.string.thumbnail_quality_low
        ThumbnailQuality.OFF -> R.string.thumbnail_quality_off
    }

@StringRes
internal fun thumbnailQualityDescription(quality: ThumbnailQuality): Int =
    when (quality) {
        ThumbnailQuality.HIGH -> R.string.thumbnail_quality_high_desc
        ThumbnailQuality.MEDIUM -> R.string.thumbnail_quality_medium_desc
        ThumbnailQuality.LOW -> R.string.thumbnail_quality_low_desc
        ThumbnailQuality.OFF -> R.string.thumbnail_quality_off_desc
    }

@StringRes
internal fun videoQualityLabel(quality: VideoQuality): Int =
    when (quality) {
        VideoQuality.AUTO -> R.string.quality_auto
        VideoQuality.Q_144P -> R.string.quality_144p
        VideoQuality.Q_240P -> R.string.quality_240p
        VideoQuality.Q_360P -> R.string.quality_360p
        VideoQuality.Q_480P -> R.string.quality_480p
        VideoQuality.Q_720P -> R.string.quality_720p_hd
        VideoQuality.Q_1080P -> R.string.quality_1080p_full_hd
        VideoQuality.Q_1440P -> R.string.quality_1440p_qhd
        VideoQuality.Q_2160P -> R.string.quality_2160p_4k
    }

internal fun fallbackCodecs(preferred: VideoCodec): List<VideoCodec> = VideoCodec.entries.filter { it != preferred }

internal fun fallbackAfterPreferredChange(
    preferred: VideoCodec,
    fallback: VideoCodec,
): VideoCodec = if (fallback == preferred) VideoCodec.AUTO else fallback

@Composable
internal fun codecLabel(codec: VideoCodec): String =
    if (codec == VideoCodec.AUTO) stringResource(R.string.player_settings_video_codec_fallback_auto) else codec.label
