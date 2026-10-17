package com.raulshma.jellyplay.core.database.entity

import androidx.room3.Entity

/** Last successful server episode count, isolated by server and user. */
@Entity(tableName = "download_episode_counts", primaryKeys = ["scopeKey", "itemId"])
data class DownloadEpisodeCountEntity(
    val scopeKey: String,
    val itemId: String,
    val episodeCount: Int,
    val updatedAt: Long,
)
