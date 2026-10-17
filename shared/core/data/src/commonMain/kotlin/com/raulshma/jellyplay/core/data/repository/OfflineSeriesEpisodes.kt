package com.raulshma.jellyplay.core.data.repository

import com.raulshma.jellyplay.core.model.OfflineMediaItem
import kotlinx.coroutines.flow.Flow

/** Reactive, uncapped episodes of one series for expandable download groups. */
interface OfflineSeriesEpisodes {
    fun observeEpisodesForSeries(seriesId: String): Flow<List<OfflineMediaItem>>
}
