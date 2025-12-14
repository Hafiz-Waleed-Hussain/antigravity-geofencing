package com.antigravity.geofencing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FlowReceiver : BroadcastReceiver() {
    companion object {
        var eventCallback: (() -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        eventCallback?.invoke()
    }
}
