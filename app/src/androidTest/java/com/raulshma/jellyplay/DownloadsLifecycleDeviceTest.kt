package com.raulshma.jellyplay

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.raulshma.jellyplay.core.data.download.DownloadIntake
import com.raulshma.jellyplay.core.data.download.DownloadQueue
import com.raulshma.jellyplay.core.model.*
import com.raulshma.jellyplay.core.network.api.LibraryApiClient
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.io.File

/** Opt-in real-device fixture; never deletes or overwrites pre-existing downloads. */
class DownloadsLifecycleDeviceTest {
    @Test
    fun validateRequestedStage() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        val stage = args.getString("stage")
        org.junit.Assume.assumeTrue("Opt in with stage=discover or stage=download", stage == "discover" || stage == "download")
        val context = instrumentation.targetContext
        val output = File(context.filesDir, "download-validation").apply { mkdirs() }
        val queue = GlobalContext.get().get<DownloadQueue>()
        val api = GlobalContext.get().get<LibraryApiClient>()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("am start -n ${context.packageName}/com.raulshma.jellyplay.MainActivity"),
        ).use { it.readBytes() }
        withTimeout(30_000) {
            while (api.getLibraryFolders().isFailure) delay(500)
        }
        val existing = queue.getAllDownloadsSnapshot()
        val fixtureFile = File(output, "fixture.json")

        if (stage == "discover") {
            check(!fixtureFile.exists()) { "A fixture already exists; inspect it before starting another run" }
            val existingIds = existing.map { it.mediaItemId }.toSet()
            val existingSeries = existing.mapNotNull { it.seriesId }.toSet()
            val available = api.getMediaItems(
                filters = LibraryFilters(mediaTypes = listOf(MediaType.EPISODE)),
                kindFilter = ItemKindFilter(includeEpisodes = true), limit = 10_000,
            ).getOrThrow().items.filter {
                it.id !in existingIds && it.seriesId !in existingSeries && !it.isVirtual &&
                    it.seriesId != null && it.seasonId != null && (it.runTimeTicks ?: 0) > 0
            }
            val groups = available.groupBy { it.seriesId to it.seasonId }.values.filter { it.size >= 2 }
                .map { it.sortedBy { episode -> episode.runTimeTicks }.take(2) }
                .sortedBy { pair -> pair.sumOf { it.runTimeTicks ?: 0 } }.take(12)
            val candidates = groups.map { pair -> pair.map { api.getMediaDetail(it.id).getOrThrow() } }
                .filter { pair -> pair.all { (it.mediaSources.firstOrNull()?.size ?: 0) in 5_000_000..120_000_000 } }
            val pair = candidates.minByOrNull { items -> items.sumOf { it.mediaSources.first().size ?: 0 } }
                ?: error("No isolated pair of reasonably small episodes found; no downloads started")
            val items = JSONArray()
            pair.forEach { detail ->
                items.put(JSONObject().put("id", detail.item.id).put("name", detail.item.name)
                    .put("seriesId", detail.item.seriesId).put("seriesName", detail.item.seriesName)
                    .put("seasonId", detail.item.seasonId).put("season", detail.item.seasonNumber)
                    .put("episode", detail.item.episodeNumber).put("size", detail.mediaSources.first().size))
            }
            fixtureFile.writeText(JSONObject().put("baselineMediaIds", JSONArray(existingIds.toList()))
                .put("items", items).toString(2))
            return@runBlocking
        }

        check(stage == "download")
        val fixture = JSONObject(fixtureFile.readText())
        val requested = args.getString("itemId") ?: error("Specify the exact fixture itemId")
        val items = fixture.getJSONArray("items")
        check((0 until items.length()).any { items.getJSONObject(it).getString("id") == requested })
        val baseline = fixture.getJSONArray("baselineMediaIds")
        check((0 until baseline.length()).none { baseline.getString(it) == requested })
        check(existing.none { it.mediaItemId == requested }) { "Refusing to replace an existing download" }

        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("input keyevent 3"),
        ).use { it.readBytes() }
        delay(1_000)
        val detail = api.getMediaDetail(requested).getOrThrow()
        val result = GlobalContext.get().get<DownloadIntake>().start(detail)
        val started = result.downloadItem ?: error(result.error ?: "Download did not start")
        File(output, "started-$requested.json").writeText(JSONObject().put("downloadId", started.id)
            .put("mediaItemId", requested).put("path", started.downloadPath).toString(2))
        val samples = JSONArray()
        val completed = withTimeout(240_000) {
            var previous: String? = null
            while (true) {
                val row = queue.getAllDownloadsSnapshot().firstOrNull { it.id == started.id }
                    ?: error("Test download disappeared")
                val signature = "${row.status}:${row.downloadedBytes}"
                if (signature != previous) {
                    samples.put(JSONObject().put("status", row.status.name).put("bytes", row.downloadedBytes)
                        .put("time", System.currentTimeMillis()))
                    File(output, "progress-$requested.json").writeText(samples.toString(2))
                    previous = signature
                }
                check(row.status != DownloadStatus.FAILED) { "Download failed: ${row.errorMessage}" }
                if (row.status == DownloadStatus.COMPLETED) return@withTimeout row
                delay(250)
            }
            error("Unreachable")
        }
        val file = File(completed.downloadPath)
        assertTrue(file.isFile)
        assertEquals(completed.downloadedBytes, file.length())
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            assertTrue((retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0) > 0)
        } finally { retriever.release() }
        File(output, "completed-$requested.json").writeText(JSONObject().put("downloadId", completed.id)
            .put("bytes", file.length()).put("validMedia", true).put("backgroundStart", true).toString(2))
    }
}
