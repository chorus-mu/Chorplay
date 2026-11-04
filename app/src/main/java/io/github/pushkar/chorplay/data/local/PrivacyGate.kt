package io.github.pushkar.chorplay.data.local

import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyGate internal constructor(
    private val deepChorActive: suspend () -> Boolean,
    private val watchHistoryPaused: suspend () -> Boolean,
) {
    @Inject
    constructor(preferences: PlayerPreferences) : this(
        deepChorActive = preferences::isDeepChorplayCurrentlyActive,
        watchHistoryPaused = { preferences.watchHistoryPaused.first() },
    )

    suspend fun isDeepChorplayActive(): Boolean = deepChorActive()

    suspend fun isWatchHistoryPaused(): Boolean = watchHistoryPaused()
}
