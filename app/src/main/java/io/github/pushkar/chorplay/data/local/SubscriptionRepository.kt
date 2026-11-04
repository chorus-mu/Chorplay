package io.github.pushkar.chorplay.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.pushkar.chorplay.data.engagement.FeedInvalidationBus
import io.github.pushkar.chorplay.data.local.AppDatabase
import io.github.pushkar.chorplay.data.profile.StoreScope
import io.github.pushkar.chorplay.utils.ThumbnailUrlResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.subscriptionsDataStore: DataStore<Preferences> by safePreferencesDataStore(
    name = "subscriptions",
    scope = StoreScope.PROFILE_LIBRARY,
)

class SubscriptionRepository private constructor(
    private val context: Context,
) {
    companion object {
        @Volatile
        private var instance: SubscriptionRepository? = null

        fun getInstance(context: Context): SubscriptionRepository =
            instance ?: synchronized(this) {
                instance ?: SubscriptionRepository(context.applicationContext).also { instance = it }
            }

        private fun channelKey(channelId: String) = stringPreferencesKey("channel_$channelId")

        private fun unsubscribedKey(channelId: String) = stringPreferencesKey("unsub_$channelId")

        private const val UNSUBSCRIBED_PREFIX = "unsub_"

        private const val SUBSCRIPTIONS_ORDER_KEY = "subscriptions_order"

        private const val UNSUBSCRIBE_TOMBSTONE_RETENTION_MS = 365L * 24 * 60 * 60 * 1000
    }

    suspend fun subscribe(channel: ChannelSubscription) {
        context.subscriptionsDataStore.edit { preferences ->
            val safeChannel = channel.withPreservedThumbnail(preferences)

            preferences[channelKey(safeChannel.channelId)] = serializeChannel(safeChannel)
            preferences.remove(unsubscribedKey(safeChannel.channelId)) // re-subscribing clears the tombstone

            val currentOrder = preferences[stringPreferencesKey(SUBSCRIPTIONS_ORDER_KEY)] ?: ""
            val orderList =
                if (currentOrder.isEmpty()) {
                    mutableListOf()
                } else {
                    currentOrder.split(",").toMutableList()
                }

            if (!orderList.contains(safeChannel.channelId)) {
                orderList.add(0, safeChannel.channelId)
                preferences[stringPreferencesKey(SUBSCRIPTIONS_ORDER_KEY)] = orderList.joinToString(",")
            }
        }
    }

    suspend fun subscribeAll(channels: Collection<ChannelSubscription>) {
        if (channels.isEmpty()) return

        context.subscriptionsDataStore.edit { preferences ->
            val currentOrder =
                preferences[stringPreferencesKey(SUBSCRIPTIONS_ORDER_KEY)]
                    .orEmpty()
                    .split(",")
                    .filter { it.isNotEmpty() }
            val knownIds = currentOrder.toMutableSet()
            val newIds = mutableListOf<String>()

            channels.forEach { channel ->
                val safeChannel = channel.withPreservedThumbnail(preferences)
                preferences[channelKey(safeChannel.channelId)] = serializeChannel(safeChannel)
                preferences.remove(unsubscribedKey(safeChannel.channelId))
                if (knownIds.add(safeChannel.channelId)) {
                    newIds += safeChannel.channelId
                }
            }

            if (newIds.isNotEmpty()) {
                preferences[stringPreferencesKey(SUBSCRIPTIONS_ORDER_KEY)] =
                    (newIds.asReversed() + currentOrder).joinToString(",")
            }
        }
    }

    suspend fun unsubscribe(channelId: String) {
        val now = System.currentTimeMillis()
        context.subscriptionsDataStore.edit { preferences ->
            preferences.remove(channelKey(channelId))
            preferences[unsubscribedKey(channelId)] = now.toString()
            prune(preferences, now)

            val currentOrder = preferences[stringPreferencesKey(SUBSCRIPTIONS_ORDER_KEY)] ?: ""
            if (currentOrder.isNotEmpty()) {
                val orderList = currentOrder.split(",").toMutableList()
                orderList.remove(channelId)
                preferences[stringPreferencesKey(SUBSCRIPTIONS_ORDER_KEY)] = orderList.joinToString(",")
            }
        }

        AppDatabase
            .profile(context)
            .cacheDao()
            .deleteSubscriptionFeedForChannel(channelId)
        FeedInvalidationBus.emit(FeedInvalidationBus.Event.ChannelUnsubscribed(channelId))
    }

    fun isSubscribed(channelId: String): Flow<Boolean> =
        context.subscriptionsDataStore.data.map { preferences ->
            preferences.contains(channelKey(channelId))
        }

    fun getAllSubscriptions(): Flow<List<ChannelSubscription>> =
        context.subscriptionsDataStore.data.map { preferences ->
            val orderString = preferences[stringPreferencesKey(SUBSCRIPTIONS_ORDER_KEY)] ?: ""
            if (orderString.isEmpty()) {
                emptyList()
            } else {
                val orderList = orderString.split(",")
                orderList.mapNotNull { channelId ->
                    val channelData = preferences[channelKey(channelId)]
                    channelData?.let { deserializeChannel(it) }
                }
            }
        }

    fun notifyingChannelIds(): Flow<Set<String>> =
        getAllSubscriptions()
            .map(::notifyingIds)
            .distinctUntilChanged()

    /**
     * Get all subscription IDs as a Set
     */
    suspend fun getAllSubscriptionIds(): Set<String> {
        val orderString =
            context.subscriptionsDataStore.data
                .map { preferences ->
                    preferences[stringPreferencesKey(SUBSCRIPTIONS_ORDER_KEY)] ?: ""
                }.first()

        return if (orderString.isEmpty()) {
            emptySet()
        } else {
            orderString.split(",").toSet()
        }
    }

    suspend fun unsubscribedTombstones(): Map<String, Long> =
        context.subscriptionsDataStore.data
            .map { preferences ->
                preferences
                    .asMap()
                    .mapNotNull { (key, value) ->
                        if (!key.name.startsWith(UNSUBSCRIBED_PREFIX)) return@mapNotNull null
                        val at = (value as? String)?.toLongOrNull() ?: return@mapNotNull null
                        key.name.removePrefix(UNSUBSCRIBED_PREFIX) to at
                    }.toMap()
            }.first()

    suspend fun recordUnsubscribedAt(tombstones: Map<String, Long>) {
        if (tombstones.isEmpty()) return
        val now = System.currentTimeMillis()
        context.subscriptionsDataStore.edit { preferences ->
            tombstones.forEach { (channelId, at) ->
                val existing = preferences[unsubscribedKey(channelId)]?.toLongOrNull() ?: 0L
                if (at > existing) preferences[unsubscribedKey(channelId)] = at.toString()
            }
            prune(preferences, now)
        }
    }

    private fun prune(
        preferences: MutablePreferences,
        now: Long,
    ) {
        val cutoff = now - UNSUBSCRIBE_TOMBSTONE_RETENTION_MS
        preferences
            .asMap()
            .keys
            .filter { it.name.startsWith(UNSUBSCRIBED_PREFIX) }
            .forEach { key ->
                val at = (preferences[key] as? String)?.toLongOrNull()
                if (at == null || at < cutoff) preferences.remove(key)
            }
    }

    fun getSubscription(channelId: String): Flow<ChannelSubscription?> =
        context.subscriptionsDataStore.data.map { preferences ->
            val channelData = preferences[channelKey(channelId)]
            channelData?.let { deserializeChannel(it) }
        }

    suspend fun repairVideoThumbnailSubscriptions(fetchChannelThumbnail: suspend (String) -> String): Int {
        val subscriptions = getAllSubscriptions().first()
        val repairs =
            subscriptions
                .filter { ThumbnailUrlResolver.isUnusableChannelAvatar(it.channelThumbnail) }
                .mapNotNull { subscription ->
                    val avatar = fetchChannelThumbnail(subscription.channelId).trim()
                    if (avatar.isNotEmpty() && !ThumbnailUrlResolver.isUnusableChannelAvatar(avatar)) {
                        subscription.channelId to subscription.copy(channelThumbnail = avatar)
                    } else {
                        null
                    }
                }.toMap()

        if (repairs.isEmpty()) return 0

        context.subscriptionsDataStore.edit { preferences ->
            repairs.forEach { (channelId, subscription) ->
                preferences[channelKey(channelId)] = serializeChannel(subscription)
            }
        }
        return repairs.size
    }

    private fun serializeChannel(channel: ChannelSubscription): String = SubscriptionRecordCodec.encode(channel)

    private fun ChannelSubscription.withPreservedThumbnail(preferences: Preferences): ChannelSubscription {
        val existing = preferences[channelKey(channelId)]?.let { deserializeChannel(it) }
        return if (
            ThumbnailUrlResolver.isUnusableChannelAvatar(channelThumbnail) &&
            existing?.channelThumbnail?.isNotBlank() == true &&
            !ThumbnailUrlResolver.isUnusableChannelAvatar(existing.channelThumbnail)
        ) {
            copy(channelThumbnail = existing.channelThumbnail)
        } else {
            this
        }
    }

    private fun deserializeChannel(data: String): ChannelSubscription? = SubscriptionRecordCodec.decode(data)

    suspend fun updateNotificationState(
        channelId: String,
        enabled: Boolean,
    ) {
        var turnedOn = false
        context.subscriptionsDataStore.edit { preferences ->
            val channelData = preferences[channelKey(channelId)]
            if (channelData != null) {
                val subscription = deserializeChannel(channelData)
                if (subscription != null) {
                    val updated = subscription.copy(isNotificationEnabled = enabled)
                    preferences[channelKey(channelId)] = serializeChannel(updated)
                    turnedOn = enabled && !subscription.isNotificationEnabled
                }
            }
        }
        if (turnedOn) bellsTurnedOn.tryEmit(channelId)
    }

    private val bellsTurnedOn = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val notificationsTurnedOn: SharedFlow<String> = bellsTurnedOn.asSharedFlow()

    suspend fun markFeedFetched(
        channelIds: Collection<String>,
        fetchedAt: Long,
    ) {
        if (channelIds.isEmpty()) return

        context.subscriptionsDataStore.edit { preferences ->
            channelIds.forEach { channelId ->
                val subscription = preferences[channelKey(channelId)]?.let { deserializeChannel(it) }
                if (subscription != null) {
                    preferences[channelKey(channelId)] =
                        serializeChannel(subscription.copy(lastFeedFetchAt = fetchedAt))
                }
            }
        }
    }

    suspend fun updateChannelLatestVideo(
        channelId: String,
        videoId: String,
    ) {
        context.subscriptionsDataStore.edit { preferences ->
            val channelData = preferences[channelKey(channelId)]
            if (channelData != null) {
                val subscription = deserializeChannel(channelData)
                if (subscription != null) {
                    val updated =
                        subscription.copy(
                            lastVideoId = videoId,
                            lastCheckTime = System.currentTimeMillis(),
                        )
                    preferences[channelKey(channelId)] = serializeChannel(updated)
                }
            }
        }
    }
}

data class ChannelSubscription(
    val channelId: String,
    val channelName: String,
    val channelThumbnail: String,
    val subscribedAt: Long = System.currentTimeMillis(),
    val lastVideoId: String? = null,
    val lastCheckTime: Long = 0L,
    val isNotificationEnabled: Boolean = false,
    val isMusic: Boolean = false,
    val lastFeedFetchAt: Long = 0L,
)

internal fun notifyingIds(subscriptions: List<ChannelSubscription>): Set<String> =
    subscriptions.filter { it.isNotificationEnabled }.mapTo(HashSet()) { it.channelId }
