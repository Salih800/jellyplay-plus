package com.raulshma.jellyplay.core.data.download

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Cached counts of actual server episodes, keyed by series or season id. */
interface DownloadEpisodeCounts {
    fun observe(): Flow<Map<String, Int>>
    suspend fun refresh(seriesId: String): Result<Unit>
}

object EmptyDownloadEpisodeCounts : DownloadEpisodeCounts {
    override fun observe(): Flow<Map<String, Int>> = flowOf(emptyMap())
    override suspend fun refresh(seriesId: String): Result<Unit> = Result.success(Unit)
}
