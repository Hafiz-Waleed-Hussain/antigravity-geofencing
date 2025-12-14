package com.antigravity.geofencing.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofence_history")
data class GeofenceEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val requestId: String, // Unique ID for GeofencingClient
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
        val timestamp: Long = System.currentTimeMillis()
)
