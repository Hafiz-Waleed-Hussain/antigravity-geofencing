package com.antigravity.geofencing.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GeofenceEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun geofenceDao(): GeofenceDao
}
