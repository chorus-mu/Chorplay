package io.github.pushkar.chorplay.data.recommendation

import kotlin.math.pow

internal object NeighbourChannels {
    const val MIN_POINTERS = 2
    const val MAX_CHANNELS = 12
    const val REFRESH_PER_LOAD = 4
    const val UPLOADS_PER_CHANNEL = 3
    const val REFRESH_AFTER_MS = 6L * 60L * 60L * 1000L
    const val UPLOAD_WINDOW_MS = 14L * 24L * 60L * 60L * 1000L
    const val IGNORED_IMPRESSIONS = 6

    private const val HALF_LIFE_MS = 45.0 * 24.0 * 60.0 * 60.0 * 1000.0

    data class Neighbour(
        val channelId: String,
        val pointers: List<String>,
        val score: Double,
    )

    fun neighbours(
        sources: Collection<NeuroCoWatchStore.Source>,
        anchors: Set<String>,
        excluded: (String) -> Boolean,
        now: Long,
    ): List<Neighbour> {
        val byChannel = HashMap<String, HashMap<String, Double>>()
        sources.forEach { source ->
            if (source.channelId !in anchors) return@forEach
            val weight = 0.5.pow((now - source.at).coerceAtLeast(0L) / HALF_LIFE_MS)
            source.related.forEach { channel ->
                if (channel in anchors || excluded(channel)) return@forEach
                val pointers = byChannel.getOrPut(channel) { HashMap() }
                pointers[source.channelId] = maxOf(pointers[source.channelId] ?: 0.0, weight)
            }
        }
        return byChannel
            .filterValues { it.size >= MIN_POINTERS }
            .map { (channel, pointers) ->
                Neighbour(
                    channel,
                    pointers.entries.sortedByDescending { it.value }.map { it.key },
                    pointers.values.sum()
                )
            }.sortedByDescending { it.score }
            .take(MAX_CHANNELS)
    }
}
