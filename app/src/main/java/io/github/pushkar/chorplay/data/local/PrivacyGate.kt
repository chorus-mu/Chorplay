package io.github.pushkar.chorplay.data.local

import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What Deep Chorplay and paused watch history allow to be recorded. Writers ask here instead of reading
 * the preferences themselves, so every one of them follows the same rules.
 */
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

    /** Deep Chorplay is on and its timer has not run out. */
    suspend fun isDeepChorplayActive(): Boolean = deepChorActive()

    suspend fun isWatchHistoryPaused(): Boolean = watchHistoryPaused()
}
