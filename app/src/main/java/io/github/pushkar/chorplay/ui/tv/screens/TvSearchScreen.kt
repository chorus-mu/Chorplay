package io.github.pushkar.chorplay.ui.tv.screens

import android.app.Activity
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.local.HomeFeedColumns
import io.github.pushkar.chorplay.data.local.availableWith
import io.github.pushkar.chorplay.data.model.Channel
import io.github.pushkar.chorplay.data.model.Playlist
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.ui.components.search.SearchSuggestionsPanel
import io.github.pushkar.chorplay.ui.components.shared.ChorplaySearchField
import io.github.pushkar.chorplay.ui.components.shared.quickactions.sharedQuickActionsViewModel
import io.github.pushkar.chorplay.ui.screens.search.SearchResultsBody
import io.github.pushkar.chorplay.ui.screens.search.SearchViewModel
import io.github.pushkar.chorplay.ui.screens.search.launchVoiceSearch
import io.github.pushkar.chorplay.ui.screens.search.rememberSearchState
import io.github.pushkar.chorplay.ui.screens.search.rememberSearchSubmitter
import io.github.pushkar.chorplay.ui.tv.components.TvScreenFrame
import io.github.pushkar.chorplay.ui.tv.theme.LocalTvDimens

@Composable
fun TvSearchScreen(
    viewModel: SearchViewModel,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dimens = LocalTvDimens.current
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsState()
    val state = rememberSearchState(viewModel)
    val quickActions = sharedQuickActionsViewModel()
    val submitVideo = rememberSearchSubmitter(viewModel, state, quickActions)
    val pagingItems = viewModel.searchResults.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val text = state.textFieldState

    fun submit() {
        softwareKeyboard?.hide()
        val query = text.text.toString()
        submitVideo(query, uiState.filters)
    }

    val voiceLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf(String::isNotBlank)
                ?.let { spoken ->
                    text.setTextAndPlaceCursorAtEnd(spoken)
                    submit()
                }
        }

    TvScreenFrame(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = SearchBarHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChorplaySearchField(
                    state = text,
                    placeholder = stringResource(R.string.search_videos_channels_placeholder),
                    onSearch = { submit() },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { launchVoiceSearch(context, voiceLauncher::launch) }) {
                    Icon(imageVector = Icons.Rounded.Mic, contentDescription = stringResource(R.string.voice_search_cd))
                }
            }
            if (uiState.query.isBlank() || state.isTyping) {
                SearchSuggestionsPanel(
                    query = state.query,
                    history = state.matchingHistory,
                    suggestions = state.suggestions,
                    onSubmit = { suggestion ->
                        text.setTextAndPlaceCursorAtEnd(suggestion)
                        submitVideo(suggestion, uiState.filters)
                    },
                    onHistorySelect = { item ->
                        text.setTextAndPlaceCursorAtEnd(item.query)
                        submitVideo(item.query, item.filters?.availableWith(false) ?: uiState.filters)
                    },
                    onFill = text::setTextAndPlaceCursorAtEnd,
                    onDeleteHistoryItem = state::deleteHistoryItem,
                    onClearHistory = state::clearHistory,
                )
            } else {
                SearchResultsBody(
                    viewModel = viewModel,
                    filters = uiState.filters,
                    shortsEnabled = false,
                    isGridMode = false,
                    feedColumns = HomeFeedColumns.AUTO,
                    pagingItems = pagingItems,
                    gridState = gridState,
                    quickActions = quickActions,
                    onVideoClick = onVideoClick,
                    onChannelClick = onChannelClick,
                    onPlaylistClick = onPlaylistClick,
                    onShortsQueue = {},
                    dismissKeyboard = state::stopTyping,
                    maxAutoColumns = dimens.feedMaxColumns,
                    minCellWidth = dimens.feedCardMinWidth,
                )
            }
        }
    }
}

private val SearchBarHorizontalPadding = 16.dp
