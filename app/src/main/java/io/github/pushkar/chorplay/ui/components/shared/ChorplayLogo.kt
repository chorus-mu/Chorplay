package io.github.pushkar.chorplay.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.pushkar.chorplay.R

private const val LOGO_ASPECT = 1f

/** The app's badge, drawn in theme colours so it follows every palette. Size it by width. */
@Composable
fun ChorplayLogo(modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(LOGO_ASPECT)) {
        Icon(
            painter = painterResource(R.drawable.ic_chorplay_badge_shape),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chorplay_badge_glyph),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
