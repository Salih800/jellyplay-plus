package com.raulshma.jellyplay.feature.downloads

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.DotsVertical
import com.raulshma.jellyplay.feature.downloads.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Keep the selection count readable on phones, even with mixed download states. */
@Composable
internal fun DownloadSelectionMenu(
    canPause: Boolean,
    canResume: Boolean,
    canCancel: Boolean,
    onAction: (DownloadBulkAction) -> Unit,
) {
    if (!canPause && !canResume && !canCancel) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Tabler.Outline.DotsVertical, stringResource(Res.string.downloads_selection_actions))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (canPause) DropdownMenuItem(
                text = { Text(stringResource(Res.string.downloads_action_pause)) },
                onClick = { expanded = false; onAction(DownloadBulkAction.PAUSE) },
            )
            if (canResume) DropdownMenuItem(
                text = { Text(stringResource(Res.string.downloads_action_resume)) },
                onClick = { expanded = false; onAction(DownloadBulkAction.RESUME) },
            )
            if (canCancel) DropdownMenuItem(
                text = { Text(stringResource(Res.string.downloads_action_cancel)) },
                onClick = { expanded = false; onAction(DownloadBulkAction.CANCEL) },
            )
        }
    }
}
