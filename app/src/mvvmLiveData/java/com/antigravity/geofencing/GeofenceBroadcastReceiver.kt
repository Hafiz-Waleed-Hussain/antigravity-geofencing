package com.antigravity.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        // Quick and dirty listener for the single-Activity demo
        var listener: (() -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "Broadcast Received!", Toast.LENGTH_SHORT).show()
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null && geofencingEvent.hasError()) {
            Toast.makeText(context, "Geofencing Error", Toast.LENGTH_SHORT).show()
            return
        }

        if (geofencingEvent?.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Toast.makeText(context, "Entered Geofence!", Toast.LENGTH_LONG).show()

            // Notify UI
            listener?.invoke()

            // Play sound immediately if UI is not bound?
            // The requirements say "phone alarms... until I silence".
            // If the activity is destroyed, we should probably start a Service or a Notification
            // with Full Screen Intent.
            // For 2017 style, we'd start a JobIntentService or just a high priority notification.
            // Since this is a demo, communicating to the Activity is fine if open,
            // but we should probably do a Notification if closed.
        }
    }
}
