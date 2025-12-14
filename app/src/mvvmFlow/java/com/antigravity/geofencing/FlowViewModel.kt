package com.antigravity.geofencing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlowState(val status: String = "Idle", val isAlarmVisible: Boolean = false)

class FlowViewModel : ViewModel() {
    private val _state = MutableStateFlow(FlowState())
    val state: StateFlow<FlowState> = _state.asStateFlow()

    fun setGeofence(lat: Double, lng: Double, radius: Float) {
        _state.value = _state.value.copy(status = "Geofence Active (Flow) for $lat, $lng")
    }

    fun onGeofenceEntered() {
        _state.value = _state.value.copy(status = "ALARM! FLOW DETECTED!", isAlarmVisible = true)
    }

    fun silence() {
        _state.value = _state.value.copy(status = "Silenced", isAlarmVisible = false)
    }

    fun snooze() {
        viewModelScope.launch {
            _state.value = _state.value.copy(status = "Snoozed...", isAlarmVisible = false)
            delay(10000)
            _state.value = _state.value.copy(status = "Snooze Over!", isAlarmVisible = true)
        }
    }
}
