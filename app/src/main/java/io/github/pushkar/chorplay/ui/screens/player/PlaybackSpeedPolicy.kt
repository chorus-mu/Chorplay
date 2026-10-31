package io.github.pushkar.chorplay.ui.screens.player

/** What the player knows about a video when it picks a starting speed. */
internal data class PlaybackSpeedContext(
    val videoId: String?,
    val channelId: String?,
) {
    companion object {
        val Unknown = PlaybackSpeedContext(videoId = null, channelId = null)
    }
}

internal sealed interface SpeedDecision {
    /** Applies to this video only; the speed it replaced comes back on the next one. */
    data class Override(
        val speed: Float,
    ) : SpeedDecision

    data class Remembered(
        val speed: Float,
    ) : SpeedDecision

    data object Keep : SpeedDecision
}

internal object PlaybackSpeedPolicy {
    fun decide(
        channelSpeed: Float?,
        rememberSpeed: Boolean,
        rememberedSpeed: Float,
    ): SpeedDecision =
        when {
            channelSpeed != null -> SpeedDecision.Override(channelSpeed)
            rememberSpeed -> SpeedDecision.Remembered(rememberedSpeed)
            else -> SpeedDecision.Keep
        }
}
