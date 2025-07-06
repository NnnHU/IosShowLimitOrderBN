package com.pythonn.androidshowlimitorderbn.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CacheMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: CacheMetadataEntity)
    
    @Query("SELECT * FROM cache_metadata WHERE key = :key")
    suspend fun getMetadata(key: String): CacheMetadataEntity?
    
    @Query("SELECT COUNT(*) > 0 FROM cache_metadata WHERE key = :key AND lastIncrementalUpdate > :threshold AND isValid = 1")
    suspend fun isCacheValid(key: String, threshold: Long): Boolean
    
    @Query("UPDATE cache_metadata SET lastIncrementalUpdate = :timestamp WHERE key = :key")
    suspend fun updateIncrementalTimestamp(key: String, timestamp: Long)
    
    @Query("UPDATE cache_metadata SET lastFullUpdate = :timestamp, dataVersion = :version WHERE key = :key")
    suspend fun updateFullUpdateInfo(key: String, timestamp: Long, version: Long)
    
    @Query("UPDATE cache_metadata SET isValid = :isValid WHERE key = :key")
    suspend fun updateCacheValidity(key: String, isValid: Boolean)
    
    @Query("DELETE FROM cache_metadata WHERE key = :key")
    suspend fun deleteMetadata(key: String)
    
    @Query("SELECT * FROM cache_metadata WHERE isValid = 1")
    suspend fun getAllValidCache(): List<CacheMetadataEntity>
}