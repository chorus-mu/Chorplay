package io.github.pushkar.chorplay.data.local

data class PlayerOverlayPreferences(
    val castEnabled: Boolean = true,
    val captionsEnabled: Boolean = false,
    val pipEnabled: Boolean = false,
    val autoplayEnabled: Boolean = false,
    val sleepTimerEnabled: Boolean = true,
    val speedIndicatorEnabled: Boolean = false,
    val commentsEnabled: Boolean = true,
    val sponsorBlockEnabled: Boolean = true,
    val nextPrevButtonsEnabled: Boolean = true,
    val showControlsWhileLoading: Boolean = false,
    val fullscreenSeekbarHorizontalPaddingDp: Int =
        resolveSeekbarHorizontalPaddingDp(
            mode = SeekbarPaddingMode.DEFAULT,
            customPaddingDp = DEFAULT_FULLSCREEN_SEEKBAR_PADDING_DP,
            defaultPaddingDp = DEFAULT_FULLSCREEN_SEEKBAR_PADDING_DP,
            maxPaddingDp = MAX_FULLSCREEN_SEEKBAR_PADDING_DP,
        ),
    val portraitSeekbarHorizontalPaddingDp: Int =
        resolveSeekbarHorizontalPaddingDp(
            mode = resolvePortraitSeekbarPaddingMode(null),
            customPaddingDp = DEFAULT_PORTRAIT_SEEKBAR_PADDING_DP,
            defaultPaddingDp = DEFAULT_PORTRAIT_SEEKBAR_PADDING_DP,
            maxPaddingDp = MAX_PORTRAIT_SEEKBAR_PADDING_DP,
        ),
    val scrubPreviewStyle: ScrubPreviewStyle = ScrubPreviewStyle.STRIP,
    val frameStepButtonsEnabled: Boolean = false,
    val sponsorCategoryColors: Map<String, Int> = emptyMap(),
)
