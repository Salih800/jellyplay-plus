package com.raulshma.jellyplay.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.raulshma.jellyplay.core.database.entity.DownloadEpisodeCountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadEpisodeCountDao {
    @Query("SELECT * FROM download_episode_counts WHERE scopeKey = :scopeKey")
    fun observe(scopeKey: String): Flow<List<DownloadEpisodeCountEntity>>

    @Query("SELECT * FROM download_episode_counts WHERE scopeKey = :scopeKey AND itemId = :itemId")
    suspend fun get(scopeKey: String, itemId: String): DownloadEpisodeCountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rows: List<DownloadEpisodeCountEntity>)
}
