package io.github.pushkar.chorplay.player.audio

import android.content.Context
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.player.state.EnhancedPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log10

@UnstableApi
class AudioFeaturesManager(
    private val scope: CoroutineScope,
    private val stateFlow: MutableStateFlow<EnhancedPlayerState>,
) {
    companion object {
        private const val TAG = "AudioFeaturesManager"

        fun getAudioSessionId(): Int = 0
    }

    private var playerRef: ExoPlayer? = null

    private var pendingSkipSilence: Boolean? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var desiredPlaybackSpeed: Float = 1.0f
    private var boostGainMb: Int = 0
    private var userVolume: Float = 1f
    private var normalizationEnabled: Boolean = true

    private var loudness: Pair<String, Float>? = null

    fun setPlayer(player: ExoPlayer) {
        this.playerRef = player
        if (desiredPlaybackSpeed != 1.0f) {
            player.setPlaybackParameters(PlaybackParameters(desiredPlaybackSpeed))

            stateFlow.value = stateFlow.value.copy(playbackSpeed = desiredPlaybackSpeed)
            Log.d(TAG, "Re-applied playback speed on new player: ${desiredPlaybackSpeed}x")
        }
        pendingSkipSilence?.let { pending ->
            player.skipSilenceEnabled = pending
            pendingSkipSilence = null
            Log.d(TAG, "Applied pending skip silence: $pending")
        }
        applyVolume(player)
    }

    fun onAudioSessionChanged() {
        val player = playerRef ?: return
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        applyLoudnessGain(player)
    }

    fun clearPlayer() {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        playerRef = null
    }

    fun setSkipSilenceInternal(isEnabled: Boolean) {
        val player = playerRef
        if (player != null) {
            player.skipSilenceEnabled = isEnabled
        } else {
            pendingSkipSilence = isEnabled
            Log.d(TAG, "Player not ready, queuing skip silence: $isEnabled")
        }
        stateFlow.value = stateFlow.value.copy(isSkipSilenceEnabled = isEnabled)
    }

    fun toggleSkipSilence(
        isEnabled: Boolean,
        context: Context?,
    ) {
        setSkipSilenceInternal(isEnabled)
        context?.let { ctx ->
            scope.launch {
                PlayerPreferences(ctx).setSkipSilenceEnabled(isEnabled)
            }
        }
    }

    fun setPlaybackSpeed(
        player: ExoPlayer?,
        speed: Float,
    ) {
        desiredPlaybackSpeed = speed
        stateFlow.value = stateFlow.value.copy(playbackSpeed = speed)
        val target = player ?: playerRef
        if (target != null) {
            target.setPlaybackParameters(PlaybackParameters(speed))
            Log.d(TAG, "Playback speed set to: ${speed}x")
        } else {
            Log.d(TAG, "Player not ready, queued playback speed: ${speed}x")
        }
    }

    fun setVolumeBoost(
        player: ExoPlayer?,
        volume: Float,
    ) {
        val exoPlayer = player ?: return
        userVolume = volume
        boostGainMb = boostGainMillibels(volume)
        applyVolume(exoPlayer)
        applyLoudnessGain(exoPlayer)
        Log.d(TAG, "Volume set to: $volume (player=${exoPlayer.volume}, boost=${boostGainMb}mB)")
    }

    fun setLoudness(
        mediaId: String,
        gainDb: Float?,
    ) {
        loudness = gainDb?.let { mediaId to it }
        playerRef?.let(::applyVolume)
    }

    fun onMediaItemChanged() {
        playerRef?.let(::applyVolume)
    }

    private fun applyVolume(player: ExoPlayer) {
        val gainDb =
            loudness
                ?.takeIf { (mediaId, _) -> normalizationEnabled && mediaId == player.currentMediaItem?.mediaId }
                ?.second
        player.volume = userVolume.coerceIn(0f, 1f) * LoudnessGain.volume(gainDb)
    }

    fun observeSkipSilencePreference(
        context: Context,
        observerScope: CoroutineScope,
    ) {
        observerScope.launch {
            PlayerPreferences(context).skipSilenceEnabled.collect { isEnabled ->
                setSkipSilenceInternal(isEnabled)
            }
        }
    }

    fun observeVolumeNormalizationPreference(
        context: Context,
        observerScope: CoroutineScope,
    ) {
        observerScope.launch {
            PlayerPreferences(context).videoVolumeNormalizationEnabled.collect(::setVolumeNormalizationInternal)
        }
    }

    fun toggleVolumeNormalization(
        isEnabled: Boolean,
        context: Context?,
    ) {
        setVolumeNormalizationInternal(isEnabled)
        context?.let { ctx -> scope.launch { PlayerPreferences(ctx).setVideoVolumeNormalizationEnabled(isEnabled) } }
    }

    private fun setVolumeNormalizationInternal(isEnabled: Boolean) {
        normalizationEnabled = isEnabled
        playerRef?.let(::applyVolume)
        stateFlow.value = stateFlow.value.copy(isVolumeNormalizationEnabled = isEnabled)
    }

    private fun applyLoudnessGain(player: ExoPlayer) {
        val targetGainMb = boostGainMb.coerceIn(0, MAX_GAIN_MB)
        try {
            if (targetGainMb <= 0) {
                loudnessEnhancer?.release()
                loudnessEnhancer = null
                Log.d(TAG, "LoudnessEnhancer released")
                return
            }
            val sessionId = player.audioSessionId
            if (sessionId == android.media.AudioTrack.ERROR_BAD_VALUE || sessionId == 0) {
                Log.d(TAG, "No audio session yet — gain of ${targetGainMb}mB deferred")
                return
            }
            val existing = loudnessEnhancer
            if (existing != null) {
                existing.setTargetGain(targetGainMb)
                existing.enabled = true
            } else {
                loudnessEnhancer =
                    LoudnessEnhancer(sessionId).apply {
                        setTargetGain(targetGainMb)
                        enabled = true
                    }
            }
            Log.d(TAG, "LoudnessEnhancer at ${targetGainMb}mB on session $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure LoudnessEnhancer", e)
        }
    }
}

private const val MAX_GAIN_MB = 2000

internal fun boostGainMillibels(volume: Float): Int =
    if (volume <= 1f) {
        0
    } else {
        (2000.0 * log10(volume.toDouble())).toInt().coerceIn(0, MAX_GAIN_MB)
    }
