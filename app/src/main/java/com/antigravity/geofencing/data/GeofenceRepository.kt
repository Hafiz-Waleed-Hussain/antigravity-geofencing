package com.antigravity.geofencing.data

import android.content.Context
import androidx.room.Room

object GeofenceRepository {
    private var database: AppDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            database =
                    Room.databaseBuilder(
                                    context.applicationContext,
                                    AppDatabase::class.java,
                                    "geofence-db"
                            )
                            .fallbackToDestructiveMigration()
                            .build()
        }
    }

    fun getDao(): GeofenceDao {
        return database?.geofenceDao() ?: throw IllegalStateException("Repository not initialized")
    }
}
