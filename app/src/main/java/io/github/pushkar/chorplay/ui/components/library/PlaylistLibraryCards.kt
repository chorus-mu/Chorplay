package io.github.pushkar.chorplay.ui.components.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.pushkar.chorplay.R

@Composable
internal fun PlaylistActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDownload: (() -> Unit)?,
    onRename: (() -> Unit)?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        if (onDownload != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.download)) },
                onClick = {
                    onDismiss()
                    onDownload()
                },
                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
            )
        }
        if (onRename != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rename)) },
                onClick = {
                    onDismiss()
                    onRename()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete)) },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
        )
    }
}
