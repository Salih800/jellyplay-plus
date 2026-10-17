package com.raulshma.jellyplay.feature.downloads

import com.raulshma.jellyplay.core.data.offline.OfflineDeleteActions
import com.raulshma.jellyplay.core.concurrency.runCatchingRethrowingCancellation
import com.raulshma.jellyplay.core.data.repository.OfflineRepository
import com.raulshma.jellyplay.core.data.repository.UserDataMutator
import com.raulshma.jellyplay.core.data.download.DownloadQueue
import com.raulshma.jellyplay.core.data.download.DownloadEpisodeCounts
import com.raulshma.jellyplay.core.data.download.EmptyDownloadEpisodeCounts
import com.raulshma.jellyplay.core.ui.viewmodel.ConfirmationHost
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import com.raulshma.jellyplay.core.model.MediaItem
import com.raulshma.jellyplay.core.model.matchesOfflineQuery
import com.raulshma.jellyplay.core.model.OfflineMediaItem
import com.raulshma.jellyplay.core.ui.viewmodel.JellyPlayViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import com.raulshma.jellyplay.feature.downloads.generated.resources.Res
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_sort_name
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_sort_rating
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_sort_recent
import com.raulshma.jellyplay.feature.downloads.generated.resources.downloads_sort_size

/** How the downloaded library is sorted. */
enum class OfflineLibrarySort(val labelRes: StringResource) {
    RECENT(Res.string.downloads_sort_recent),
    NAME(Res.string.downloads_sort_name),
    RATING(Res.string.downloads_sort_rating),
    SIZE(Res.string.downloads_sort_size),
}

data class StorageSummary(val totalBytes: Long, val itemCount: Int)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class OfflineLibraryViewModel(
    private val offlineRepository: OfflineRepository,
    private val userDataMutator: UserDataMutator,
    private val seriesEpisodes: com.raulshma.jellyplay.core.data.repository.OfflineSeriesEpisodes,
    private val queue: DownloadQueue,
    private val episodeCounts: DownloadEpisodeCounts = EmptyDownloadEpisodeCounts,
) : JellyPlayViewModel() {

    val episodeTotals = episodeCounts.observe().stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    private val _selectedItems = MutableStateFlow<Map<String, OfflineMediaItem>>(emptyMap())
    val selectedItems = _selectedItems.asStateFlow()
    private val _selectionGroups = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val selectionGroups = _selectionGroups.asStateFlow()
    private val _actionInProgress = MutableStateFlow(false)
    val actionInProgress = _actionInProgress.asStateFlow()
    val pendingBulkDelete = ConfirmationHost<List<OfflineMediaItem>>()
    private val messageChannel = Channel<DownloadsUserMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    fun refreshEpisodeTotals(seriesIds: Set<String>) {
        launch { seriesIds.forEach { episodeCounts.refresh(it) } }
    }

    fun clearSelection() { if (!_actionInProgress.value) _selectedItems.value = emptyMap() }

    fun toggleSelection(item: OfflineMediaItem) = toggleGroupSelection(listOf(item))

    fun toggleGroupSelection(items: List<OfflineMediaItem>, groupKey: String? = null) {
        if (_actionInProgress.value || items.isEmpty()) return
        val candidates = items.associateBy { it.id }
        if (groupKey != null) _selectionGroups.update { it + (groupKey to candidates.keys) }
        _selectedItems.update { selected ->
            if (selected.keys.containsAll(candidates.keys)) selected - candidates.keys else selected + candidates
        }
    }

    fun toggleSeriesSelection(seriesId: String) {
        if (_actionInProgress.value) return
        launch {
            val items = offlineRepository.getEpisodesForSeries(seriesId).filter { it.downloadStatus != null }
            toggleGroupSelection(items, "series:$seriesId")
        }
    }

    fun selectAllVisible() {
        if (_actionInProgress.value) return
        launch {
            val items = offlineLibrary.value.flatMap {
                if (it.mediaType == com.raulshma.jellyplay.core.model.MediaType.SERIES) offlineRepository.getEpisodesForSeries(it.id)
                else listOf(it)
            }.filter { it.downloadStatus != null }
            if (_actionInProgress.value) return@launch
            _selectedItems.value = items.associateBy { it.id }
            _selectionGroups.update { groups ->
                groups + items.filter { it.seriesId != null }.groupBy { "series:${it.seriesId}" }
                    .mapValues { (_, episodes) -> episodes.map { it.id }.toSet() }
            }
        }
    }

    fun deleteSelected(items: List<OfflineMediaItem>) {
        if (_actionInProgress.value || items.isEmpty()) return
        _actionInProgress.value = true
        launch {
            try {
                val wanted = items.map { it.id }.toSet()
                val downloads = runCatchingRethrowingCancellation { queue.getAllDownloadsSnapshot() }.getOrElse {
                    messageChannel.trySend(DownloadsUserMessage.OperationFailed(items.size))
                    return@launch
                }.filter { it.mediaItemId in wanted }
                val failed = mutableSetOf<String>()
                downloads.forEach { item ->
                    if (runCatchingRethrowingCancellation { queue.deleteDownload(item.id).getOrThrow() }.isFailure) failed += item.mediaItemId
                }
                _selectedItems.update { selected -> selected.filterKeys { it !in wanted || it in failed } }
                val removed = wanted - failed
                _selectionGroups.update { groups -> groups.mapValues { (_, ids) -> ids - removed } }
                if (failed.isNotEmpty()) messageChannel.trySend(DownloadsUserMessage.OperationFailed(failed.size))
                else if (queue.isSupported) messageChannel.trySend(DownloadsUserMessage.Deleted)
            } finally { _actionInProgress.value = false }
        }
    }

    private val _isLoading = composeState(true)
    val isLoading: Boolean get() = _isLoading.value

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(value: String) { _query.value = value }

    private val _sort = MutableStateFlow(OfflineLibrarySort.RECENT)
    val sort: StateFlow<OfflineLibrarySort> = _sort.asStateFlow()
    fun setSort(value: OfflineLibrarySort) { _sort.value = value }

    private val _filter = MutableStateFlow(OfflineLibraryFilter.ALL)
    val filter: StateFlow<OfflineLibraryFilter> = _filter.asStateFlow()
    fun setFilter(value: OfflineLibraryFilter) { _filter.value = value }

    /**
     * Single subscription to the raw offline library; both [offlineLibrary] and
     * [storageSummary] derive from this shared upstream so the Room query and
     * the per-entity mapping run once per change instead of twice.
     */
    private val rawLibrary: StateFlow<List<OfflineMediaItem>> =
        offlineRepository.getOfflineLibrary()
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    /**
     * Library items after applying the active search query, filter and sort.
     * The query is debounced so typing doesn't re-filter on every keystroke.
     */
    val offlineLibrary: StateFlow<List<OfflineMediaItem>> =
        combine(
            rawLibrary,
            _query.debounce(180),
            combine(_sort, _filter) { s, f -> s to f },
        ) { items, query, (sort, filter) ->
            _isLoading.value = false
            // Filter/sort of the full offline library stays off the Main
            // dispatcher — only a concern for very large libraries, but free.
            withContext(Dispatchers.Default) {
                applyQueryFilterAndSort(items, query, filter, sort)
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Total storage used by all completed downloads, plus the item count. */
    val storageSummary: StateFlow<StorageSummary> =
        rawLibrary.map { items ->
            StorageSummary(totalBytes = items.sumOf { it.totalSizeBytes }, itemCount = items.size)
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StorageSummary(0L, 0),
        )

    private fun applyQueryFilterAndSort(
        items: List<OfflineMediaItem>,
        query: String,
        filter: OfflineLibraryFilter,
        sort: OfflineLibrarySort,
    ): List<OfflineMediaItem> {
        // The type partition and the query-field match are the shared
        // OfflineShelf rules — the same partition the home's mode filter uses,
        // the same field set the repository's searchOffline matches.
        val filtered = items.filter { filter.accepts(it.mediaType) }
        val q = query.trim()
        val matched = if (q.length < 2) {
            filtered
        } else {
            filtered.filter { it.matchesOfflineQuery(q) }
        }
        return when (sort) {
            OfflineLibrarySort.RECENT -> matched.sortedByDescending { it.createdAt }
            OfflineLibrarySort.NAME -> matched.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            OfflineLibrarySort.RATING -> matched.sortedByDescending { it.communityRating ?: -1f }
            OfflineLibrarySort.SIZE -> matched.sortedByDescending { it.totalSizeBytes }
        }
    }

    /** Observe only the expanded series; no whole-library episode window. */
    fun episodesForSeries(seriesId: String) = seriesEpisodes.observeEpisodesForSeries(seriesId)

    fun expandedSeriesEpisodes(seriesIds: Set<String>): kotlinx.coroutines.flow.Flow<Map<String, List<OfflineMediaItem>>> =
        if (seriesIds.isEmpty()) flowOf(emptyMap()) else {
            combine(seriesIds.map { id -> episodesForSeries(id).map { id to it } }) { it.toMap() }
        }

    /**
     * Toggles played state for a downloaded item from the long-press quick-action
     * sheet. Routes through [UserDataMutator.setPlayed] (silent — the reactive
     * [offlineLibrary] Room flow refreshes on its own) so the change is applied
     * to the local offline DB AND enqueued into the playback outbox for server
     * sync on reconnect (or pushed immediately when online).
     */
    fun markItemPlayed(item: MediaItem, played: Boolean) {
        launch {
            userDataMutator.setPlayed(item.id, played)
        }
    }

    /**
     * Toggles favorite for a downloaded item. Routes through
     * [UserDataMutator.setFavorite], so it works fully offline: the flip is
     * applied to the local offline store immediately and staged in the playback
     * outbox for delivery on reconnect. Silent — Room refreshes the badge.
     */
    fun toggleFavorite(item: MediaItem) {
        launch { userDataMutator.setFavorite(item.id) }
    }

    /**
     * Shared offline-delete module (core/data) — the same routing the detail
     * screen and offline home use. Quick-action providers default to empty;
     * explicit batch selection uses [queue] instead. `onContentMutated` stays a no-op: the
     * reactive [offlineLibrary] Room flow refreshes on its own once rows are
     * gone.
     */
    private val deleteActions = OfflineDeleteActions(
        scope = scope,
        offlineRepository = offlineRepository,
    )

    /**
     * Deletes a downloaded item from the long-press quick-action sheet:
     * [OfflineDeleteActions.deleteDownload] routes a series to the whole-series
     * delete and anything else to the single-item delete.
     */
    fun delete(item: MediaItem) {
        deleteActions.deleteDownload(item)
    }
}
