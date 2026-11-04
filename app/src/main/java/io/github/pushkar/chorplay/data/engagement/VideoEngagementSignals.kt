package io.github.pushkar.chorplay.data.engagement

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.recommendation.ChorplayNeuroEngine
import io.github.pushkar.chorplay.data.recommendation.InteractionType
import io.github.pushkar.chorplay.data.repository.YouTubeRepository
import javax.inject.Inject

class VideoEngagementSignals
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val repository: YouTubeRepository,
    ) {
        suspend fun channelSubscriptionChanged(
            channelId: String,
            channelName: String,
            subscribed: Boolean,
        ) = ChorplayNeuroEngine.onChannelSubscriptionChanged(context, channelId, channelName, subscribed)

        suspend fun videoInteraction(
            video: Video,
            interactionType: InteractionType,
        ) = ChorplayNeuroEngine.onVideoInteraction(context, video, interactionType)

        suspend fun channelTagsLearned(channelId: String) = repository.learnChannelTags(context, channelId)
    }
