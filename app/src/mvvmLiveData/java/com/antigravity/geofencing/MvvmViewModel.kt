package com.antigravity.geofencing

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MvvmViewModel(application: Application) : AndroidViewModel(application) {

    // Two-way binding fields
    val latitude = MutableLiveData<String>("37.4220")
    val longitude = MutableLiveData<String>("-122.0841")
    val radius = MutableLiveData<String>("100.0")

    private val _status = MutableLiveData<String>("Idle")
    val status: LiveData<String> = _status

    private val _isAlarmVisible = MutableLiveData<Boolean>(false)
    val isAlarmVisible: LiveData<Boolean> = _isAlarmVisible

    private val _currentLocation = MutableLiveData<String>("Waiting for GPS...")
    val currentLocation: LiveData<String> = _currentLocation

    private val _navigateToHistory = MutableLiveData<Boolean>(false)
    val navigateToHistory: LiveData<Boolean> = _navigateToHistory

    fun updateCurrentLocation(lat: Double, lng: Double) {
        _currentLocation.value = "Current: $lat, $lng"
    }

    fun onHistoryClicked() {
        _navigateToHistory.value = true
    }

    fun onHistoryNavigated() {
        _navigateToHistory.value = false
    }

    // SingleLiveEvent pattern (simplified here using a wrapper or just observing)
    // For simplicity, we expose events via LiveData
    // Updated to include RequestID
    private val _geofenceRequest = MutableLiveData<DataRequest?>()
    val geofenceRequest: LiveData<DataRequest?> = _geofenceRequest

    data class DataRequest(val lat: Double, val lng: Double, val rad: Float, val requestId: String)

    private val handler = Handler(Looper.getMainLooper())
    private var snoozeRunnable: Runnable? = null

    fun onSetGeofenceClicked() {
        val lat = latitude.value?.toDoubleOrNull()
        val lng = longitude.value?.toDoubleOrNull()
        val rad = radius.value?.toFloatOrNull()

        if (lat != null && lng != null && rad != null) {
            val requestId = java.util.UUID.randomUUID().toString()
            _geofenceRequest.value = DataRequest(lat, lng, rad, requestId)
            _status.value = "Setting Geofence..."
        } else {
            _status.value = "Invalid Input"
        }
    }

    // Pass requestId back or just capture it via local scope if possible?
    // Actually, ViewModel doesn't know when success happens exactly relative to which request if
    // async.
    // But here we assume linear flow.
    // Better: Helper function to save
    fun onGeofenceSetSuccess(requestId: String, lat: Double, lng: Double, rad: Float) {
        _status.value = "Geofence Added!"

        // Save to DB
        viewModelScope.launch {
            val entity =
                    com.antigravity.geofencing.data.GeofenceEntity(
                            requestId = requestId,
                            latitude = lat,
                            longitude = lng,
                            radius = rad
                    )
            com.antigravity.geofencing.data.GeofenceRepository.getDao().insert(entity)
        }
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

    fun onPermissionRequired() {
        _status.value = "Waiting for Permissions..."
    }
}
