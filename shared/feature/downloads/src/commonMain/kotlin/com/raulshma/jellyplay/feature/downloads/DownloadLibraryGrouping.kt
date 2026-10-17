package com.raulshma.jellyplay.feature.downloads

import com.raulshma.jellyplay.core.model.DownloadItem
import com.raulshma.jellyplay.core.model.MediaType
import com.raulshma.jellyplay.core.model.OfflineMediaItem
import com.raulshma.jellyplay.feature.downloads.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import androidx.compose.ui.state.ToggleableState

internal fun downloadGroupSelectionState(ids: Collection<String>, selectedIds: Set<String>): ToggleableState = when {
    ids.isEmpty() || ids.none { it in selectedIds } -> ToggleableState.Off
    ids.all { it in selectedIds } -> ToggleableState.On
    else -> ToggleableState.Indeterminate
}

/** Presentation categories shared by the queue and the offline library. */
enum class OfflineLibraryFilter(val labelRes: StringResource) {
    ALL(Res.string.downloads_filter_all),
    MOVIES(Res.string.downloads_filter_movies),
    SERIES(Res.string.downloads_filter_series),
    MUSIC(Res.string.downloads_filter_music),
    BOOKS(Res.string.downloads_filter_books),
    OTHER(Res.string.downloads_filter_other);

    fun accepts(type: MediaType): Boolean = this == ALL || this == categoryOf(type)

    companion object {
        fun categoryOf(type: MediaType): OfflineLibraryFilter = when (type) {
            MediaType.MOVIE -> MOVIES
            MediaType.SERIES, MediaType.SEASON, MediaType.EPISODE -> SERIES
            MediaType.AUDIO, MediaType.MUSIC, MediaType.ALBUM, MediaType.ARTIST -> MUSIC
            MediaType.BOOK -> BOOKS
            else -> OTHER
        }
    }
}

internal data class EpisodePosition(
    val id: String,
    val seriesId: String?,
    val seriesName: String?,
    val seasonId: String?,
    val seasonName: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
) {
    // Legacy rows without a series id can still group by their stored name.
    // Completely unidentified rows stay separate rather than becoming one series.
    val seriesKey: String get() = seriesId?.takeIf { it.isNotBlank() }?.let { "series:$it" }
        ?: seriesName?.takeIf { it.isNotBlank() }?.let { "series-name:$it" }
        ?: "episode:$id"
    val seasonKey: String get() = seasonNumber?.let { "number:$it" }
        ?: seasonId?.takeIf { it.isNotBlank() }?.let { "id:$it" }
        ?: "unknown"
}

internal val DownloadItem.episodePosition: EpisodePosition
    get() = EpisodePosition(id, seriesId, seriesName, seasonId, seasonName, seasonNumber, episodeNumber)

internal val OfflineMediaItem.episodePosition: EpisodePosition
    get() = EpisodePosition(id, seriesId, seriesName, seasonId, seasonName, seasonNumber, episodeNumber)

internal data class DownloadSeason<T>(
    val key: String,
    val number: Int?,
    val name: String?,
    val episodes: List<T>,
)

/** Numeric order is independent of the title-level sort and download date. */
internal fun <T> downloadedSeasons(
    episodes: List<T>,
    position: (T) -> EpisodePosition,
): List<DownloadSeason<T>> = episodes.map { it to position(it) }
    .groupBy { (_, location) -> location.seasonKey }
    .map { (key, rows) ->
        val location = rows.first().second
        DownloadSeason(
            key = key,
            number = location.seasonNumber,
            name = rows.firstNotNullOfOrNull { it.second.seasonName?.takeIf(String::isNotBlank) },
            episodes = rows.sortedWith(
                compareBy<Pair<T, EpisodePosition>> { it.second.episodeNumber ?: Int.MAX_VALUE }
                    .thenBy { it.second.id },
            ).map { it.first },
        )
    }.sortedWith(compareBy<DownloadSeason<T>> { it.number ?: Int.MAX_VALUE }.thenBy { it.key })

internal sealed interface DownloadLibraryRow {
    val key: String

    data class Category(val category: OfflineLibraryFilter) : DownloadLibraryRow {
        override val key = "category:${category.name}"
    }
    data class Series(
        override val key: String,
        val name: String,
        val downloads: List<DownloadItem>,
    ) : DownloadLibraryRow
    data class Season(
        override val key: String,
        val season: DownloadSeason<DownloadItem>,
    ) : DownloadLibraryRow
    data class Item(val download: DownloadItem) : DownloadLibraryRow {
        override val key = "download:${download.id}"
    }
}

internal fun downloadLibraryRows(
    downloads: List<DownloadItem>,
    filter: OfflineLibraryFilter,
    expandedKeys: Collection<String>,
): List<DownloadLibraryRow> = buildList {
    val categories = if (filter == OfflineLibraryFilter.ALL) {
        OfflineLibraryFilter.entries.filter { it != OfflineLibraryFilter.ALL }
    } else listOf(filter)
    for (category in categories) {
        val items = downloads.filter { category.accepts(it.mediaType) }
        if (items.isEmpty()) continue
        if (filter == OfflineLibraryFilter.ALL) add(DownloadLibraryRow.Category(category))
        if (category != OfflineLibraryFilter.SERIES) {
            items.forEach { add(DownloadLibraryRow.Item(it)) }
            continue
        }
        for ((key, episodes) in items.groupBy { it.episodePosition.seriesKey }) {
            add(DownloadLibraryRow.Series(key, episodes.first().seriesName ?: episodes.first().name, episodes))
            if (key !in expandedKeys) continue
            val seasons = downloadedSeasons(episodes) { it.episodePosition }
            for (season in seasons) {
                val seasonKey = "$key/season:${season.key}"
                if (seasons.size > 1) {
                    add(DownloadLibraryRow.Season(seasonKey, season))
                    if (seasonKey !in expandedKeys) continue
                }
                season.episodes.forEach { add(DownloadLibraryRow.Item(it)) }
            }
        }
    }
}

internal sealed interface OfflineLibraryRow {
    val key: String
    data class Category(val category: OfflineLibraryFilter) : OfflineLibraryRow {
        override val key = "category:${category.name}"
    }
    data class Series(val item: OfflineMediaItem) : OfflineLibraryRow {
        override val key = "series:${item.id}"
    }
    data class Season(override val key: String, val season: DownloadSeason<OfflineMediaItem>) : OfflineLibraryRow
    data class Item(val item: OfflineMediaItem) : OfflineLibraryRow {
        override val key = "item:${item.id}"
    }
    data class Loading(override val key: String) : OfflineLibraryRow
}

internal fun offlineLibraryRows(
    items: List<OfflineMediaItem>,
    filter: OfflineLibraryFilter,
    expandedKeys: Collection<String>,
    episodesBySeries: Map<String, List<OfflineMediaItem>>,
): List<OfflineLibraryRow> = buildList {
    val categories = if (filter == OfflineLibraryFilter.ALL) {
        OfflineLibraryFilter.entries.filter { it != OfflineLibraryFilter.ALL }
    } else listOf(filter)
    for (category in categories) {
        val categoryItems = items.filter { category.accepts(it.mediaType) }
        if (categoryItems.isEmpty()) continue
        if (filter == OfflineLibraryFilter.ALL) add(OfflineLibraryRow.Category(category))
        for (item in categoryItems) {
            if (item.mediaType != MediaType.SERIES) {
                add(OfflineLibraryRow.Item(item))
                continue
            }
            val key = "series:${item.id}"
            add(OfflineLibraryRow.Series(item))
            if (key !in expandedKeys) continue
            val episodes = episodesBySeries[item.id]
            if (episodes == null) {
                add(OfflineLibraryRow.Loading("$key/loading"))
                continue
            }
            val seasons = downloadedSeasons(episodes) { it.episodePosition }
            for (season in seasons) {
                val seasonKey = "$key/season:${season.key}"
                if (seasons.size > 1) {
                    add(OfflineLibraryRow.Season(seasonKey, season))
                    if (seasonKey !in expandedKeys) continue
                }
                season.episodes.forEach { add(OfflineLibraryRow.Item(it)) }
            }
        }
    }
}
