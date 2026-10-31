package io.github.pushkar.chorplay.sync.canonical

import io.github.pushkar.chorplay.sync.identity.Hlc
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

@Serializable
data class CanonicalWatchHistory(
    val videoId: String,
    val title: String = "",
    val channelName: String = "",
    val channelId: String = "",
    val thumbnailUrl: String = "",
    val watchedAtMs: Long = 0,
    val progress: Double = 0.0,
    val durationSeconds: Long = 0,
    val isMusic: Boolean = false,
    val isShort: Boolean = false,
    val hlc: String = "",
    val deleted: Boolean = false,
)

@Serializable
data class CanonicalPlaylistItem(
    val videoId: String,
    val position: Long = 0,
    val addedAtMs: Long = 0,
    val deleted: Boolean = false,
    val title: String = "",
    val channelName: String = "",
    val channelId: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Long = 0,
    val isMusic: Boolean = false,
    val hlc: String = "",
)

@Serializable
data class CanonicalPlaylist(
    val syncId: String,
    val origin: String = ORIGIN_LOCAL,
    val youtubeId: String? = null,
    val title: String = "",
    val description: String = "",
    val isMusic: Boolean = false,
    val isUserCreated: Boolean = true,
    val isProtected: Boolean = false,
    val createdAtMs: Long = 0,
    val updatedHlc: String = "",
    val deleted: Boolean = false,
    val items: List<CanonicalPlaylistItem> = emptyList(),
) {
    companion object {
        const val ORIGIN_LOCAL = "local"
        const val ORIGIN_YOUTUBE = "youtube"
        const val RESERVED_WATCH_LATER = "reserved:watch-later"
    }
}

@Serializable
data class CanonicalLikeMeta(
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
)

@Serializable
data class CanonicalLike(
    val kind: String,
    val id: String,
    val state: String,
    val updatedAtMs: Long = 0,
    val hlc: String = "",
    val meta: CanonicalLikeMeta = CanonicalLikeMeta(),
    val title: String = "",
    val channelName: String = "",
    val thumbnailUrl: String = "",
) {
    companion object {
        const val KIND_VIDEO = "video"
        const val KIND_MUSIC = "music"
        const val STATE_LIKED = "liked"
        const val STATE_DISLIKED = "disliked"
        const val STATE_NONE = "none"
    }
}

@Serializable
data class CanonicalSetting(
    val key: String,
    val value: JsonElement,
    val hlc: String = "",
)

@Serializable
data class CanonicalNote(
    val id: String,
    val targetId: String = "",
    val kind: String = "",
    val text: String = "",
    val updatedAt: Long = 0L,
    val deleted: Boolean = false,
    val title: String? = null,
    val channelName: String? = null,
    val channelId: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val channelAvatarUrl: String? = null,
    val channelHandle: String? = null,
    val subscriberCountText: String? = null,
)

@Serializable
data class CanonicalSubscriptionGroup(
    val name: String,
    val channelIds: List<String> = emptyList(),
    val sortOrder: Int = 0,
    val hlc: String = "",
    val deleted: Boolean = false,

    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val members: List<CanonicalGroupMember> = emptyList(),
)

@Serializable
data class CanonicalGroupMember(
    val channelId: String,
    val hlc: String = "",
    val deleted: Boolean = false,
)

@Serializable
data class CanonicalSubscribedChannel(
    val channelId: String,
    val name: String = "",
    val avatarUrl: String = "",
    val subscribedAtMs: Long = 0,
    val isMusic: Boolean = false,
    val hlc: String = "",
    val deleted: Boolean = false,
)

@Serializable(with = GCounterSerializer::class)
data class GCounter(
    val perDevice: Map<String, Long> = emptyMap(),
) {
    fun sum(): Long = perDevice.values.sum()

    fun merge(other: GCounter): GCounter {
        if (other.perDevice.isEmpty()) return this
        if (perDevice.isEmpty()) return other
        val out = HashMap(perDevice)
        for ((d, c) in other.perDevice) {
            out[d] = maxOf(out[d] ?: Long.MIN_VALUE, c)
        }
        return GCounter(out)
    }
}

object GCounterSerializer : KSerializer<GCounter> {
    private val delegate = MapSerializer(String.serializer(), Long.serializer())
    override val descriptor: SerialDescriptor = SerialDescriptor("GCounter", delegate.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: GCounter,
    ) = delegate.serialize(encoder, value.perDevice)

    override fun deserialize(decoder: Decoder): GCounter = GCounter(delegate.deserialize(decoder))
}

@Serializable
data class OrSet(
    val adds: Map<String, String> = emptyMap(),
    val removes: Map<String, String> = emptyMap(),
) {
    fun contains(member: String): Boolean {
        val added = adds[member] ?: return false
        val removed = removes[member] ?: return true
        return Hlc.compareEncoded(added, removed) >= 0
    }

    fun members(): Set<String> = adds.keys.filterTo(LinkedHashSet()) { contains(it) }

    fun add(
        member: String,
        hlc: String,
    ): OrSet = copy(adds = adds + (member to Hlc.maxEncoded(adds[member].orEmpty(), hlc)))

    fun remove(
        member: String,
        hlc: String,
    ): OrSet = copy(removes = removes + (member to Hlc.maxEncoded(removes[member].orEmpty(), hlc)))

    fun merge(other: OrSet): OrSet {
        if (other.adds.isEmpty() && other.removes.isEmpty()) return this
        if (adds.isEmpty() && removes.isEmpty()) return other
        return OrSet(mergeStamps(adds, other.adds), mergeStamps(removes, other.removes))
    }

    private fun mergeStamps(
        a: Map<String, String>,
        b: Map<String, String>,
    ): Map<String, String> {
        if (b.isEmpty()) return a
        if (a.isEmpty()) return b
        val out = HashMap(a)
        for ((k, v) in b) out[k] = Hlc.maxEncoded(out[k].orEmpty(), v)
        return out
    }
}

@Serializable
data class Lww<T>(
    val value: T,
    val hlc: String = "",
) {
    fun merge(other: Lww<T>): Lww<T> = if (Hlc.compareEncoded(other.hlc, hlc) > 0) other else this
}

@Serializable
data class CanonicalVector(
    val topics: Map<String, Double> = emptyMap(),
    val dims: Map<String, Double> = emptyMap(),
) {
    val duration: Double get() = dims[DIM_DURATION] ?: 0.5
    val pacing: Double get() = dims[DIM_PACING] ?: 0.5
    val complexity: Double get() = dims[DIM_COMPLEXITY] ?: 0.5
    val isLive: Double get() = dims[DIM_IS_LIVE] ?: 0.0

    companion object {
        const val DIM_DURATION = "duration"
        const val DIM_PACING = "pacing"
        const val DIM_COMPLEXITY = "complexity"
        const val DIM_IS_LIVE = "isLive"

        fun of(
            topics: Map<String, Double>,
            duration: Double,
            pacing: Double,
            complexity: Double,
            isLive: Double,
        ) = CanonicalVector(
            topics = topics,
            dims =
                mapOf(
                    DIM_DURATION to duration,
                    DIM_PACING to pacing,
                    DIM_COMPLEXITY to complexity,
                    DIM_IS_LIVE to isLive,
                ),
        )
    }
}

@Serializable
data class CanonicalRejectionSignal(
    val count: Int = 0,
    val lastRejectedAt: Long = 0,
)

@Serializable
data class CanonicalFeedEntry(
    val lastShown: Long = 0,
    val showCount: Int = 0,
)

@Serializable
data class CanonicalChannelStrike(
    val count: Int = 0,
    val firstAt: Long = 0,
    val lastAt: Long = 0,
)

@Serializable
data class CanonicalTopicEvidence(
    val positiveSignals: Int = 0,
    val negativeSignals: Int = 0,
    val watchSignals: Int = 0,
    val explicitSignals: Int = 0,
    val positiveScore: Double = 0.0,
    val videoIds: Set<String> = emptySet(),
    val channelIds: Set<String> = emptySet(),
    val firstSeenAt: Long = 0,
    val lastSeenAt: Long = 0,
)

@Serializable
data class CanonicalBrainVectors(
    val globalVector: CanonicalVector = CanonicalVector(),
    val timeVectors: Map<String, CanonicalVector> = emptyMap(),
    val shortsVector: CanonicalVector? = null,
    val topicAffinities: Map<String, Double> = emptyMap(),
    val channelScores: Map<String, Double> = emptyMap(),
    val channelTopicProfiles: Map<String, Map<String, Double>> = emptyMap(),
)

@Serializable
data class BrainCounters(
    val idfTotalDocuments: GCounter = GCounter(),
    val totalInteractions: GCounter = GCounter(),
)

@Serializable
data class BrainPerVideo(
    val watchHistoryMap: Map<String, Float> = emptyMap(),
    val watchSignalProgress: Map<String, Float> = emptyMap(),
)

@Serializable
data class BrainSets(
    val blockedTopics: OrSet = OrSet(),
    val blockedChannels: OrSet = OrSet(),
    val preferredTopics: OrSet = OrSet(),
)

@Serializable
data class BrainLwwMaps(
    val suppressedVideoIds: Map<String, Lww<Long>> = emptyMap(),
    val suppressedChannels: Map<String, Lww<Long>> = emptyMap(),
    val rejectionPatterns: Map<String, Lww<CanonicalRejectionSignal>> = emptyMap(),
    val topicEvidence: Map<String, Lww<CanonicalTopicEvidence>> = emptyMap(),
    val feedHistory: Map<String, Lww<CanonicalFeedEntry>> = emptyMap(),
    val channelStrikes: Map<String, Lww<CanonicalChannelStrike>> = emptyMap(),
)

@Serializable
data class BrainFlags(
    val hasCompletedOnboarding: Boolean = false,
)

@Serializable
data class CanonicalBrain(
    val schema: Int = 13,
    val deviceId: String = "",
    val hlc: String = "",
    val vectors: CanonicalBrainVectors = CanonicalBrainVectors(),
    val counters: BrainCounters = BrainCounters(),
    val idfWordFrequency: Map<String, GCounter> = emptyMap(),
    val perVideo: BrainPerVideo = BrainPerVideo(),
    val sets: BrainSets = BrainSets(),
    val lwwMaps: BrainLwwMaps = BrainLwwMaps(),
    val flags: BrainFlags = BrainFlags(),
)
