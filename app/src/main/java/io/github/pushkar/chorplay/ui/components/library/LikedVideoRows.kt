package io.github.pushkar.chorplay.ui.components.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.LikedVideoInfo
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.model.toVideo
import io.github.pushkar.chorplay.ui.components.shared.MediaKind
import io.github.pushkar.chorplay.ui.components.shared.MediaRowAction

@Composable
internal fun LikedRow(
    like: LikedVideoInfo,
    onVideoClick: (Video) -> Unit,
    onUnlike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val video = remember(like) { like.toVideo() }
    val unlikeLabel = stringResource(R.string.unlike)

    LibraryMediaListRow(
        video = video,
        title = like.title,
        onVideoClick = { onVideoClick(video) },
        removeLabel = unlikeLabel,
        onRemove = onUnlike,
        modifier = modifier,
        subtitle = like.channelName.takeIf { it.isNotBlank() },
        thumbnailUrl = like.thumbnail,
    ) {
        MediaRowAction(
            icon = Icons.Filled.ThumbUp,
            contentDescription = unlikeLabel,
            onClick = onUnlike,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

internal fun MediaKind.emptyTitleRes(): Int =
    when (this) {
        MediaKind.Videos -> R.string.empty_liked_videos
    }

internal fun MediaKind.emptyBodyRes(): Int =
    when (this) {
        MediaKind.Videos -> R.string.empty_liked_body
    }
