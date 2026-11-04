package io.github.pushkar.chorplay.data.comments

import android.util.Log
import io.github.pushkar.chorplay.data.model.Comment
import io.github.pushkar.chorplay.data.model.distinctByNonBlankKey
import io.github.pushkar.chorplay.data.model.mergeDistinctByNonBlankKey
import io.github.pushkar.chorplay.data.repository.YouTubeRepository
import io.github.pushkar.chorplay.innertube.pages.VideoCommentSort
import io.github.pushkar.chorplay.player.PlaybackStartupPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "CommentsPager"

private const val STARTUP_GATE_TIMEOUT_MS = 20_000L

private const val DEFAULT_PREFETCH_PAGES = 3

internal data class CommentsPlaybackState(
    val isPlaybackLoading: Boolean,
    val currentVideoId: String?,
) {
    companion object {
        val READY = CommentsPlaybackState(isPlaybackLoading = false, currentVideoId = null)
    }
}

internal class CommentsPager(
    private val repository: YouTubeRepository,
    private val scope: CoroutineScope,
    private val playbackState: Flow<CommentsPlaybackState> = flowOf(CommentsPlaybackState.READY),
    private val isCurrentVideo: (String) -> Boolean = { true },
    private val fetchTimeoutMs: Long? = null,
    private val prefetchPages: Int = DEFAULT_PREFETCH_PAGES,
) {
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _sortOptions = MutableStateFlow<List<VideoCommentSort>>(emptyList())
    val sortOptions: StateFlow<List<VideoCommentSort>> = _sortOptions.asStateFlow()

    private val _totalText = MutableStateFlow<String?>(null)
    val totalText: StateFlow<String?> = _totalText.asStateFlow()

    private val _totalCount = MutableStateFlow<Long?>(null)
    val totalCount: StateFlow<Long?> = _totalCount.asStateFlow()

    private var next: CommentsPageResult = CommentsPageResult.EMPTY
    private var loadJob: Job? = null
    private var prefetchJob: Job? = null
    private var loadingVideoId: String? = null
    private var activeSortToken: String? = null
    private val repliesInFlight = mutableSetOf<String>()

    fun clear() {
        loadJob?.cancel()
        prefetchJob?.cancel()
        loadJob = null
        prefetchJob = null
        loadingVideoId = null
        activeSortToken = null
        next = CommentsPageResult.EMPTY
        _comments.value = emptyList()
        _isLoading.value = false
        _hasMore.value = false
        _isLoadingMore.value = false
        _sortOptions.value = emptyList()
        _totalText.value = null
        _totalCount.value = null
    }

    fun load(videoId: String) {
        if (loadingVideoId == videoId && activeSortToken == null && loadJob?.isActive == true) return
        startLoad(videoId, sortToken = null)
    }

    fun selectSort(
        videoId: String,
        sort: VideoCommentSort,
    ) {
        if (activeSortToken == sort.token && loadJob?.isActive == true) return
        startLoad(videoId, sortToken = sort.token)
    }

    private fun startLoad(
        videoId: String,
        sortToken: String?,
    ) {
        loadJob?.cancel()
        prefetchJob?.cancel()
        if (videoId != loadingVideoId) {
            _sortOptions.value = emptyList()
            _totalText.value = null
            _totalCount.value = null
        }
        loadingVideoId = videoId
        activeSortToken = sortToken
        _comments.value = emptyList()
        next = CommentsPageResult.EMPTY
        _hasMore.value = false
        _isLoadingMore.value = false
        _isLoading.value = true
        loadJob =
            scope.launch {
                try {
                    withTimeoutOrNull(STARTUP_GATE_TIMEOUT_MS) {
                        playbackState.first { state ->
                            !PlaybackStartupPolicy.shouldDelaySecondaryContent(
                                isPlaybackLoading = state.isPlaybackLoading,
                                currentVideoId = state.currentVideoId,
                                requestedVideoId = videoId,
                            )
                        }
                    }
                    if (!isCurrentVideo(videoId)) return@launch
                    val page = fetch { repository.getVideoComments(videoId, sortToken) } ?: return@launch
                    if (!isCurrentVideo(videoId)) return@launch
                    _comments.value = page.comments.distinctByNonBlankKey(Comment::id)
                    next = page
                    _hasMore.value = page.hasMore
                    if (page.sortOptions.isNotEmpty()) _sortOptions.value = page.sortOptions
                    page.totalText?.let { _totalText.value = it }
                    page.totalCount?.let { _totalCount.value = it }
                    prefetchFollowingPages(videoId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading comments", e)
                } finally {
                    if (loadingVideoId == videoId) _isLoading.value = false
                }
            }
    }

    fun loadMore(videoId: String) {
        if (!next.hasMore) return
        if (_isLoadingMore.value) return
        prefetchJob?.cancel()
        scope.launch {
            _isLoadingMore.value = true
            try {
                appendNextPage(videoId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error loading more comments", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun prefetchFollowingPages(videoId: String) {
        if (prefetchPages <= 0) return
        prefetchJob?.cancel()
        prefetchJob =
            scope.launch {
                repeat(prefetchPages) {
                    if (!next.hasMore || !isCurrentVideo(videoId) || _isLoadingMore.value) return@launch
                    try {
                        appendNextPage(videoId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.d(TAG, "Comment prefetch stopped for $videoId: ${e.message}")
                        return@launch
                    }
                }
            }
    }

    private suspend fun appendNextPage(videoId: String) {
        val continuation = next.continuation
        val legacyPage = next.legacyPage
        val page =
            when {
                continuation != null -> {
                    repository.getMoreVideoComments(videoId, continuation)
                }

                legacyPage != null -> {
                    val (comments, nextLegacy) = repository.getMoreComments(videoId, legacyPage)
                    CommentsPageResult(comments = comments, legacyPage = nextLegacy)
                }

                else -> {
                    return
                }
            }
        if (!isCurrentVideo(videoId)) return
        _comments.value = _comments.value.mergeDistinctByNonBlankKey(page.comments, Comment::id)
        next = page
        _hasMore.value = page.hasMore
    }

    fun loadReplies(
        videoId: String,
        comment: Comment,
    ) = loadReplies(videoId, comment, append = false)

    fun loadMoreReplies(
        videoId: String,
        comment: Comment,
    ) = loadReplies(videoId, comment, append = true)

    private fun loadReplies(
        videoId: String,
        comment: Comment,
        append: Boolean,
    ) {
        val continuation = comment.continuationToken
        val repliesPage = comment.repliesPage
        if (continuation == null && repliesPage == null) return
        if (!repliesInFlight.add(comment.id)) return
        scope.launch {
            try {
                val (replies, nextContinuation, nextLegacyPage) =
                    if (continuation != null) {
                        val page = repository.getVideoCommentReplies(videoId, continuation)
                        Triple(page.comments, page.continuation, null)
                    } else {
                        val url = "https://www.youtube.com/watch?v=$videoId"
                        val (items, nextPage) = repository.getCommentReplies(url, requireNotNull(repliesPage))
                        Triple(items, null, nextPage)
                    }
                _comments.value =
                    _comments.value.map { current ->
                        if (current.id != comment.id) {
                            current
                        } else {
                            current.copy(
                                replies =
                                    if (append) {
                                        current.replies.mergeDistinctByNonBlankKey(replies, Comment::id)
                                    } else {
                                        replies.distinctByNonBlankKey(Comment::id)
                                    },
                                continuationToken = nextContinuation,
                                repliesPage = nextLegacyPage,
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error loading replies", e)
            } finally {
                repliesInFlight.remove(comment.id)
            }
        }
    }

    private suspend fun fetch(block: suspend () -> CommentsPageResult): CommentsPageResult? =
        if (fetchTimeoutMs == null) block() else withTimeoutOrNull(fetchTimeoutMs) { block() }
}
