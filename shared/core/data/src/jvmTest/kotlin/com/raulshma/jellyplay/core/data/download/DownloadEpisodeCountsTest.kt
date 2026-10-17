package com.raulshma.jellyplay.core.data.download

import com.raulshma.jellyplay.core.database.dao.DownloadEpisodeCountDao
import com.raulshma.jellyplay.core.database.entity.DownloadEpisodeCountEntity
import com.raulshma.jellyplay.core.datastore.identity.ServerIdentity
import com.raulshma.jellyplay.core.datastore.identity.ServerIdentityStore
import com.raulshma.jellyplay.core.model.*
import com.raulshma.jellyplay.core.network.api.LibraryApiClient
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class DownloadEpisodeCountsTest {
    private val dao = mockk<DownloadEpisodeCountDao>(relaxed = true)
    private val api = mockk<LibraryApiClient>()
    private val identity = mockk<ServerIdentityStore>()
    private val state = MutableStateFlow(ServerIdentity("server", "user"))
    private val clock = mockk<TimeSource>()
    private val store: DownloadEpisodeCountsImpl

    init {
        every { identity.identity } returns state
        every { clock.nowEpochMillis() } returns 1_000_000L
        coEvery { dao.get(any(), any()) } returns null
        store = DownloadEpisodeCountsImpl(dao, api, identity, clock)
    }

    @Test
    fun `counts only distinct actual episodes and persists series and season totals`() = runTest {
        val episode = MediaItem(id = "one", name = "One", mediaType = MediaType.EPISODE, seasonId = "s1")
        coEvery { api.getAllEpisodes("show", false) } returns Result.success(listOf(
            episode, episode,
            episode.copy(id = "two", seasonId = "s2"),
            episode.copy(id = "missing", isVirtual = true),
        ))
        assertTrue(store.refresh("show").isSuccess)
        coVerify { dao.upsert(match { rows ->
            rows.associate { it.itemId to it.episodeCount } == mapOf("show" to 2, "s1" to 1, "s2" to 1) &&
                rows.all { it.scopeKey == "server:user" }
        }) }
    }

    @Test
    fun `offline refresh failure preserves the last successful counts`() = runTest {
        val saved = DownloadEpisodeCountEntity("server:user", "show", 26, 10)
        every { dao.observe("server:user") } returns MutableStateFlow(listOf(saved))
        coEvery { api.getAllEpisodes("show", false) } returns Result.failure(java.io.IOException("offline"))
        assertEquals(mapOf("show" to 26), store.observe().first())
        assertTrue(store.refresh("show").isFailure)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `fresh snapshot avoids repeatedly fetching the same series`() = runTest {
        coEvery { dao.get("server:user", "show") } returns DownloadEpisodeCountEntity("server:user", "show", 26, 999_000)
        assertTrue(store.refresh("show").isSuccess)
        coVerify(exactly = 0) { api.getAllEpisodes(any(), any()) }
    }

    @Test
    fun `account change during a request cannot write counts into another account`() = runTest {
        coEvery { api.getAllEpisodes("show", false) } coAnswers {
            state.value = ServerIdentity("server", "other-user")
            Result.success(emptyList())
        }
        assertTrue(store.refresh("show").isSuccess)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }
}
