package io.github.pushkar.chorplay.data.recommendation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The topics the recommendation engine follows and hides, behind an injectable seam. It adapts the
 * engine's static entry points so callers take this as a dependency instead of reaching the engine
 * globally; the engine keeps its single instance underneath.
 */
@Singleton
class TopicPreferencesRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun preferredTopics(): Set<String> = ChorplayNeuroEngine.getPreferredTopics()

        suspend fun blockedTopics(): Set<String> = ChorplayNeuroEngine.getBlockedTopics()

        suspend fun addPreferred(topic: String) = ChorplayNeuroEngine.addPreferredTopic(context, topic)

        suspend fun removePreferred(topic: String) = ChorplayNeuroEngine.removePreferredTopic(context, topic)

        suspend fun addBlocked(topic: String) = ChorplayNeuroEngine.addBlockedTopic(context, topic)

        suspend fun removeBlocked(topic: String) = ChorplayNeuroEngine.removeBlockedTopic(context, topic)
    }
