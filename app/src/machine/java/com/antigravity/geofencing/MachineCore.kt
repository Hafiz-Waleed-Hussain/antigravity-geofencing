package com.antigravity.geofencing

import kotlinx.coroutines.flow.StateFlow

/**
 * AI_CONTEXT: Core Domain Definitions These interfaces define the capabilities required by the
 * system. They are designed to be implementation-agnostic and easily mocked.
 */

// Value Types
data class GeoCoordinate(val latitude: Double, val longitude: Double)

data class GeofenceConfig(
        val center: GeoCoordinate,
        val radiusMeters: Float,
        val expireInMs: Long = -1
)

// System Actions
interface GeofenceSystem {
    /**
     * Registers a geofence with the underlying OS.
     * @param config The geofence configuration.
     * @return Result of the operation.
     */
    suspend fun addGeofence(config: GeofenceConfig): Result<Unit>

    /** Removes all geofences. */
    suspend fun removeAllGeofences(): Result<Unit>
}

interface AlarmSystem {
    fun playAlarm()
    fun stopAlarm()
    fun scheduleSnooze(durationMs: Long)
}

// UI State
data class MachineState(
        val statusMessage: String = "System Idle",
        val alarmActive: Boolean = false,
        val logs: List<String> = emptyList()
)

// Business Logic Component
interface MachineController {
    val state: StateFlow<MachineState>
    fun processInput(lat: String, lng: String, rad: String)
    fun onGeofenceTriggered()
    fun onSilenceRequested()
    fun onSnoozeRequested()
}
