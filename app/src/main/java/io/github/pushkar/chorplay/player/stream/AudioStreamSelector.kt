package io.github.pushkar.chorplay.player.stream

import io.github.pushkar.chorplay.data.local.AudioStreamQuality
import io.github.pushkar.chorplay.innertube.models.response.PlayerResponse
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import java.util.Locale
import kotlin.math.abs

object AudioStreamSelector {
    fun selectPreferredAudioStream(
        streams: List<AudioStream>,
        preferredAudioLanguage: String,
        preferredAudioQuality: AudioStreamQuality = AudioStreamQuality.AUTO,
        compatibilityFilter: ((AudioStream) -> Boolean)? = null,
    ): AudioStream? {
        if (streams.isEmpty()) return null

        val compatibleStreams =
            compatibilityFilter
                ?.let { filter -> streams.filter(filter).ifEmpty { streams } }
                ?: streams

        val preferredCandidates = preferredCandidates(compatibleStreams, preferredAudioLanguage)
        return selectByQuality(preferredCandidates, preferredAudioQuality)
            ?: selectByQuality(compatibleStreams, preferredAudioQuality)
    }

    private fun selectByQuality(
        streams: List<AudioStream>,
        preferredAudioQuality: AudioStreamQuality,
    ): AudioStream? {
        if (streams.isEmpty()) return null

        val streamsWithKnownBitrate = streams.filter { it.audioBitrate() > 0 }.ifEmpty { streams }
        return when (preferredAudioQuality) {
            AudioStreamQuality.AUTO,
            AudioStreamQuality.HIGH,
                -> streamsWithKnownBitrate.maxByOrNull { it.audioBitrate() }

            AudioStreamQuality.MEDIUM -> streamsWithKnownBitrate.minByOrNull { abs(it.audioBitrate() - MEDIUM_BITRATE_TARGET) }

            AudioStreamQuality.LOW -> streamsWithKnownBitrate.minByOrNull { it.audioBitrate() }
        }
    }

    private fun AudioStream.audioBitrate(): Int = averageBitrate.takeIf { it > 0 } ?: bitrate

    private fun preferredCandidates(
        streams: List<AudioStream>,
        preferredAudioLanguage: String,
    ): List<AudioStream> {
        val normalizedPreference = preferredAudioLanguage.trim().lowercase(Locale.ROOT)

        if (normalizedPreference.isBlank() || normalizedPreference == "original") {
            val originals = streams.filter { it.audioTrackType == AudioTrackType.ORIGINAL }
            if (originals.isNotEmpty()) return originals

            val nonDubbed = streams.filter { it.audioTrackType != AudioTrackType.DUBBED }
            if (nonDubbed.isNotEmpty()) return nonDubbed

            return streams
        }

        val languageMatches =
            streams.filter { stream ->
                languageMatches(stream.audioLocale?.toLanguageTag(), normalizedPreference)
            }
        if (languageMatches.isNotEmpty()) return languageMatches

        val originals = streams.filter { it.audioTrackType == AudioTrackType.ORIGINAL }
        if (originals.isNotEmpty()) return originals

        val nonDubbed = streams.filter { it.audioTrackType != AudioTrackType.DUBBED }
        if (nonDubbed.isNotEmpty()) return nonDubbed

        return streams
    }

    fun languageMatches(
        languageTag: String?,
        preference: String,
    ): Boolean {
        if (languageTag.isNullOrBlank() || preference.isBlank()) return false
        return languageTag.equals(preference, ignoreCase = true) ||
                languageTag.startsWith("$preference-", ignoreCase = true)
    }


    internal fun preferredAudioFormats(
        formats: List<PlayerResponse.StreamingData.Format>,
        preferredAudioLanguage: String,
    ): List<PlayerResponse.StreamingData.Format> {
        val normalizedPreference = preferredAudioLanguage.trim().lowercase(Locale.ROOT)

        if (normalizedPreference.isBlank() || normalizedPreference == "original") {
            val originals = formats.filter { it.isOriginal }
            if (originals.isNotEmpty()) return originals
            return formats
        }

        val languageMatches =
            formats.filter { format ->
                languageMatches(format.audioLanguageTag, normalizedPreference)
            }
        if (languageMatches.isNotEmpty()) return languageMatches

        val originals = formats.filter { it.isOriginal }
        if (originals.isNotEmpty()) return originals

        return formats
    }

    private const val MEDIUM_BITRATE_TARGET = 128_000
}
