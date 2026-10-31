package io.github.pushkar.chorplay.ui.screens.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.model.Channel
import io.github.pushkar.chorplay.ui.components.layout.chorBottomContentPadding

private val ContentHorizontalPadding = 16.dp
private val ListItemSpacing = 12.dp

@Composable
internal fun SubscriptionsManageContent(
    channels: List<Channel>,
    searchQuery: String,
    notificationStates: Map<String, Boolean>,
    excludedShortsChannelIds: Set<String>,
    onChannelClick: (Channel) -> Unit,
    onNotificationChange: (String, Boolean) -> Unit,
    onShortsExcludeChange: (String, Boolean) -> Unit,
    onUnsubscribe: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredChannels =
        remember(channels, searchQuery) {
            if (searchQuery.isBlank()) {
                channels
            } else {
                channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = ContentHorizontalPadding,
                    end = ContentHorizontalPadding,
                    top = 4.dp,
                    bottom = chorBottomContentPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(ListItemSpacing),
        ) {
            item {
                Text(
                    text =
                        pluralStringResource(
                            id = R.plurals.channels_count,
                            count = filteredChannels.size,
                            filteredChannels.size,
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            items(filteredChannels, key = { it.id }) { channel ->
                SubscriptionManagerItem(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    onUnsubscribe = { onUnsubscribe(channel) },
                    isNotificationsEnabled = notificationStates[channel.id] ?: false,
                    areShortsExcluded = channel.id in excludedShortsChannelIds,
                    onNotificationChange = { enabled -> onNotificationChange(channel.id, enabled) },
                    onShortsExcludeChange = { excluded -> onShortsExcludeChange(channel.id, excluded) },
                )
            }

            if (filteredChannels.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_subscriptions_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = ContentHorizontalPadding),
                    )
                }
            }
        }
    }
}
