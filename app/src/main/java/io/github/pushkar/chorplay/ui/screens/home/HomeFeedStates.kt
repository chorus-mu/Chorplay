package io.github.pushkar.chorplay.ui.screens.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.ui.components.shared.ChorplayEmptyState

@Composable
internal fun HomeFeedDisabledState(modifier: Modifier = Modifier) {
    ChorplayEmptyState(
        title = stringResource(R.string.content_settings_home_feed_disabled_title),
        subtitle = stringResource(R.string.content_settings_home_feed_disabled_body),
        icon = Icons.Outlined.SmartDisplay,
        modifier = modifier,
    )
}
