package io.github.pushkar.chorplay.data.feed

data class FeedPrefetchRequest(
    val generation: Int,
    val targetItemCount: Int,
)

class FeedPrefetchQueue(
    private val prefetchAheadItemCount: Int,
    private val triggerRemainingItems: Int,
) {
    private var generation = 0
    private var isVisible = false
    private var targetItemCount = 0


    @Synchronized
    fun onVisible(
        currentItemCount: Int,
        feedReady: Boolean,
    ): FeedPrefetchRequest? {
        isVisible = true
        return if (feedReady) currentRequestLocked(currentItemCount) else null
    }

    @Synchronized
    fun onHidden() {
        isVisible = false
        generation++
    }

    @Synchronized
    fun onViewportChanged(
        currentItemCount: Int,
        lastVisibleItemIndex: Int,
    ): FeedPrefetchRequest? {
        if (!isVisible || currentItemCount <= 0) return null
        val remainingBelowViewport = currentItemCount - (lastVisibleItemIndex + 1)
        if (remainingBelowViewport > triggerRemainingItems) return null
        targetItemCount = maxOf(targetItemCount, lastVisibleItemIndex + 1 + prefetchAheadItemCount)
        return currentRequestLocked(currentItemCount)
    }

    @Synchronized
    fun currentRequest(currentItemCount: Int): FeedPrefetchRequest? = currentRequestLocked(currentItemCount)

    @Synchronized
    fun reset() {
        generation++
        targetItemCount = 0
    }

    @Synchronized
    fun isCurrent(requestGeneration: Int): Boolean = isVisible && generation == requestGeneration

    private fun currentRequestLocked(currentItemCount: Int): FeedPrefetchRequest? =
        if (isVisible && currentItemCount < targetItemCount) FeedPrefetchRequest(generation, targetItemCount) else null
}
