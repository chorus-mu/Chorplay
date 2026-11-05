package io.github.pushkar.chorplay.ui.screens.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pushkar.chorplay.data.model.Channel
import io.github.pushkar.chorplay.ui.components.shared.dpadFocusGroup

private val RowHorizontalPadding = 12.dp
private val AvatarSpacing = 12.dp
private val AvatarRowTopPadding = 4.dp
private val AvatarRowBottomPadding = 6.dp

@Composable
internal fun SubscriptionsQuickAccessRow(
    channels: List<Channel>,
    selectedChannelId: String? = null,
    unreadChannelIds: Set<String> = emptySet(),
    liveChannelIds: Set<String> = emptySet(),
    onChannelClick: (Channel) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.dpadFocusGroup(),
        contentPadding =
            PaddingValues(
                start = RowHorizontalPadding,
                end = 8.dp,
                top = AvatarRowTopPadding,
                bottom = AvatarRowBottomPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(AvatarSpacing),
    ) {
        items(channels, key = { it.id }) { channel ->
            ChannelAvatarItem(
                channel = channel,
                isSelected = channel.id == selectedChannelId,
                hasUnread = channel.id in unreadChannelIds,
                isLive = channel.id in liveChannelIds,
                onClick = { onChannelClick(channel) },
            )
        }
        item(key = "view_all") {
            AllSubscriptionsAvatarItem(onClick = onViewAllClick)
        }
    }
}
