package com.raulshma.jellyplay.feature.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.focusable
import com.raulshma.jellyplay.core.model.MediaType
import com.raulshma.jellyplay.core.ui.components.JellyPlayCircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import com.raulshma.jellyplay.core.ui.components.SelectionActionBar
import com.raulshma.jellyplay.core.ui.components.clearFloatingNav
import com.raulshma.jellyplay.core.ui.message.LocalUserMessageBus
import org.jetbrains.compose.resources.getString
import com.raulshma.jellyplay.feature.downloads.generated.resources.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.tabler.Tabler
import com.composables.icons.tabler.outline.ArrowDown
import com.composables.icons.tabler.outline.DeviceFloppy
import com.composables.icons.tabler.outline.Download
import com.composables.icons.tabler.outline.Search
import com.composables.icons.tabler.outline.X
import com.raulshma.jellyplay.core.designsystem.theme.ShapeCache
import com.raulshma.jellyplay.core.model.MediaItem
import com.raulshma.jellyplay.core.model.toMediaItem
import com.raulshma.jellyplay.core.model.MediaQuickActionScope
import com.raulshma.jellyplay.core.model.formatBytes
import com.raulshma.jellyplay.core.ui.adaptive.LocalAdaptiveInfo
import com.raulshma.jellyplay.core.ui.adaptive.bottomPadding
import com.raulshma.jellyplay.core.ui.adaptive.contentPadding
import com.raulshma.jellyplay.core.ui.components.JellyPlayScreenScaffold
import com.raulshma.jellyplay.core.ui.components.LocalMediaQuickActionController
import com.raulshma.jellyplay.core.ui.components.OfflineMediaCard
import com.raulshma.jellyplay.core.ui.components.QuickActionAdapter
import com.raulshma.jellyplay.core.ui.components.QuickActionIntakeHost
import com.raulshma.jellyplay.core.ui.components.ScreenEmptyState
import com.raulshma.jellyplay.core.ui.components.rememberQuickActionIntake
import com.raulshma.jellyplay.core.ui.components.ScreenLoadingState
import com.raulshma.jellyplay.core.ui.tv.LocalTvMode
import com.raulshma.jellyplay.core.ui.tv.TvFocusableGrid
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.raulshma.jellyplay.feature.downloads.generated.resources.Res
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_clear_cd
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_item_count
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_library_title
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_no_downloaded_content
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_no_items_in_category
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_no_matches
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_on_device
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_search_cd
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_search_downloads_placeholder
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_sort_cd

@Composable
fun OfflineLibraryScreen(
    onSeriesClick: (seriesId: String) -> Unit,
    onItemClick: (itemId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: OfflineLibraryViewModel = koinViewModel(),
) {
    val items by viewModel.offlineLibrary.collectAsStateWithLifecycle(initialValue = emptyList())
    val isLoading = viewModel.isLoading
    val storageSummary by viewModel.storageSummary.collectAsStateWithLifecycle(
        initialValue = StorageSummary(0L, 0),
    )
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedItems by viewModel.selectedItems.collectAsStateWithLifecycle()
    val selectionGroups by viewModel.selectionGroups.collectAsStateWithLifecycle()
    val actionInProgress by viewModel.actionInProgress.collectAsStateWithLifecycle()
    val episodeTotals by viewModel.episodeTotals.collectAsStateWithLifecycle()
    val selectedIds = selectedItems.keys
    val selectionMode = selectedIds.isNotEmpty()
    val seriesIds = remember(items) { items.filter { it.mediaType == MediaType.SERIES }.map { it.id }.toSet() }
    LaunchedEffect(seriesIds) { viewModel.refreshEpisodeTotals(seriesIds) }
    val messageBus = LocalUserMessageBus.current
    LaunchedEffect(viewModel, messageBus) {
        viewModel.messages.collect { message ->
            when (message) {
                DownloadsUserMessage.Deleted -> messageBus.info(getString(Res.string.downloads_deleted_message))
                is DownloadsUserMessage.OperationFailed -> messageBus.error(getString(Res.string.downloads_operation_failed, message.count))
                is DownloadsUserMessage.Raw -> messageBus.error(message.text)
            }
        }
    }
    var expandedKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val expandedSeriesIds = remember(items, expandedKeys) {
        items.filter { it.mediaType == MediaType.SERIES && "series:${it.id}" in expandedKeys }
            .map { it.id }.toSet()
    }
    val episodeFlow = remember(viewModel, expandedSeriesIds) { viewModel.expandedSeriesEpisodes(expandedSeriesIds) }
    val episodesBySeries by episodeFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val rows = remember(items, filter, expandedKeys, episodesBySeries) {
        offlineLibraryRows(items, filter, expandedKeys, episodesBySeries)
    }
    val toggleGroup: (String) -> Unit = { key ->
        expandedKeys = if (key in expandedKeys) expandedKeys - key else expandedKeys + key
    }
    val isTv = LocalTvMode.current
    val adaptiveInfo = LocalAdaptiveInfo.current
    val contentPad = adaptiveInfo.contentPadding(isTv)

    var searchOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchOpen) {
        if (searchOpen) searchFocus.requestFocus()
    }

    // Long-press quick actions, on the shared intake (core/ui — see
    // QuickActionIntake). Removal goes through the shared confirm dialog,
    // like every other host. Everything in this grid is downloaded, so the
    // sheet offers mark-watched / favorite / delete / view-details and routing
    // always lands on the offline detail screens (never the online page).
    val quickActionIntake = rememberQuickActionIntake(
        scope = MediaQuickActionScope.LIBRARY,
        includeRemoveDownload = true,
        includeFavorite = true,
        adapter = remember(viewModel, onItemClick, onSeriesClick) {
            // PLAY and DETAILS both open the offline detail/series screen,
            // which owns the Play button and full offline metadata.
            val openOfflineItem: (MediaItem) -> Unit = { item ->
                if (item.mediaType == com.raulshma.jellyplay.core.model.MediaType.SERIES) {
                    onSeriesClick(item.id)
                } else {
                    onItemClick(item.id)
                }
            }
            QuickActionAdapter(
                onPlay = openOfflineItem,
                onOpenDetail = openOfflineItem,
                onMarkPlayed = viewModel::markItemPlayed,
                onToggleFavorite = viewModel::toggleFavorite,
                onRemoveDownload = viewModel::delete,
            )
        },
    )

    JellyPlayScreenScaffold(
        title = stringResource(Res.string.downloads_library_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = {
                searchOpen = !searchOpen
            }) {
                Icon(if (searchOpen) Tabler.Outline.X else Tabler.Outline.Search, contentDescription = stringResource(Res.string.downloads_search_cd))
            }
            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(Tabler.Outline.ArrowDown, contentDescription = stringResource(Res.string.downloads_sort_cd))
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    OfflineLibrarySort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(option.labelRes),
                                    fontWeight = if (option == sort) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                viewModel.setSort(option)
                                sortMenuOpen = false
                            },
                        )
                    }
                }
            }
        },
    ) {
        if (isLoading) {
            ScreenLoadingState()
        } else if (storageSummary.itemCount == 0 && query.isBlank()) {
            ScreenEmptyState(
                icon = Tabler.Outline.Download,
                title = stringResource(Res.string.downloads_no_downloaded_content),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                // Storage summary.
                if (storageSummary.itemCount > 0) {
                    StorageHeader(
                        totalBytes = storageSummary.totalBytes,
                        itemCount = storageSummary.itemCount,
                        modifier = Modifier.padding(horizontal = contentPad, vertical = 8.dp),
                    )
                }

                // Expandable search field.
                AnimatedVisibility(
                    visible = searchOpen,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::setQuery,
                        placeholder = { Text(stringResource(Res.string.downloads_search_downloads_placeholder)) },
                        leadingIcon = { Icon(Tabler.Outline.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(Tabler.Outline.X, contentDescription = stringResource(Res.string.downloads_clear_cd))
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentPad, vertical = 4.dp)
                            .focusRequester(searchFocus),
                    )
                }

                DownloadCategoryTabs(
                    selected = filter,
                    enabled = !actionInProgress,
                    onSelect = { viewModel.clearSelection(); viewModel.setFilter(it) },
                    modifier = Modifier.padding(horizontal = contentPad),
                )

                Spacer(Modifier.height(8.dp))

                Box(Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ScreenEmptyState(
                            icon = Tabler.Outline.Search,
                            title = if (query.isNotBlank()) stringResource(Res.string.downloads_no_matches) else stringResource(Res.string.downloads_no_items_in_category),
                        )
                    }
                } else {
                    TvFocusableGrid(
                        items = rows,
                        key = { it.key },
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(
                            start = contentPad,
                            end = contentPad,
                            top = 8.dp,
                            bottom = adaptiveInfo.bottomPadding(isTv) + if (selectionMode) 72.dp else 0.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentType = { it::class },
                        span = { row ->
                            GridItemSpan(if (row is OfflineLibraryRow.Item && row.item.mediaType != MediaType.EPISODE) 1 else maxLineSpan)
                        },
                        modifier = Modifier.fillMaxSize(),
                    ) { _, row, itemModifier ->
                        CompositionLocalProvider(LocalMediaQuickActionController provides quickActionIntake.controller) {
                            when (row) {
                                is OfflineLibraryRow.Category -> Text(
                                    stringResource(row.category.labelRes),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = itemModifier.padding(top = 12.dp).focusable(enabled = isTv),
                                )
                                is OfflineLibraryRow.Series -> DownloadGroupHeader(
                                    title = row.item.name,
                                    count = row.item.downloadedEpisodeCount ?: 0,
                                    serverTotal = episodeTotals[row.item.id],
                                    selectionState = downloadGroupSelectionState(
                                        selectionGroups[row.key] ?: episodesBySeries[row.item.id].orEmpty().map { it.id }.toSet(), selectedIds,
                                    ),
                                    onToggleSelection = { viewModel.toggleSeriesSelection(row.item.id) },
                                    selectionEnabled = !actionInProgress,
                                    totalBytes = row.item.downloadedBytes,
                                    expanded = row.key in expandedKeys,
                                    onClick = { toggleGroup(row.key) },
                                    onDetails = { onSeriesClick(row.item.id) },
                                    onLongClick = { viewModel.toggleSeriesSelection(row.item.id) },
                                    modifier = itemModifier,
                                )
                                is OfflineLibraryRow.Season -> DownloadGroupHeader(
                                    title = downloadSeasonTitle(row.season.number, row.season.name),
                                    count = row.season.episodes.count { it.downloadStatus == com.raulshma.jellyplay.core.model.DownloadStatus.COMPLETED },
                                    serverTotal = row.season.episodes.firstOrNull()?.seasonId?.let { episodeTotals[it] },
                                    selectionState = downloadGroupSelectionState(row.season.episodes.map { it.id }, selectedIds),
                                    onToggleSelection = { viewModel.toggleGroupSelection(row.season.episodes, row.key) },
                                    onLongClick = { viewModel.toggleGroupSelection(row.season.episodes, row.key) },
                                    selectionEnabled = !actionInProgress,
                                    totalBytes = row.season.episodes.sumOf { it.downloadedBytes },
                                    expanded = row.key in expandedKeys,
                                    onClick = { toggleGroup(row.key) },
                                    modifier = itemModifier.padding(start = 12.dp),
                                )
                                is OfflineLibraryRow.Item -> {
                                    if (row.item.mediaType == MediaType.EPISODE) {
                                        OfflineEpisodeRow(
                                            row.item, onClick = { onItemClick(row.item.id) }, modifier = itemModifier,
                                            selected = if (selectionMode) row.item.id in selectedIds else null,
                                            onToggleSelection = { viewModel.toggleSelection(row.item) },
                                        )
                                    } else {
                                        Box(itemModifier) {
                                            OfflineMediaCard(
                                                item = row.item,
                                                onClick = { if (selectionMode) viewModel.toggleSelection(row.item) else onItemClick(row.item.id) },
                                            )
                                            if (selectionMode) Checkbox(
                                                checked = row.item.id in selectedIds,
                                                onCheckedChange = { viewModel.toggleSelection(row.item) },
                                                enabled = !actionInProgress,
                                                modifier = Modifier.align(Alignment.TopStart),
                                            )
                                        }
                                    }
                                }
                                is OfflineLibraryRow.Loading -> Box(
                                    modifier = itemModifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) { JellyPlayCircularProgressIndicator() }
                            }
                        }
                    }
                }
                if (selectionMode) {
                    SelectionActionBar(
                        countLabel = stringResource(Res.string.downloads_selected_count, selectedIds.size),
                        selectedCount = selectedIds.size,
                        selectAllLabel = stringResource(Res.string.downloads_action_select_all),
                        clearLabel = stringResource(Res.string.downloads_action_clear_selection),
                        onSelectAll = viewModel::selectAllVisible,
                        onClear = viewModel::clearSelection,
                        actionInProgress = actionInProgress,
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .padding(horizontal = contentPad).clearFloatingNav(),
                        actions = { enabled ->
                            FilledTonalButton(
                                onClick = { viewModel.pendingBulkDelete.show(selectedItems.values.toList()) },
                                enabled = enabled,
                            ) { Text(stringResource(Res.string.downloads_delete)) }
                        },
                    )
                }
                }
            }
        }
    }
    OfflineLibrarySelectionDialog(viewModel)
    // Quick-action sheet + remove-download confirm — the shared intake hosts
    // both (removal only ever deletes the local download; the server copy is
    // untouched).
    QuickActionIntakeHost(quickActionIntake)
}

@Composable
private fun StorageHeader(
    totalBytes: Long,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(ShapeCache.smooth16)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Icon(
            Tabler.Outline.DeviceFloppy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = pluralStringResource(Res.plurals.downloads_item_count, itemCount, itemCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
        if (totalBytes > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.downloads_on_device, totalBytes.formatBytes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
