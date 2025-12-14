package com.antigravity.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MachineReceiver : BroadcastReceiver() {
    companion object {
        var eventBus: (() -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        android.widget.Toast.makeText(
                        context,
                        "Machine Broadcast!",
                        android.widget.Toast.LENGTH_SHORT
                )
                .show()
        // AI Note: In production, consider EventBus or GlobalScope flow emission
        eventBus?.invoke()
    }
}
