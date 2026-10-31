package io.github.pushkar.chorplay.ui.screens.subscriptions

import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.model.Channel
import io.github.pushkar.chorplay.data.model.Video

internal fun SubscriptionSortMode.labelRes(): Int =
    when (this) {
        SubscriptionSortMode.DEFAULT -> R.string.subscriptions_sort_default
        SubscriptionSortMode.NAME_ASC -> R.string.subscriptions_sort_name
        SubscriptionSortMode.RECENTLY_UPDATED -> R.string.subscriptions_sort_recent
    }

internal fun sortSubscriptions(
    channels: List<Channel>,
    sortMode: SubscriptionSortMode,
    recentVideos: List<Video>,
): List<Channel> =
    when (sortMode) {
        SubscriptionSortMode.DEFAULT -> {
            channels
        }

        SubscriptionSortMode.NAME_ASC -> {
            channels.sortedBy { it.name.lowercase() }
        }

        SubscriptionSortMode.RECENTLY_UPDATED -> {
            val latestUploadByChannel =
                recentVideos
                    .groupBy { it.channelId }
                    .mapValues { (_, videos) -> videos.maxOf { it.timestamp } }
            channels.sortedByDescending { latestUploadByChannel[it.id] ?: 0L }
        }
    }

internal fun quickAccessOrder(channels: List<Channel>): List<Channel> =
    channels.distinctBy(Channel::id)
