package io.github.pushkar.chorplay.ui.screens.subscriptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.HomeFeedColumns
import io.github.pushkar.chorplay.data.model.Channel
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.shorts.queue.ShortsQueueSource
import io.github.pushkar.chorplay.ui.components.FEED_MAX_AUTO_COLUMNS
import io.github.pushkar.chorplay.ui.components.FeedCardMinWidth
import io.github.pushkar.chorplay.ui.components.layout.chorBottomContentPadding
import io.github.pushkar.chorplay.ui.components.rememberFeedGridLayout
import io.github.pushkar.chorplay.ui.components.shared.ChorplayFilterChip
import io.github.pushkar.chorplay.ui.components.shared.ChorplayPullToRefreshBox
import io.github.pushkar.chorplay.ui.components.shared.MediaShortsShelf
import io.github.pushkar.chorplay.ui.components.shared.card.MediaVideoCard
import io.github.pushkar.chorplay.ui.components.shared.card.VideoCardLayout
import io.github.pushkar.chorplay.ui.components.shared.dpadFocusGroup
import io.github.pushkar.chorplay.ui.components.shared.rememberFeedGridPlan

private val GroupRowHorizontalPadding = 12.dp
private val GroupRowVerticalPadding = 8.dp
private val GroupChipSpacing = 8.dp
private val GroupEditButtonSize = 32.dp
private val GroupEditGlyphSize = 18.dp
private val HeaderTopPadding = 8.dp
private val HeaderBottomPadding = 12.dp
private val ShelfSpacing = 8.dp
private val ErrorCardPadding = 4.dp
private val FeedTopPadding = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionsFeedContent(
    state: SubscriptionsUiState,
    videos: List<Video>,
    topChannels: List<Channel>,
    gridState: LazyGridState,
    columnPreference: HomeFeedColumns,
    onRefresh: () -> Unit,
    onVideoClick: (Video) -> Unit,
    onShortClick: (ShortsQueueSource) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onChannelSelect: (String?) -> Unit = {},
    onViewAllClick: () -> Unit,
    onGroupSelected: (String?) -> Unit,
    onManageGroups: () -> Unit,
    onRetryFailedChannels: () -> Unit,
    onDismissFailedChannels: () -> Unit,
    modifier: Modifier = Modifier,
    maxAutoColumns: Int = FEED_MAX_AUTO_COLUMNS,
    minCellWidth: Dp = FeedCardMinWidth,
    showShorts: Boolean = true,
) {
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            pullRefreshState.animateToHidden()
        }
    }

    val unreadChannelIds =
        remember(state.recentVideos, state.channelLastVisited, state.watchedVideoIds) {
            state.unreadChannelIds()
        }

    val liveChannelIds =
        remember(state.recentVideos) {
            state.recentVideos.filter { it.isLive }.mapTo(HashSet()) { it.channelId }
        }

    val selectedChannel =
        remember(state.selectedChannelId, topChannels, state.subscribedChannels) {
            state.selectedChannelId?.let { id ->
                topChannels.find { it.id == id } ?: state.subscribedChannels.find { it.id == id }
            }
        }

    val displayedVideos =
        remember(videos, state.selectedChannelId) {
            if (state.selectedChannelId != null) {
                videos.filter { it.channelId == state.selectedChannelId }
            } else {
                videos
            }
        }

    val displayedShorts =
        remember(state.shorts, state.selectedChannelId) {
            if (state.selectedChannelId != null) {
                state.shorts.filter { it.channelId == state.selectedChannelId }
            } else {
                state.shorts
            }
        }

    ChorplayPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        state = pullRefreshState,
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val feedLayout = rememberFeedGridLayout(maxWidth, columnPreference, maxAutoColumns, minCellWidth)
            val listMode = !state.isFullWidthView
            val plan =
                rememberFeedGridPlan(
                    layout = feedLayout,
                    listMode = listMode,
                    itemCount = displayedVideos.size,
                    spansOwnRow = { false },
                    includeLastRun = true,
                    itemsKey = displayedVideos,
                    compactRowSpacing = if (listMode) 0.dp else feedLayout.cardSpacing,
                )
            LazyVerticalGrid(
                columns = plan.cells,
                state = gridState,
                modifier = Modifier.fillMaxSize().dpadFocusGroup(),
                contentPadding = plan.contentPadding(top = FeedTopPadding, bottom = chorBottomContentPadding()),
                verticalArrangement = Arrangement.spacedBy(plan.rowSpacing),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SubscriptionsQuickAccessRow(
                            channels = topChannels,
                            selectedChannelId = state.selectedChannelId,
                            unreadChannelIds = unreadChannelIds,
                            liveChannelIds = liveChannelIds,
                            onChannelClick = { channel ->
                                if (state.selectedChannelId == channel.id) {
                                    onChannelSelect(null)
                                } else {
                                    onChannelSelect(channel.id)
                                }
                            },
                            onViewAllClick = onViewAllClick,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = HeaderTopPadding, bottom = 4.dp),
                        )

                        if (selectedChannel != null) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = GroupRowHorizontalPadding, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(
                                    onClick = { onChannelClick(selectedChannel) },
                                ) {
                                    Text(
                                        text = stringResource(R.string.view_channel),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(
                                            horizontal = GroupRowHorizontalPadding,
                                            vertical = GroupRowVerticalPadding,
                                        ),
                                horizontalArrangement = Arrangement.spacedBy(GroupChipSpacing),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ChorplayFilterChip(
                                    label = stringResource(R.string.group_all),
                                    selected = state.selectedGroupName == null,
                                    onClick = { onGroupSelected(null) },
                                )
                                state.groups.forEach { group ->
                                    ChorplayFilterChip(
                                        label = group.name,
                                        selected = state.selectedGroupName == group.name,
                                        onClick = { onGroupSelected(group.name) },
                                    )
                                }
                                IconButton(
                                    onClick = onManageGroups,
                                    modifier = Modifier.size(GroupEditButtonSize),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.manage_groups),
                                        modifier = Modifier.size(GroupEditGlyphSize),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                if (showShorts && state.isShortsShelfEnabled && displayedShorts.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            MediaShortsShelf(
                                shorts = displayedShorts,
                                onShortClick = { _, tapped ->
                                    onShortClick(ShortsQueueSource.Subscriptions(tapped.id))
                                },
                            )
                            Spacer(modifier = Modifier.height(ShelfSpacing))
                        }
                    }
                }

                if (displayedVideos.isEmpty() && state.selectedChannelId != null && displayedShorts.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.no_recent_videos_for_channel),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            FilledTonalButton(
                                onClick = {
                                    if (selectedChannel != null) onChannelClick(selectedChannel)
                                },
                            ) {
                                Text(stringResource(R.string.view_channel))
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = displayedVideos,
                        key = { _, video -> video.id },
                        contentType = { _, _ -> "video" },
                        span = { index, _ -> plan.span(index, spansOwnRow = false, maxLineSpan = maxLineSpan) },
                    ) { index, video ->
                        MediaVideoCard(
                            video = video,
                            layout = if (plan.isListCard(index)) VideoCardLayout.Row else VideoCardLayout.Stacked,
                            onClick = { onVideoClick(video) },
                            useInternalPadding = !feedLayout.cardsBleed,
                            thumbnailWidth = plan.listThumbnailWidth,
                        )
                    }
                }
            }
        }
    }
}
