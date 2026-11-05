package io.github.pushkar.chorplay.ui.screens.player.content

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.ui.components.shared.ChorplayFilterChip

internal enum class RelatedVideoFilter {
    ALL,
    FROM_CHANNEL,
    RELATED,
    RECENTLY_UPLOADED,
    WATCHED,
}

@Composable
internal fun PlayerRelatedFilterChips(
    channelName: String,
    selectedFilter: RelatedVideoFilter,
    onFilterSelected: (RelatedVideoFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier =
            modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChorplayFilterChip(
            label = stringResource(R.string.player_filter_all),
            selected = selectedFilter == RelatedVideoFilter.ALL,
            onClick = { onFilterSelected(RelatedVideoFilter.ALL) },
        )

        val trimmedChannel = channelName.trim()
        if (trimmedChannel.isNotEmpty()) {
            ChorplayFilterChip(
                label = stringResource(R.string.player_filter_from_channel, trimmedChannel),
                selected = selectedFilter == RelatedVideoFilter.FROM_CHANNEL,
                onClick = { onFilterSelected(RelatedVideoFilter.FROM_CHANNEL) },
            )
        }

        ChorplayFilterChip(
            label = stringResource(R.string.player_filter_related),
            selected = selectedFilter == RelatedVideoFilter.RELATED,
            onClick = { onFilterSelected(RelatedVideoFilter.RELATED) },
        )

        ChorplayFilterChip(
            label = stringResource(R.string.player_filter_recently_uploaded),
            selected = selectedFilter == RelatedVideoFilter.RECENTLY_UPLOADED,
            onClick = { onFilterSelected(RelatedVideoFilter.RECENTLY_UPLOADED) },
        )

        ChorplayFilterChip(
            label = stringResource(R.string.player_filter_watched),
            selected = selectedFilter == RelatedVideoFilter.WATCHED,
            onClick = { onFilterSelected(RelatedVideoFilter.WATCHED) },
        )
    }
}

