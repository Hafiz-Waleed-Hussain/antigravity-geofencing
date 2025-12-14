package com.antigravity.geofencing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlowState(
        val status: String = "Idle",
        val isAlarmVisible: Boolean = false,
        val navigateToHistory: Boolean = false
)

class FlowViewModel : ViewModel() {
    private val _state = MutableStateFlow(FlowState())
    val state: StateFlow<FlowState> = _state.asStateFlow()

    fun setGeofence(requestId: String, lat: Double, lng: Double, radius: Float) {
        viewModelScope.launch {
            val entity =
                    com.antigravity.geofencing.data.GeofenceEntity(
                            requestId = requestId,
                            latitude = lat,
                            longitude = lng,
                            radius = radius
                    )
            com.antigravity.geofencing.data.GeofenceRepository.getDao().insert(entity)
            _state.value = _state.value.copy(status = "Geofence Active (Flow) for $lat, $lng")
        }
    }

    fun onHistoryClicked() {
        _state.value = _state.value.copy(navigateToHistory = true)
    }

    fun onHistoryNavigated() {
        _state.value = _state.value.copy(navigateToHistory = false)
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
