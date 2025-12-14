package com.antigravity.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class CoroutinesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null && !geofencingEvent.hasError()) {
            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                // Start Worker
                val request = OneTimeWorkRequestBuilder<GeofenceWorker>().build()
                WorkManager.getInstance(context).enqueue(request)
            }
        }
    }
}
