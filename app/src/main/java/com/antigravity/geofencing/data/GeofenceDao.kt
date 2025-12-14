package com.antigravity.geofencing.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Query("SELECT * FROM geofence_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<GeofenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(geofence: GeofenceEntity)

    @Delete suspend fun delete(geofence: GeofenceEntity)
}
