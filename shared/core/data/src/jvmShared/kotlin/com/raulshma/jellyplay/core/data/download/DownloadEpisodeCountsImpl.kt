package com.raulshma.jellyplay.core.data.download

import com.raulshma.jellyplay.core.concurrency.runCatchingRethrowingCancellation
import com.raulshma.jellyplay.core.database.dao.DownloadEpisodeCountDao
import com.raulshma.jellyplay.core.database.entity.DownloadEpisodeCountEntity
import com.raulshma.jellyplay.core.datastore.identity.ServerIdentityStore
import com.raulshma.jellyplay.core.model.MediaType
import com.raulshma.jellyplay.core.model.TimeSource
import com.raulshma.jellyplay.core.network.api.LibraryApiClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadEpisodeCountsImpl(
    private val dao: DownloadEpisodeCountDao,
    private val api: LibraryApiClient,
    private val identity: ServerIdentityStore,
    private val clock: TimeSource,
) : DownloadEpisodeCounts {
    private val mutex = Mutex()

    private fun scopeKey(): String? = identity.identity.value.let {
        if (it.activeServerId.isNullOrBlank() || it.activeUserId.isNullOrBlank()) null
        else "${it.activeServerId}:${it.activeUserId}"
    }

    override fun observe(): Flow<Map<String, Int>> = identity.identity
        .map { scopeKey() }.distinctUntilChanged().flatMapLatest { key ->
            if (key == null) flowOf(emptyMap()) else dao.observe(key).map { rows -> rows.associate { it.itemId to it.episodeCount } }
        }

    override suspend fun refresh(seriesId: String): Result<Unit> = runCatchingRethrowingCancellation {
        mutex.withLock {
            val scope = scopeKey() ?: return@withLock
            val now = clock.nowEpochMillis()
            val cached = dao.get(scope, seriesId)
            if (cached != null && now - cached.updatedAt in 0 until 120_000) return@withLock
            val episodes = api.getAllEpisodes(seriesId, isMissing = false).getOrThrow()
                .filter { it.mediaType == MediaType.EPISODE && !it.isVirtual }.distinctBy { it.id }
            if (scope != scopeKey()) return@withLock
            val seasons = episodes.groupBy { it.seasonId }.mapNotNull { (id, items) ->
                id?.takeIf(String::isNotBlank)?.let { DownloadEpisodeCountEntity(scope, it, items.size, now) }
            }
            dao.upsert(seasons + DownloadEpisodeCountEntity(scope, seriesId, episodes.size, now))
        }
    }
}
