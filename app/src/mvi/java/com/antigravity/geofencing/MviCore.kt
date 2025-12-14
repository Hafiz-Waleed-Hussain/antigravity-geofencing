package com.antigravity.geofencing

// User Intents (Actions)
sealed class MviIntent {
    data class SetGeofence(val lat: Double, val lng: Double, val radius: Float) : MviIntent()
    object SilenceAlarm : MviIntent()
    object SnoozeAlarm : MviIntent()
    object GeofenceEntered : MviIntent()
}

// UI State (The single source of truth)
data class MviViewState(
        val status: String = "Idle",
        val isLoading: Boolean = false,
        val isAlarmPlaying: Boolean = false,
        val error: Throwable? = null
)

// Internal Results (Partial Changes)
sealed class MviResult {
    object Processing : MviResult()
    data class SetGeofenceSuccess(val message: String) : MviResult()
    data class Failure(val error: Throwable) : MviResult()
    object AlarmTriggered : MviResult()
    object AlarmSilenced : MviResult()
    object AlarmSnoozed : MviResult()
}
