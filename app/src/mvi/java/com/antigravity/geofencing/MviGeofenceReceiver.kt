package com.antigravity.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MviGeofenceReceiver : BroadcastReceiver() {
    companion object {
        var listener: (() -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        // Assume valid for demo
        listener?.invoke()
    }
}
