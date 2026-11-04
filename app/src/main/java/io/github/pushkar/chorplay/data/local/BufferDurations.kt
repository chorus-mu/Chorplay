package io.github.pushkar.chorplay.data.local

data class BufferDurations(
    val minMs: Int,
    val maxMs: Int,
    val playbackMs: Int,
    val rebufferMs: Int,
) {
    companion object {

        const val MIN_FLOOR_MS = 2_500

        const val PLAYBACK_FLOOR_MS = 250

        const val REBUFFER_FLOOR_MS = 750

        const val MAX_HEADROOM_MS = 5_000

        fun sanitize(
            minMs: Int,
            maxMs: Int,
            playbackMs: Int,
            rebufferMs: Int,
            maxSafeMinMs: Int,
            maxSafeMaxMs: Int,
        ): BufferDurations {
            val minCeiling = maxOf(maxSafeMinMs, MIN_FLOOR_MS).coerceAtMost(Int.MAX_VALUE - MAX_HEADROOM_MS)
            val resolvedMin = maxOf(minMs, rebufferMs).coerceIn(MIN_FLOOR_MS, minCeiling)
            val maxFloor = resolvedMin + MAX_HEADROOM_MS
            val resolvedMax = maxOf(maxMs, maxFloor).coerceAtMost(maxOf(maxSafeMaxMs, maxFloor))

            return BufferDurations(
                minMs = resolvedMin,
                maxMs = resolvedMax,
                playbackMs = playbackMs.coerceIn(PLAYBACK_FLOOR_MS, maxOf(resolvedMin, PLAYBACK_FLOOR_MS)),
                rebufferMs = rebufferMs.coerceIn(REBUFFER_FLOOR_MS, maxOf(resolvedMin, REBUFFER_FLOOR_MS)),
            )
        }
    }
}
