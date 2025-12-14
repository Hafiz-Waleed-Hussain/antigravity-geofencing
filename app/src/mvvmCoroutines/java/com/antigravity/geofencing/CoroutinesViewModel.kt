package com.antigravity.geofencing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoroutinesViewModel : ViewModel() {

    private val _status = MutableLiveData<String>("Idle")
    val status: LiveData<String> = _status

    private val _isAlarmPlaying = MutableLiveData<Boolean>(false)
    val isAlarmPlaying: LiveData<Boolean> = _isAlarmPlaying

    fun setGeofence(lat: Double, lng: Double, radius: Float) {
        viewModelScope.launch {
            _status.value = "Setting Geofence (Async)..."
            delay(500) // Simulate generic async work
            _status.value = "Geofence Active!"
        }
    }

    fun onGeofenceEntered() {
        _isAlarmPlaying.value = true
        _status.value = "ALARM! Worker Triggered."
    }

    fun stopAlarm() {
        _isAlarmPlaying.value = false
        _status.value = "Alarm Stopped."
        // In real world, cancel worker
    }

    fun snoozeAlarm() {
        viewModelScope.launch {
            _isAlarmPlaying.value = false
            _status.value = "Snoozed 10s..."
            delay(10000)
            _isAlarmPlaying.value = true
            _status.value = "Snooze Over!"
        }
    }
}
