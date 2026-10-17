package com.raulshma.jellyplay.core.database.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/** Adds an independent cache; existing downloads and offline metadata stay intact. */
val MIGRATION_60_61 = object : Migration(60, 61) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `download_episode_counts` (`scopeKey` TEXT NOT NULL, `itemId` TEXT NOT NULL, `episodeCount` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`scopeKey`, `itemId`))")
    }
}
