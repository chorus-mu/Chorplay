package io.github.pushkar.chorplay.ui.screens.settings.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.SpeedSwipeSide
import io.github.pushkar.chorplay.notification.BackgroundWorkPolicy
import io.github.pushkar.chorplay.ui.components.settings.SettingsPage
import io.github.pushkar.chorplay.ui.components.settings.SettingsTarget

internal enum class PlaybackDialog {
    AUTOPLAY_COUNTDOWN,
    LONG_PRESS_SPEED,
    DOUBLE_TAP_SEEK,
    SEEK_ZONE_WIDTH,
    SPEED_SWIPE_SIDE,
    SPEED_SWIPE_STEP,
    AUDIO_LANGUAGE,
    SUBTITLE_LANGUAGE,
    SHORTS_MODE,
}

@Composable
internal fun PlaybackSettingsScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    onNavigate: (SettingsTarget) -> Unit,
    viewModel: PlaybackSettingsViewModel = hiltViewModel(),
) {
    val loopAll by viewModel.loopAll.collectAsStateWithLifecycle()
    val customSpeeds by viewModel.customSpeeds.collectAsStateWithLifecycle()
    val doubleTapSeek by viewModel.doubleTapSeek.collectAsStateWithLifecycle()
    val speedSwipeSide by viewModel.speedSwipeSide.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val shortsContent by viewModel.shortsContent.collectAsStateWithLifecycle()
    val disableShortsPlayer by viewModel.disableShortsPlayer.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<PlaybackDialog?>(null) }
    val context = LocalContext.current
    var backgroundAllowed by remember { mutableStateOf(BackgroundWorkPolicy.isBackgroundWorkUnrestricted(context)) }
    LifecycleResumeEffect(Unit) {
        backgroundAllowed = BackgroundWorkPolicy.isBackgroundWorkUnrestricted(context)
        onPauseOrDispose { }
    }

    SettingsPage(
        title = stringResource(R.string.settings_playback_title),
        onBack = onBack,
        highlight = highlight,
    ) {
        playbackSections(
            viewModel = viewModel,
            state =
                PlaybackStructure(
                    loopAll,
                    customSpeeds,
                    doubleTapSeek > 0,
                    speedSwipeSide,
                    comments,
                    shortsContent,
                    disableShortsPlayer,
                    backgroundRestricted = !backgroundAllowed,
                ),
            openDialog = { dialog = it },
            onNavigate = onNavigate,
            onAllowBackground = { BackgroundWorkPolicy.requestUnrestrictedBackgroundWork(context) },
        )
    }

    dialog?.let { open ->
        PlaybackDialogs(dialog = open, viewModel = viewModel, onDismiss = { dialog = null })
    }
}

internal data class PlaybackStructure(
    val loopAll: Boolean,
    val customSpeeds: Boolean,
    val doubleTapSeek: Boolean,
    val speedSwipeSide: SpeedSwipeSide,
    val comments: Boolean,
    val shortsContent: Boolean,
    val disableShortsPlayer: Boolean,
    val backgroundRestricted: Boolean,
)
