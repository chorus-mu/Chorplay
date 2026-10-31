package io.github.pushkar.chorplay.ui.screens.settings.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.DEEP_CHOR_NEVER_EXPIRES_HOURS

internal val DeepChorplayDurations = listOf(DEEP_CHOR_NEVER_EXPIRES_HOURS, 1, 2, 4, 6, 8, 12, 24)

@Composable
internal fun deepChorDurationLabel(hours: Int): String =
    when (hours) {
        DEEP_CHOR_NEVER_EXPIRES_HOURS -> stringResource(R.string.deep_chor_duration_never)
        1 -> stringResource(R.string.deep_chor_duration_1h)
        2 -> stringResource(R.string.deep_chor_duration_2h)
        4 -> stringResource(R.string.deep_chor_duration_4h)
        6 -> stringResource(R.string.deep_chor_duration_6h)
        8 -> stringResource(R.string.deep_chor_duration_8h)
        12 -> stringResource(R.string.deep_chor_duration_12h)
        24 -> stringResource(R.string.deep_chor_duration_24h)
        else -> pluralStringResource(R.plurals.deep_chor_duration_hours, hours, hours)
    }
