package com.antigravity.geofencing

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MvvmViewModel(application: Application) : AndroidViewModel(application) {

    // Two-way binding fields
    val latitude = MutableLiveData<String>()
    val longitude = MutableLiveData<String>()
    val radius = MutableLiveData<String>("100.0")

    private val _status = MutableLiveData<String>("Idle")
    val status: LiveData<String> = _status

    private val _isAlarmVisible = MutableLiveData<Boolean>(false)
    val isAlarmVisible: LiveData<Boolean> = _isAlarmVisible

    // SingleLiveEvent pattern (simplified here using a wrapper or just observing)
    // For simplicity, we expose events via LiveData
    private val _geofenceRequest = MutableLiveData<Triple<Double, Double, Float>?>()
    val geofenceRequest: LiveData<Triple<Double, Double, Float>?> = _geofenceRequest

    private val handler = Handler(Looper.getMainLooper())
    private var snoozeRunnable: Runnable? = null

    fun onSetGeofenceClicked() {
        val lat = latitude.value?.toDoubleOrNull()
        val lng = longitude.value?.toDoubleOrNull()
        val rad = radius.value?.toFloatOrNull()

        if (lat != null && lng != null && rad != null) {
            _geofenceRequest.value = Triple(lat, lng, rad)
            _status.value = "Setting Geofence..."
        } else {
            _status.value = "Invalid Input"
        }
    }

    fun onGeofenceSetSuccess() {
        _status.value = "Geofence Active!"
    }

    fun onGeofenceSetFailure(error: String) {
        _status.value = "Error: $error"
    }

    fun onGeofenceEntered() {
        _isAlarmVisible.value = true
        _status.value = "ENTERED ZONE!"
    }

    fun onSilenceClicked() {
        _isAlarmVisible.value = false
        _status.value = "Task Complete."
    }

    fun onSnoozeClicked() {
        _isAlarmVisible.value = false
        _status.value = "Snoozed..."

        snoozeRunnable = Runnable {
            _isAlarmVisible.value = true
            _status.value = "Snooze Over!"
        }
        handler.postDelayed(snoozeRunnable!!, 10 * 60 * 1000)
    }
}
