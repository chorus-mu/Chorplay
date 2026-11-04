package io.github.pushkar.chorplay.data.comments

import io.github.pushkar.chorplay.data.model.Comment
import io.github.pushkar.chorplay.innertube.pages.VideoCommentSort
import org.schabi.newpipe.extractor.Page

data class CommentsPageResult(
    val comments: List<Comment> = emptyList(),
    val continuation: String? = null,
    val legacyPage: Page? = null,
    val sortOptions: List<VideoCommentSort> = emptyList(),
    val totalText: String? = null,
    val totalCount: Long? = null,
) {
    val hasMore: Boolean get() = continuation != null || legacyPage != null

    companion object {
        val EMPTY = CommentsPageResult()
    }
}
