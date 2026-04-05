package com.dailycurator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dailycurator.data.local.entity.CachedInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedInsightDao {
    @Query("SELECT * FROM cached_insights WHERE type = :type LIMIT 1")
    fun observe(type: String): Flow<CachedInsightEntity?>

    @Query("SELECT * FROM cached_insights WHERE type = :type LIMIT 1")
    suspend fun get(type: String): CachedInsightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedInsightEntity)
}
