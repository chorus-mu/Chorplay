package io.github.pushkar.chorplay.ui.screens.settings.appearance.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.GestureOverlayStyle
import io.github.pushkar.chorplay.data.local.MiniBarSwipeAction
import io.github.pushkar.chorplay.data.local.SliderStyle
import io.github.pushkar.chorplay.ui.components.shared.GestureLevelHudPreview
import io.github.pushkar.chorplay.ui.components.shared.MediaMiniBarDefaults
import io.github.pushkar.chorplay.ui.components.shared.MediaMiniBarSwipeReveal
import io.github.pushkar.chorplay.ui.components.shared.MiniBarReveal
import io.github.pushkar.chorplay.ui.components.shared.icon
import io.github.pushkar.chorplay.ui.components.shared.isDestructive
import io.github.pushkar.chorplay.ui.components.shared.labelRes
import io.github.pushkar.chorplay.ui.theme.PlayerGround

private const val PREVIEW_PROGRESS = 0.4f
private const val PREVIEW_DURATION = 100f
private const val SEEKBAR_PREVIEW_PROGRESS = 0.36f
private val SeekbarPreviewHeight = 44.dp
private val SeekbarPreviewInset = 8.dp
private val SeekbarTrackHeight = 4.dp
private val BackgroundPreviewHeight = 72.dp
private val GesturePreviewHeight = 184.dp

@Composable
internal fun GestureOverlayPreview(style: GestureOverlayStyle) {
    GestureLevelHudPreview(
        style = style,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(GesturePreviewHeight)
                .clip(MaterialTheme.shapes.large)
                .background(PlayerGround),
    )
}

@Composable
internal fun MiniBarSwipePreview(
    action: MiniBarSwipeAction,
    towardsLeft: Boolean,
) {
    val reveal =
        MiniBarReveal(
            icon = action.icon,
            label = stringResource(action.labelRes),
            destructive = action.isDestructive,
        )
    Box(modifier = Modifier.fillMaxWidth().height(MediaMiniBarDefaults.Height)) {
        MediaMiniBarSwipeReveal(
            offset = { if (towardsLeft) -Float.MAX_VALUE else Float.MAX_VALUE },
            towardsLeft = reveal,
            towardsRight = reveal,
            shape = RoundedCornerShape(MediaMiniBarDefaults.CornerRadius),
        )
    }
}


