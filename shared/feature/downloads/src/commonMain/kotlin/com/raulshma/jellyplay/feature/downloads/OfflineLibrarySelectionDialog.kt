package com.raulshma.jellyplay.feature.downloads

import androidx.compose.runtime.Composable
import com.raulshma.jellyplay.core.ui.components.ConfirmDialog
import com.raulshma.jellyplay.core.ui.components.ConfirmTone
import com.raulshma.jellyplay.feature.downloads.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OfflineLibrarySelectionDialog(viewModel: OfflineLibraryViewModel) {
    viewModel.pendingBulkDelete.item?.let { items ->
        ConfirmDialog(
            title = stringResource(Res.string.downloads_delete_downloads_title),
            message = pluralStringResource(Res.plurals.downloads_delete_downloads_message, items.size, items.size),
            confirmText = stringResource(Res.string.downloads_delete),
            dismissText = stringResource(Res.string.downloads_cancel),
            tone = ConfirmTone.DESTRUCTIVE,
            onConfirm = { viewModel.pendingBulkDelete.confirm()?.let(viewModel::deleteSelected) },
            onDismiss = { viewModel.pendingBulkDelete.dismiss(inFlight = false) },
        )
    }
}
