package io.github.pushkar.chorplay.ui.components.videoplayer.ambient

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val GLOW_MAX_OPACITY = 0.88f
private val DEFAULT_GLOW_HEIGHT = 230.dp

/**
 * Renders a dynamic, living ambient gradient glow directly underneath the video player.
 *
 * Samples the temporally smoothed 32x18 ambient frame from [AmbientFrameHolder] and applies
 * hardware bilinear texture upscaling combined with a vertical gradient fade into the
 * surface background color, mimicking the immersive ambient lighting seen in desktop
 * browser extensions and YouTube Ambient Mode.
 */
@Composable
fun VideoUnderPlayerGlow(
    frame: ImageBitmap?,
    modifier: Modifier = Modifier,
    glowHeight: Dp = DEFAULT_GLOW_HEIGHT,
) {
    val appearAlpha by animateFloatAsState(
        targetValue = if (frame != null) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "underPlayerGlowAppear",
    )

    if (appearAlpha <= 0.001f || frame == null) return

    val surfaceColor = MaterialTheme.colorScheme.background

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(glowHeight)
                .graphicsLayer {
                    alpha = appearAlpha * GLOW_MAX_OPACITY
                },
    ) {
        Image(
            bitmap = frame,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low,
            modifier =
                Modifier
                    .matchParentSize()
                    .drawWithCache {
                        val verticalFadeMask =
                            Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                0.25f to surfaceColor.copy(alpha = 0.18f),
                                0.55f to surfaceColor.copy(alpha = 0.65f),
                                0.85f to surfaceColor.copy(alpha = 0.92f),
                                1.00f to surfaceColor,
                            )
                        onDrawWithContent {
                            drawContent()
                            drawRect(verticalFadeMask, size = size)
                        }
                    },
        )
    }
}

