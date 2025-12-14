package com.antigravity.geofencing

import android.app.Application
import com.antigravity.geofencing.data.GeofenceRepository

class GeofenceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GeofenceRepository.initialize(this)
    }
}
