package com.raulshma.jellyplay.feature.downloads

import com.raulshma.jellyplay.core.model.DownloadItem
import com.raulshma.jellyplay.core.model.DownloadStatus
import com.raulshma.jellyplay.core.model.MediaType
import com.raulshma.jellyplay.core.model.OfflineMediaItem
import kotlin.test.*

class DownloadLibraryGroupingTest {
    @Test
    fun `group checkboxes distinguish none partial and complete selection`() {
        val ids = listOf("e1", "e2")
        assertEquals(androidx.compose.ui.state.ToggleableState.Off, downloadGroupSelectionState(ids, emptySet()))
        assertEquals(androidx.compose.ui.state.ToggleableState.Indeterminate, downloadGroupSelectionState(ids, setOf("e1")))
        assertEquals(androidx.compose.ui.state.ToggleableState.On, downloadGroupSelectionState(ids, setOf("e1", "e2", "movie")))
    }
    private fun episode(id: String, season: Int? = 1, number: Int? = 1, series: String? = "show") = DownloadItem(
        id = id, mediaItemId = id, name = "Episode $id", mediaType = MediaType.EPISODE,
        downloadPath = "/$id.mkv", downloadUrl = "", totalSizeBytes = 100, downloadedBytes = 100,
        status = DownloadStatus.COMPLETED, seriesId = series, seriesName = "Same title",
        seasonNumber = season, episodeNumber = number,
    )

    private fun List<DownloadLibraryRow>.episodeIds() = filterIsInstance<DownloadLibraryRow.Item>().map { it.download.id }

    @Test
    fun `all separates movies series music and books while preserving standalone order`() {
        val items = listOf(
            episode("e"),
            episode("m2").copy(mediaType = MediaType.MOVIE),
            episode("m1").copy(mediaType = MediaType.MOVIE),
            episode("a").copy(mediaType = MediaType.AUDIO),
            episode("b").copy(mediaType = MediaType.BOOK),
            episode("unknown").copy(mediaType = MediaType.UNKNOWN),
        )
        val rows = downloadLibraryRows(items, OfflineLibraryFilter.ALL, emptyList())
        assertEquals(
            listOf(OfflineLibraryFilter.MOVIES, OfflineLibraryFilter.SERIES, OfflineLibraryFilter.MUSIC,
                OfflineLibraryFilter.BOOKS, OfflineLibraryFilter.OTHER),
            rows.filterIsInstance<DownloadLibraryRow.Category>().map { it.category },
        )
        assertEquals(listOf("m2", "m1", "a", "b", "unknown"), rows.episodeIds())
        assertEquals(1, rows.filterIsInstance<DownloadLibraryRow.Series>().size)
        assertEquals(rows.size, rows.map { it.key }.toSet().size)
    }

    @Test
    fun `single season opens directly and uses numeric episode order`() {
        val items = listOf(episode("ten", number = 10), episode("two", number = 2), episode("one", number = 1))
        val rows = downloadLibraryRows(items, OfflineLibraryFilter.SERIES, listOf("series:show"))
        assertEquals(listOf("one", "two", "ten"), rows.episodeIds())
        assertTrue(rows.none { it is DownloadLibraryRow.Season })
    }

    @Test
    fun `multiple seasons have independent expandable groups in numeric order`() {
        val items = listOf(episode("s2e2", 2, 2), episode("s1e2", 1, 2), episode("s2e1", 2, 1), episode("s1e1", 1, 1))
        val onlySeriesOpen = downloadLibraryRows(items, OfflineLibraryFilter.SERIES, listOf("series:show"))
        val seasons = onlySeriesOpen.filterIsInstance<DownloadLibraryRow.Season>()
        assertEquals(listOf(1, 2), seasons.map { it.season.number })
        assertTrue(onlySeriesOpen.episodeIds().isEmpty())
        val expanded = listOf("series:show") + seasons.map { it.key }
        val rows = downloadLibraryRows(items, OfflineLibraryFilter.SERIES, expanded)
        assertEquals(listOf("s1e1", "s1e2", "s2e1", "s2e2"), rows.episodeIds())
        val oneSeason = downloadLibraryRows(items, OfflineLibraryFilter.SERIES, expanded - seasons[1].key)
        assertEquals(listOf("s1e1", "s1e2"), oneSeason.episodeIds())
    }

    @Test
    fun `series identity takes precedence over equal display names`() {
        val rows = downloadLibraryRows(listOf(episode("a", series = "a"), episode("b", series = "b")), OfflineLibraryFilter.SERIES, emptyList())
        assertEquals(listOf("series:a", "series:b"), rows.filterIsInstance<DownloadLibraryRow.Series>().map { it.key })
    }

    @Test
    fun `specials and missing numbers remain visible with unknown episodes last`() {
        val rows = downloadedSeasons(listOf(
            episode("unknown", null, null), episode("special", 0, 1),
            episode("no-number", 1, null), episode("normal", 1, 2),
        )) { it.episodePosition }
        assertEquals(listOf(0, 1, null), rows.map { it.number })
        assertEquals(listOf("normal", "no-number"), rows[1].episodes.map { it.id })
    }

    @Test
    fun `legacy rows group by name but completely anonymous episodes stay separate`() {
        val legacy = listOf(episode("a", series = null), episode("b", series = null))
        assertEquals(1, downloadLibraryRows(legacy, OfflineLibraryFilter.SERIES, emptyList()).size)
        val anonymous = legacy.map { it.copy(seriesName = null) }
        assertEquals(2, downloadLibraryRows(anonymous, OfflineLibraryFilter.SERIES, emptyList()).size)
    }

    @Test
    fun `grouping keeps paused failed queued and completed items`() {
        val items = DownloadStatus.entries.mapIndexed { i, status -> episode(status.name, number = i).copy(status = status) }
        val rows = downloadLibraryRows(items, OfflineLibraryFilter.SERIES, listOf("series:show"))
        assertEquals(DownloadStatus.entries.toSet(), rows.filterIsInstance<DownloadLibraryRow.Item>().map { it.download.status }.toSet())
    }

    @Test
    fun `offline groups preserve title order and sort only their episodes`() {
        val series = listOf(
            OfflineMediaItem("b", "B", MediaType.SERIES),
            OfflineMediaItem("a", "A", MediaType.SERIES),
        )
        val episodes = mapOf("b" to listOf(
            OfflineMediaItem("b2", "Last downloaded first", MediaType.EPISODE, seriesId = "b", seasonNumber = 1, episodeNumber = 2),
            OfflineMediaItem("b1", "First", MediaType.EPISODE, seriesId = "b", seasonNumber = 1, episodeNumber = 1),
        ))
        val rows = offlineLibraryRows(series, OfflineLibraryFilter.SERIES, listOf("series:b"), episodes)
        assertEquals(listOf("b", "a"), rows.filterIsInstance<OfflineLibraryRow.Series>().map { it.item.id })
        assertEquals(listOf("b1", "b2"), rows.filterIsInstance<OfflineLibraryRow.Item>().map { it.item.id })
        assertTrue(rows.none { it is OfflineLibraryRow.Season })
        assertTrue(rows.none { it is OfflineLibraryRow.Loading })
    }

    @Test
    fun `offline expanded series waits for its own episode flow`() {
        val series = listOf(OfflineMediaItem("s", "Show", MediaType.SERIES))
        val pending = offlineLibraryRows(series, OfflineLibraryFilter.SERIES, listOf("series:s"), emptyMap())
        assertTrue(pending.any { it is OfflineLibraryRow.Loading })
        val loaded = offlineLibraryRows(series, OfflineLibraryFilter.SERIES, listOf("series:s"), mapOf("s" to emptyList()))
        assertTrue(loaded.none { it is OfflineLibraryRow.Loading })
    }
}
