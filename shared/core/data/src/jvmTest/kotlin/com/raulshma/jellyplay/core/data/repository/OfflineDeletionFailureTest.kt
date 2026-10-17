package com.raulshma.jellyplay.core.data.repository

import com.raulshma.jellyplay.core.database.JellyPlayDatabase
import com.raulshma.jellyplay.core.database.dao.*
import com.raulshma.jellyplay.core.database.entity.DownloadEntity
import io.mockk.mockk
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.*

class OfflineDeletionFailureTest {
    @Test
    fun `an undeletable media path never removes its database records`() = runTest {
        val dir = Files.createTempDirectory("download-delete-failure").toFile()
        val nonEmpty = java.io.File(dir, "media.mkv").apply { mkdirs() }
        java.io.File(nonEmpty, "held-file").writeText("preserve")
        try {
            val database = mockk<JellyPlayDatabase>(relaxed = true)
            val downloads = mockk<DownloadDao>(relaxed = true)
            val core = OfflineDeletionCore(database, downloads, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
            val item = DownloadEntity("download", "media", "Fixture", "EPISODE", nonEmpty.absolutePath, "", 8, 8, "COMPLETED")
            var metadataDeleted = false
            assertFailsWith<java.io.IOException> { core.delete(listOf(item), { metadataDeleted = true }) }
            assertFalse(metadataDeleted)
            assertTrue(java.io.File(nonEmpty, "held-file").exists())
            coVerify(exactly = 0) { downloads.deleteDownloadsByIds(any()) }
        } finally { dir.deleteRecursively() }
    }
}
