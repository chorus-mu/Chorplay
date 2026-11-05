package io.github.pushkar.chorplay.ui.screens.subscriptions

import com.google.common.truth.Truth.assertThat
import io.github.pushkar.chorplay.data.model.Channel
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.subscriptions.withStableUploadSortKeys
import org.junit.Test

class SubscriptionsSortingTest {

    private fun channel(id: String, name: String) =
        Channel(id = id, name = name, thumbnailUrl = "https://example.com/avatar/$id.jpg")

    private fun video(id: String, channelId: String, timestamp: Long) =
        Video(
            id = id,
            title = "Video $id",
            channelName = "Channel $channelId",
            channelId = channelId,
            thumbnailUrl = "",
            duration = 120,
            viewCount = 0L,
            uploadDate = "",
            timestamp = timestamp,
        )

    @Test
    fun `quickAccessOrder places channel with most recent upload at index 0`() {
        val channelA = channel("chA", "Alpha")
        val channelB = channel("chB", "Beta")
        val channelC = channel("chC", "Gamma")

        val recentVideos = listOf(
            video("v1", "chA", timestamp = 1000L),
            video("v2", "chB", timestamp = 5000L), // newest upload
            video("v3", "chC", timestamp = 2000L),
        )

        val ordered = quickAccessOrder(listOf(channelA, channelB, channelC), recentVideos)

        assertThat(ordered.map { it.id }).containsExactly("chB", "chC", "chA").inOrder()
        assertThat(ordered.first().id).isEqualTo("chB")
    }

    @Test
    fun `quickAccessOrder preserves channels without recent uploads at the end`() {
        val channelA = channel("chA", "Alpha")
        val channelB = channel("chB", "Beta")
        val channelC = channel("chC", "Gamma")

        val recentVideos = listOf(
            video("v1", "chB", timestamp = 5000L),
        )

        val ordered = quickAccessOrder(listOf(channelA, channelB, channelC), recentVideos)

        assertThat(ordered.first().id).isEqualTo("chB")
        assertThat(ordered.map { it.id }).containsExactly("chB", "chA", "chC").inOrder()
    }

    @Test
    fun `unreadChannelIds detects channels with unwatched uploads after last visited`() {
        val state = SubscriptionsUiState(
            recentVideos = listOf(
                video("v1", "chA", timestamp = 2000L),
                video("v2", "chB", timestamp = 1000L),
            ),
            channelLastVisited = mapOf(
                "chA" to 1500L, // upload at 2000L > 1500L -> unread
                "chB" to 1500L, // upload at 1000L <= 1500L -> read
            ),
        )

        val unread = state.unreadChannelIds(watchedIds = emptySet())

        assertThat(unread).containsExactly("chA")
    }

    @Test
    fun `unreadChannelIds excludes videos already watched`() {
        val state = SubscriptionsUiState(
            recentVideos = listOf(
                video("v1", "chA", timestamp = 2000L),
            ),
            channelLastVisited = mapOf("chA" to 1000L),
        )

        val unread = state.unreadChannelIds(watchedIds = setOf("v1"))

        assertThat(unread).isEmpty()
    }

    @Test
    fun `quickAccessOrder does not cap large channel lists beyond 15 channels`() {
        val allChannels = (1..30).map { i -> channel("ch$i", "Creator $i") }
        val recentVideos = listOf(
            video("v25", "ch25", timestamp = 5000L),
            video("v18", "ch18", timestamp = 3000L),
        )

        val ordered = quickAccessOrder(allChannels, recentVideos)

        assertThat(ordered).hasSize(30)
        assertThat(ordered[0].id).isEqualTo("ch25")
        assertThat(ordered[1].id).isEqualTo("ch18")
        // All other 28 channels are preserved in the list
        assertThat(ordered.drop(2).map { it.id }).containsNoDuplicates()
        assertThat(ordered.drop(2)).hasSize(28)
    }

    @Test
    fun `quickAccessOrder places channels with active LIVE stream before recent uploads`() {
        val channelA = channel("chA", "Alpha")
        val channelB = channel("chB", "Beta")
        val channelC = channel("chC", "Gamma")

        val recentVideos = listOf(
            video("v1", "chA", timestamp = 9000L), // most recent upload, but NOT live
            video("v2", "chB", timestamp = 1000L).copy(isLive = true), // LIVE stream
            video("v3", "chC", timestamp = 5000L),
        )

        val ordered = quickAccessOrder(listOf(channelA, channelB, channelC), recentVideos)

        assertThat(ordered.first().id).isEqualTo("chB") // Live channel is prioritized first!
        assertThat(ordered.map { it.id }).containsExactly("chB", "chA", "chC").inOrder()
    }

    @Test
    fun `withStableUploadSortKeys places active LIVE stream at top of feed`() {
        val now = 10_000L
        val regularNew = video("v1", "chA", timestamp = 9500L)
        val regularOld = video("v2", "chB", timestamp = 3000L)
        val liveStream = video("v3", "chC", timestamp = 2000L).copy(isLive = true) // Started earlier, but LIVE now!

        val sorted = listOf(regularNew, regularOld, liveStream).withStableUploadSortKeys(now)

        assertThat(sorted.first().id).isEqualTo("v3") // Live stream is pinned at top!
        assertThat(sorted.map { it.id }).containsExactly("v3", "v1", "v2").inOrder()
    }
}

