package io.github.pushkar.chorplay.data.engagement

import android.util.Log
import io.github.pushkar.chorplay.data.local.ChannelSubscription
import io.github.pushkar.chorplay.data.local.LikedVideoInfo
import io.github.pushkar.chorplay.data.local.LikedVideosRepository
import io.github.pushkar.chorplay.data.local.SubscriptionRepository
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.recommendation.InteractionType
import io.github.pushkar.chorplay.data.stats.DislikedVideo
import io.github.pushkar.chorplay.data.stats.LedgerAction
import io.github.pushkar.chorplay.data.stats.VideoStatsRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val TAG = "VideoEngagement"

data class VideoEngagement(
    val isSubscribed: Boolean = false,
    val isNotificationEnabled: Boolean = false,
    val likeState: String? = null,
)

class VideoEngagementUseCase
    @Inject
    constructor(
        private val subscriptionRepository: SubscriptionRepository,
        private val likedVideosRepository: LikedVideosRepository,
        private val signals: VideoEngagementSignals,
        private val videoStats: VideoStatsRecorder,
    ) {
        fun subscriptionState(channelId: String): Flow<Boolean> = subscriptionRepository.isSubscribed(channelId)

        fun notificationState(channelId: String): Flow<Boolean> =
            subscriptionRepository.getSubscription(channelId).map { it?.isNotificationEnabled ?: false }

        fun likeState(videoId: String): Flow<String?> = likedVideosRepository.getLikeState(videoId)

        fun engagement(
            videoId: String,
            channelId: String,
        ): Flow<VideoEngagement> =
            combine(
                subscriptionState(channelId),
                notificationState(channelId),
                likeState(videoId),
            ) { isSubscribed, isNotificationEnabled, likeState ->
                VideoEngagement(
                    isSubscribed = isSubscribed,
                    isNotificationEnabled = isNotificationEnabled,
                    likeState = likeState,
                )
            }

        suspend fun toggleSubscription(
            channelId: String,
            channelName: String,
            channelThumbnail: String,
            onApplied: (Boolean) -> Unit = {},
        ) {
            val isSubscribed = subscriptionRepository.isSubscribed(channelId).first()
            applySubscription(
                channelId = channelId,
                channelName = channelName,
                channelThumbnail = channelThumbnail,
                subscribed = !isSubscribed,
                onApplied = onApplied,
            )
        }

        suspend fun applySubscription(
            channelId: String,
            channelName: String,
            channelThumbnail: String,
            subscribed: Boolean,
            onApplied: (Boolean) -> Unit = {},
        ) {
            if (subscribed) {
                subscriptionRepository.subscribe(
                    ChannelSubscription(
                        channelId = channelId,
                        channelName = channelName,
                        channelThumbnail = channelThumbnail,
                    ),
                )
            } else {
                subscriptionRepository.unsubscribe(channelId)
            }
            onApplied(subscribed)
            runCatching {
                signals.channelSubscriptionChanged(channelId, channelName, subscribed)
            }.onFailure { Log.w(TAG, "Failed to record subscription signal", it) }
            if (subscribed) {
                runCatching { signals.channelTagsLearned(channelId) }
            }
        }

        suspend fun setNotificationEnabled(
            channelId: String,
            enabled: Boolean,
        ) = subscriptionRepository.updateNotificationState(channelId, enabled)

        suspend fun like(
            video: Video,
            signalVideo: Video? = null,
            onApplied: () -> Unit = {},
        ) {
            likedVideosRepository.likeVideo(
                LikedVideoInfo(
                    videoId = video.id,
                    title = video.title,
                    thumbnail = video.thumbnailUrl,
                    channelName = video.channelName,
                    channelId = video.channelId.takeIf(String::isNotBlank),
                    durationSeconds = video.duration,
                ),
            )
            onApplied()
            videoStats.onAction(LedgerAction.LIKE)
            if (signalVideo == null) return
            try {
                signals.videoInteraction(signalVideo, InteractionType.LIKED)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record like signal", e)
            }
        }

        suspend fun dislike(
            videoId: String,
            signalVideo: Video? = null,
            onApplied: () -> Unit = {},
        ) {
            likedVideosRepository.dislikeVideo(videoId)
            onApplied()
            videoStats.onDislike(
                DislikedVideo(
                    videoId = videoId,
                    title = signalVideo?.title.orEmpty(),
                    channelName = signalVideo?.channelName.orEmpty(),
                    at = System.currentTimeMillis(),
                ),
            )
            if (signalVideo == null) return
            try {
                signals.videoInteraction(signalVideo, InteractionType.DISLIKED)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record dislike", e)
            }
        }

        suspend fun removeLike(videoId: String) {
            likedVideosRepository.removeLikeState(videoId)
            videoStats.onDislikeRemoved(videoId)
        }
    }
