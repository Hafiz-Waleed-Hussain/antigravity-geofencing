package com.antigravity.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MviGeofenceReceiver : BroadcastReceiver() {
    companion object {
        var listener: (() -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        val geofencingEvent = com.google.android.gms.location.GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null && !geofencingEvent.hasError()) {
            if (geofencingEvent.geofenceTransition ==
                            com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
            ) {
                listener?.invoke()
            }
        }
    }
}
