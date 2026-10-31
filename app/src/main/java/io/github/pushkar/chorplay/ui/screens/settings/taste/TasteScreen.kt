package io.github.pushkar.chorplay.ui.screens.settings.taste

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.backup.BackupOperation
import io.github.pushkar.chorplay.ui.components.settings.SettingsDestination
import io.github.pushkar.chorplay.ui.components.settings.SettingsPage
import io.github.pushkar.chorplay.ui.components.settings.SettingsTarget
import io.github.pushkar.chorplay.ui.components.shared.ChorplayAlertDialog
import io.github.pushkar.chorplay.ui.components.shared.ChorplayLoadingIndicator
import io.github.pushkar.chorplay.ui.components.shared.deepChorStatus
import kotlinx.coroutines.launch
import java.time.LocalDate

private val LoadingHeight = 240.dp

private enum class ResetKind { VIDEO }

@Composable
internal fun TasteScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    onNavigate: (SettingsTarget) -> Unit,
    onOpenRecap: () -> Unit,
    viewModel: TasteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deepChor by viewModel.deepChor.collectAsStateWithLifecycle()
    val operation by viewModel.operation.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmReset by rememberSaveable { mutableStateOf<ResetKind?>(null) }
    val resetDone = stringResource(R.string.taste_reset_done)
    val busy = stringResource(R.string.backup_busy)
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(operation) {
        val message =
            when (val current = operation) {
                is BackupOperation.Succeeded -> current.message
                is BackupOperation.Failed -> current.message
                else -> return@LaunchedEffect
            }
        viewModel.dismissOperation()
        viewModel.reload()
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    LaunchedEffect(notice) {
        notice?.let {
            snackbarHostState.showSnackbar(it)
            notice = null
        }
    }

    val start: (Uri?, (Uri) -> Boolean) -> Unit = { uri, action -> if (uri != null && !action(uri)) notice = busy }
    val exportVideo =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
            start(
                uri,
                viewModel::exportVideoProfile
            )
        }
    val importVideo =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            start(
                uri,
                viewModel::importVideoProfile
            )
        }
    val hiddenLabel = hiddenCountLabel(state.hidden.count)
    val noQueriesLabel = stringResource(R.string.diagnostics_engine_queries_none)
    val startersLabel =
        state.engine
            ?.starters
            ?.takeIf { it.isNotEmpty() }
            ?.let { stringResource(R.string.taste_starter_searches_detail, it.joinToString(", ")) }
    val pausedLabel = stringResource(R.string.taste_learning_paused)
    val pausedStatus = deepChorStatus(deepChor)

    val actions =
        remember(viewModel) {
            TasteActions(
                onTopicPreferred = viewModel::setTopicPreferred,
                onBlockTopic = viewModel::blockTopic,
                onBlockChannel = viewModel::blockChannel,
                onForgetChannel = viewModel::forgetChannel,
                onClearChannelMemory = viewModel::clearChannelMemory,
                onOpenHidden = { onNavigate(SettingsTarget(SettingsDestination.HIDDEN_CONTENT)) },
                onOpenDiscoverChannels = { onNavigate(SettingsTarget(SettingsDestination.DISCOVER_CHANNELS)) },
                onOpenRecap = onOpenRecap,
                onExportVideo = { exportVideo.launch("chorplay_video_profile_${LocalDate.now()}.json") },
                onImportVideo = { importVideo.launch(arrayOf(JSON)) },
                onResetVideo = { confirmReset = ResetKind.VIDEO },
            )
        }

    SettingsPage(
        title = stringResource(R.string.taste_title),
        onBack = onBack,
        highlight = highlight,
        snackbarHostState = snackbarHostState,
    ) {
        if (state.loading) {
            item("taste.loading") { ChorplayLoadingIndicator(Modifier.fillMaxWidth().height(LoadingHeight)) }
        } else {
            pausedStatus?.let { learningPaused(pausedLabel, it) }
            tasteContent(state, hiddenLabel, noQueriesLabel, startersLabel, actions)
        }
    }

    confirmReset?.let { kind ->
        val video = kind == ResetKind.VIDEO
        val title = R.string.reset_profile_dialog_title
        val body = R.string.taste_reset_video_body
        ChorplayAlertDialog(
            onDismissRequest = { confirmReset = null },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetVideoProfile()
                        confirmReset = null
                        notice = resetDone
                    },
                ) { Text(stringResource(R.string.reset_profile_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private const val JSON = "application/json"
