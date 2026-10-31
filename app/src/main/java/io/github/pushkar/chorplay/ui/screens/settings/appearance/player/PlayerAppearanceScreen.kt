package io.github.pushkar.chorplay.ui.screens.settings.appearance.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.DEFAULT_FULLSCREEN_SEEKBAR_PADDING_DP
import io.github.pushkar.chorplay.data.local.DEFAULT_PORTRAIT_SEEKBAR_PADDING_DP
import io.github.pushkar.chorplay.data.local.GestureOverlayStyle
import io.github.pushkar.chorplay.data.local.MAX_FULLSCREEN_SEEKBAR_PADDING_DP
import io.github.pushkar.chorplay.data.local.MAX_PORTRAIT_SEEKBAR_PADDING_DP
import io.github.pushkar.chorplay.data.local.MiniBarSwipeAction
import io.github.pushkar.chorplay.data.local.ScrubPreviewStyle
import io.github.pushkar.chorplay.data.local.SeekbarPaddingMode
import io.github.pushkar.chorplay.data.local.ShortsPlayerUiMode
import io.github.pushkar.chorplay.data.local.SliderStyle
import io.github.pushkar.chorplay.data.local.resolveSeekbarHorizontalPaddingDp
import io.github.pushkar.chorplay.ui.components.settings.SettingEntry
import io.github.pushkar.chorplay.ui.components.settings.SettingsGroupScope
import io.github.pushkar.chorplay.ui.components.settings.SettingsPage
import io.github.pushkar.chorplay.ui.components.settings.SettingsPreviewSheet
import io.github.pushkar.chorplay.ui.components.settings.nav
import io.github.pushkar.chorplay.ui.components.settings.slider
import io.github.pushkar.chorplay.ui.components.settings.switch
import io.github.pushkar.chorplay.ui.components.settings.toggleGroup
import io.github.pushkar.chorplay.ui.components.shared.ChorToggleOption
import io.github.pushkar.chorplay.ui.components.shared.labelRes
import io.github.pushkar.chorplay.ui.screens.settings.index.PlayerAppearanceIndex
import kotlin.math.roundToInt

private const val SEEKBAR_STEP_DP = 4
private val PortraitSeekbarModes =
    listOf(SeekbarPaddingMode.SPACED, SeekbarPaddingMode.FULL_WIDTH, SeekbarPaddingMode.CUSTOM)
private val FullscreenSeekbarModes =
    listOf(SeekbarPaddingMode.FULL_WIDTH, SeekbarPaddingMode.DEFAULT, SeekbarPaddingMode.CUSTOM)

private enum class PreviewSheet { SLIDER, GESTURE, SWIPE_LEFT, SWIPE_RIGHT }

@Composable
internal fun PlayerAppearanceScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    viewModel: PlayerAppearanceViewModel = hiltViewModel(),
) {
    val sliderStyle by viewModel.sliderStyle.collectAsStateWithLifecycle()
    val portraitMode by viewModel.portraitSeekbarMode.collectAsStateWithLifecycle()
    val portraitCustom by viewModel.portraitSeekbarCustom.collectAsStateWithLifecycle()
    val fullscreenMode by viewModel.fullscreenSeekbarMode.collectAsStateWithLifecycle()
    val fullscreenCustom by viewModel.fullscreenSeekbarCustom.collectAsStateWithLifecycle()
    val scrubPreview by viewModel.scrubPreview.collectAsStateWithLifecycle()
    val frameStep by viewModel.frameStepButtons.collectAsStateWithLifecycle()
    val adaptiveSize by viewModel.adaptivePlayerSize.collectAsStateWithLifecycle()
    val ambientMode by viewModel.ambientMode.collectAsStateWithLifecycle()
    val groupedQuality by viewModel.groupedQualitySelector.collectAsStateWithLifecycle()
    val gestureOverlay by viewModel.gestureOverlay.collectAsStateWithLifecycle()
    val shortsUi by viewModel.shortsUiMode.collectAsStateWithLifecycle()
    val miniPlayerScale by viewModel.miniPlayerScale.collectAsStateWithLifecycle()
    val miniPlayerSkip by viewModel.miniPlayerSkip.collectAsStateWithLifecycle()
    val miniPlayerNextPrev by viewModel.miniPlayerNextPrev.collectAsStateWithLifecycle()
    val miniBarSwipeLeft by viewModel.miniBarSwipeLeft.collectAsStateWithLifecycle()
    val miniBarSwipeRight by viewModel.miniBarSwipeRight.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf<PreviewSheet?>(null) }

    val portraitPadding =
        resolveSeekbarHorizontalPaddingDp(
            portraitMode,
            portraitCustom,
            DEFAULT_PORTRAIT_SEEKBAR_PADDING_DP,
            MAX_PORTRAIT_SEEKBAR_PADDING_DP,
        )
    val fullscreenPadding =
        resolveSeekbarHorizontalPaddingDp(
            fullscreenMode,
            fullscreenCustom,
            DEFAULT_FULLSCREEN_SEEKBAR_PADDING_DP,
            MAX_FULLSCREEN_SEEKBAR_PADDING_DP,
        )
    val portraitOptions = PortraitSeekbarModes.map { ChorToggleOption(it, stringResource(seekbarModeLabel(it))) }
    val fullscreenOptions = FullscreenSeekbarModes.map { ChorToggleOption(it, stringResource(seekbarModeLabel(it))) }
    val scrubOptions =
        listOf(
            ChorToggleOption(ScrubPreviewStyle.STRIP, stringResource(R.string.player_appearance_scrub_preview_strip)),
            ChorToggleOption(ScrubPreviewStyle.FRAME, stringResource(R.string.player_appearance_scrub_preview_frame)),
        )
    val shortsOptions = ShortsPlayerUiMode.entries.map { ChorToggleOption(it, stringResource(shortsUiModeLabel(it))) }
    val miniPlayerOptions = MiniPlayerSize.entries.map { ChorToggleOption(it, stringResource(miniPlayerSizeLabel(it))) }
    val portraitSummary = stringResource(R.string.player_fullscreen_seekbar_width_subtitle, portraitPadding)
    val fullscreenSummary = stringResource(R.string.player_fullscreen_seekbar_width_subtitle, fullscreenPadding)
    val sliderLabel = stringResource(sliderStyleLabel(sliderStyle))
    val gestureLabel = stringResource(gestureOverlayLabel(gestureOverlay))
    val swipeLeftLabel = stringResource(miniBarSwipeLeft.labelRes)
    val swipeRightLabel = stringResource(miniBarSwipeRight.labelRes)

    SettingsPage(
        title = stringResource(R.string.player_appearance_title),
        onBack = onBack,
        highlight = highlight,
    ) {
        group(key = "player_appearance.seek_bar", header = R.string.settings_section_seek_bar) {
            nav(PlayerAppearanceIndex.sliderStyle, value = sliderLabel, onClick = { sheet = PreviewSheet.SLIDER })
            toggleGroup(
                PlayerAppearanceIndex.portraitWidth,
                portraitOptions,
                portraitMode,
                viewModel::setPortraitSeekbarMode,
                summary = portraitSummary,
                preview = { SeekbarWidthPreview(portraitPadding.dp) },
            )
            if (portraitMode == SeekbarPaddingMode.CUSTOM) {
                seekbarSlider(
                    PlayerAppearanceIndex.portraitCustom,
                    portraitCustom,
                    MAX_PORTRAIT_SEEKBAR_PADDING_DP,
                    viewModel::setPortraitSeekbarCustom,
                )
            }
            toggleGroup(
                PlayerAppearanceIndex.fullscreenWidth,
                fullscreenOptions,
                fullscreenMode,
                viewModel::setFullscreenSeekbarMode,
                summary = fullscreenSummary,
                preview = { SeekbarWidthPreview(fullscreenPadding.dp) },
            )
            if (fullscreenMode == SeekbarPaddingMode.CUSTOM) {
                seekbarSlider(
                    PlayerAppearanceIndex.fullscreenCustom,
                    fullscreenCustom,
                    MAX_FULLSCREEN_SEEKBAR_PADDING_DP,
                    viewModel::setFullscreenSeekbarCustom,
                )
            }
            toggleGroup(PlayerAppearanceIndex.scrubPreview, scrubOptions, scrubPreview, viewModel::setScrubPreview)
            switch(PlayerAppearanceIndex.frameStep, frameStep, viewModel::setFrameStepButtons)
        }

        private fun SettingsGroupScope.seekbarSlider(
            entry: SettingEntry,
            value: Int,
            max: Int,
            onCommit: (Int) -> Unit,
        ) = slider(
            entry = entry,
            value = value.toFloat(),
            onValueCommitted = { committed ->
                onCommit(((committed / SEEKBAR_STEP_DP).roundToInt() * SEEKBAR_STEP_DP).coerceIn(0, max))
            },
            valueRange = 0f..max.toFloat(),
            steps = max / SEEKBAR_STEP_DP - 1,
            valueLabel = { stringResource(R.string.player_fullscreen_seekbar_width_value, it.roundToInt()) },
        )
