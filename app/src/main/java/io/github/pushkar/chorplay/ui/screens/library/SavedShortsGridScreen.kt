package io.github.pushkar.chorplay.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.ui.components.layout.chorBottomContentPadding
import io.github.pushkar.chorplay.ui.components.layout.topbar.ChorplayTopBar
import io.github.pushkar.chorplay.ui.components.rememberFeedGridLayout
import io.github.pushkar.chorplay.ui.components.shared.ChorplayEmptyState
import io.github.pushkar.chorplay.ui.components.shared.MediaShortCard
import io.github.pushkar.chorplay.ui.components.shared.ShortCardDefaults

private val GridPadding = 16.dp

@Composable
fun SavedShortsGridScreen(
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SavedShortsViewModel = hiltViewModel(),
) {
    val savedShorts by viewModel.savedShorts.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ChorplayTopBar(
                title = stringResource(R.string.library_saved_shorts_label),
                onBack = onBackClick,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (savedShorts.isEmpty()) {
            ChorplayEmptyState(
                modifier = Modifier.padding(padding),
                title = stringResource(R.string.empty_saved_shorts),
                icon = Icons.Default.PlayArrow,
            )
        } else {
            BoxWithConstraints(modifier = Modifier.padding(padding)) {
                val sidePadding = ShortCardDefaults.gridPadding(rememberFeedGridLayout(maxWidth), compactPadding = GridPadding)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(ShortCardDefaults.MinWidth),
                    contentPadding =
                        PaddingValues(
                            start = sidePadding,
                            top = GridPadding,
                            end = sidePadding,
                            bottom = chorBottomContentPadding(GridPadding),
                        ),
                    horizontalArrangement = Arrangement.spacedBy(ShortCardDefaults.Spacing),
                    verticalArrangement = Arrangement.spacedBy(ShortCardDefaults.Spacing),
                ) {
                    items(
                        items = savedShorts,
                        key = Video::id,
                        contentType = { "short" },
                    ) { video ->
                        MediaShortCard(
                            video = video,
                            onClick = { onVideoClick(video.id) },
                            modifier = Modifier.fillMaxSize(),
                            removableFromSavedShorts = true,
                        )
                    }
                }
            }
        }
    }
}
