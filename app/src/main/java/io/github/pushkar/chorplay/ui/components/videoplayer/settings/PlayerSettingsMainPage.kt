package io.github.pushkar.chorplay.ui.components.videoplayer.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.player.EnhancedPlayerState
import io.github.pushkar.chorplay.ui.components.equalizer.equalizerSummary
import io.github.pushkar.chorplay.ui.components.shared.ChorNavRow
import io.github.pushkar.chorplay.ui.components.shared.ChorRowGroup
import io.github.pushkar.chorplay.ui.components.shared.ChorSectionHeader
import io.github.pushkar.chorplay.ui.components.shared.ChorplaySwitchRow
import io.github.pushkar.chorplay.ui.components.shared.chorRowGroupShape
import io.github.pushkar.chorplay.ui.components.shared.playbackSpeedLabel
import io.github.pushkar.chorplay.utils.formatDurationMillis

private const val VIDEO_ROWS = 1
private const val PLAYBACK_ROWS = 3
private const val AUDIO_ROWS = 1
private const val CAPTION_ROWS = 2
private const val OVERLAY_ROWS = 3
private const val EFFECT_ROWS = 3
private const val DISPLAY_ROWS = 1

@Composable
internal fun PlayerSettingsMainPage(
    playerState: EnhancedPlayerState,
    autoplayEnabled: Boolean,
    subtitlesEnabled: Boolean,
    ambientModeEnabled: Boolean,
    onNavigateToPage: (PlayerSettingsPage) -> Unit,
    onShowSubtitleStyle: () -> Unit,
    onCastClick: () -> Unit,
    onPipClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onAutoplayToggle: (Boolean) -> Unit,
    onSkipSilenceToggle: (Boolean) -> Unit,
    onVolumeNormalizationToggle: (Boolean) -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
    sponsorBlockSegmentCount: Int = 0,
    sponsorBlockOffForVideo: Boolean = false,
    onSponsorBlockToggle: (off: Boolean) -> Unit = {},
    notePositionMs: Long? = null,
    onAddNote: () -> Unit = {},
) {
    val playbackRows = if (sponsorBlockSegmentCount > 0) PLAYBACK_ROWS + 1 else PLAYBACK_ROWS
    ChorSectionHeader(stringResource(R.string.video))
    ChorRowGroup {
        ChorNavRow(
            leadingIcon = Icons.Filled.HighQuality,
            title = stringResource(R.string.quality),
            trailingText =
                if (playerState.currentQuality == 0) {
                    stringResource(R.string.quality_auto)
                } else {
                    "${playerState.currentQuality}p"
                },
            shape = chorRowGroupShape(0, VIDEO_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Quality) },
        )
    }

    ChorSectionHeader(stringResource(R.string.playback_header))
    ChorRowGroup {
        ChorNavRow(
            leadingIcon = Icons.Filled.Speed,
            title = stringResource(R.string.playback_speed),
            trailingText = playbackSpeedLabel(playerState.playbackSpeed),
            shape = chorRowGroupShape(0, playbackRows),
            onClick = { onNavigateToPage(PlayerSettingsPage.Speed) },
        )
        ChorplaySwitchRow(
            leadingIcon = Icons.Rounded.Repeat,
            title = stringResource(R.string.loop_video),
            checked = playerState.isLooping,
            shape = chorRowGroupShape(1, playbackRows),
            onCheckedChange = onLoopToggle,
        )
        ChorplaySwitchRow(
            leadingIcon = Icons.Filled.SkipNext,
            title = stringResource(R.string.autoplay_next),
            checked = autoplayEnabled,
            enabled = !playerState.isLooping,
            shape = chorRowGroupShape(2, playbackRows),
            onCheckedChange = onAutoplayToggle,
        )
        if (sponsorBlockSegmentCount > 0) {
            ChorplaySwitchRow(
                leadingIcon = ImageVector.vectorResource(R.drawable.ic_block),
                title = stringResource(R.string.player_settings_sponsorblock_video),
                supportingText =
                    pluralStringResource(R.plurals.sb_video_segments, sponsorBlockSegmentCount, sponsorBlockSegmentCount),
                checked = !sponsorBlockOffForVideo,
                shape = chorRowGroupShape(3, playbackRows),
                onCheckedChange = { on -> onSponsorBlockToggle(!on) },
            )
        }
    }

    if (notePositionMs != null) {
        ChorSectionHeader(stringResource(R.string.note_title))
        ChorRowGroup {
            ChorNavRow(
                leadingIcon = Icons.Outlined.MoreTime,
                title = stringResource(R.string.note_at_time, formatDurationMillis(notePositionMs)),
                shape = chorRowGroupShape(0, 1),
                onClick = onAddNote,
            )
        }
    }

    ChorSectionHeader(stringResource(R.string.audio_settings_title))
    ChorRowGroup {
        ChorNavRow(
            leadingIcon = Icons.Filled.AudioFile,
            title = stringResource(R.string.audio_track),
            trailingText =
                audioTrackDisplayLabel(
                    playerState.availableAudioTracks.getOrNull(playerState.currentAudioTrack),
                    playerState.currentAudioTrack,
                ),
            shape = chorRowGroupShape(0, AUDIO_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Audio) },
        )
    }

    ChorSectionHeader(stringResource(R.string.captions))
    ChorRowGroup {
        ChorNavRow(
            leadingIcon = Icons.Filled.Subtitles,
            title = stringResource(R.string.filter_subtitles),
            trailingText =
                if (subtitlesEnabled) stringResource(R.string.on) else stringResource(R.string.off),
            shape = chorRowGroupShape(0, CAPTION_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Subtitles) },
        )
        ChorNavRow(
            leadingIcon = Icons.Filled.Tune,
            title = stringResource(R.string.subtitle_style),
            shape = chorRowGroupShape(1, CAPTION_ROWS),
            onClick = onShowSubtitleStyle,
        )
    }

    ChorSectionHeader(stringResource(R.string.player_settings_overlay_controls))
    ChorRowGroup {
        ChorNavRow(
            leadingIcon = Icons.Filled.Cast,
            title = stringResource(R.string.cast_to_tv),
            shape = chorRowGroupShape(0, OVERLAY_ROWS),
            onClick = onCastClick,
        )
        ChorNavRow(
            leadingIcon = Icons.Filled.PictureInPicture,
            title = stringResource(R.string.pip_mode),
            shape = chorRowGroupShape(1, OVERLAY_ROWS),
            onClick = onPipClick,
        )
        ChorNavRow(
            leadingIcon = Icons.Filled.Bedtime,
            title = stringResource(R.string.sleep_timer),
            shape = chorRowGroupShape(2, OVERLAY_ROWS),
            onClick = onSleepTimerClick,
        )
    }

    ChorSectionHeader(stringResource(R.string.audio_effects))
    ChorRowGroup {
        ChorNavRow(
            leadingIcon = Icons.Filled.Equalizer,
            title = stringResource(R.string.equalizer),
            trailingText = equalizerSummary(),
            shape = chorRowGroupShape(0, EFFECT_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Equalizer) },
        )
        ChorplaySwitchRow(
            leadingIcon = Icons.Rounded.GraphicEq,
            title = stringResource(R.string.player_settings_skip_silence),
            checked = playerState.isSkipSilenceEnabled,
            shape = chorRowGroupShape(1, EFFECT_ROWS),
            onCheckedChange = onSkipSilenceToggle,
        )
        ChorplaySwitchRow(
            leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.player_settings_normalize_volume),
            checked = playerState.isVolumeNormalizationEnabled,
            shape = chorRowGroupShape(2, EFFECT_ROWS),
            onCheckedChange = onVolumeNormalizationToggle,
        )
    }

    ChorSectionHeader(stringResource(R.string.player_settings_display))
    ChorRowGroup {
        ChorplaySwitchRow(
            leadingIcon = ImageVector.vectorResource(R.drawable.ic_ambient_mode),
            title = stringResource(R.string.player_settings_ambient_mode),
            checked = ambientModeEnabled,
            shape = chorRowGroupShape(0, DISPLAY_ROWS),
            onCheckedChange = onAmbientModeToggle,
        )
    }
}
