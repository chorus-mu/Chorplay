package io.github.pushkar.chorplay.ui.components.layout.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.pushkar.chorplay.R

/**
 * Which screens show the app-shell actions ([ChorplayGlobalActions]).
 *
 * [Auto] is the default and resolves from the bar's own shape: a bar with no back affordance is a
 * root destination, and every root destination must be able to reach Notifications and Settings.
 * A new root tab therefore cannot forget them — it would have to opt out with [None].
 */
enum class ChorplayGlobalActionsMode {
    Auto,
    Always,
    None,
}

/**
 * The single top bar for every non-player screen.
 *
 * Pass [onBack] for a detail screen; leave it null for a root tab. See [ChorplayTopBarDefaults] for why
 * the colours and window insets differ from the Material defaults.
 *
 * @param leading replaces the default back button — used by Home for its logo + wordmark. A bar
 *   with a [leading] slot is still treated as a root destination.
 * @param actions screen-specific actions. Keep this to two on a root destination; put the rest in
 *   [ChorplayTopBarOverflow] so the title keeps its width.
 * @param trailingAction drawn after the global actions, last in the bar: Home's profile avatar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChorplayTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    globalActions: ChorplayGlobalActionsMode = ChorplayGlobalActionsMode.Auto,
    windowInsets: WindowInsets = ChorplayTopBarDefaults.WindowInsets,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    ChorplayTopBar(
        modifier = modifier,
        title = {
            ChorplayTopBarTitle(title = title, subtitle = subtitle)
        },
        onBack = onBack,
        leading = leading,
        actions = actions,
        globalActions = globalActions,
        windowInsets = windowInsets,
        trailingAction = trailingAction,
    )
}

/**
 * Slot-based overload for bars whose title is not plain text — a branded wordmark, a tab row, or an
 * inline field. Prefer the string overload; this exists so such screens still share the container,
 * colours, insets and global-action behaviour instead of hand-rolling a bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChorplayTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    globalActions: ChorplayGlobalActionsMode = ChorplayGlobalActionsMode.Auto,
    windowInsets: WindowInsets = ChorplayTopBarDefaults.WindowInsets,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val showGlobalActions =
        when (globalActions) {
            ChorplayGlobalActionsMode.Always -> true
            ChorplayGlobalActionsMode.None -> false
            ChorplayGlobalActionsMode.Auto -> onBack == null
        }

    TopAppBar(
        modifier = modifier,
        title = title,
        navigationIcon = {
            when {
                leading != null -> {
                    leading()
                }

                onBack != null -> {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                        )
                    }
                }
            }
        },
        actions = {
            actions()
            if (showGlobalActions) {
                ChorplayGlobalActionsRow()
            }
            trailingAction?.invoke()
        },
        colors = ChorplayTopBarDefaults.colors(),
        windowInsets = windowInsets,
    )
}

@Composable
private fun ChorplayTopBarTitle(
    title: String,
    subtitle: String?,
) {
    if (subtitle == null) {
        Text(
            text = title,
            style = ChorplayTopBarDefaults.titleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        Column {
            Text(
                text = title,
                style = ChorplayTopBarDefaults.titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = ChorplayTopBarDefaults.subtitleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
