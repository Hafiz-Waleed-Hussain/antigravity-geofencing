package com.antigravity.geofencing

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * IMPLEMENTATION: Concrete versions of the core interfaces. Uses Android APIs (Play Services, etc).
 */
class AndroidGeofenceSystem(private val context: Context) : GeofenceSystem {

    private val client = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, MachineReceiver::class.java)
        intent.action = "com.antigravity.geofencing.ACTION_MACHINE_EVENT"
        PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission") // Caller responsibility to check perms
    override suspend fun addGeofence(config: GeofenceConfig): Result<Unit> {
        return try {
            val geofence =
                    Geofence.Builder()
                            .setRequestId("MACHINE_ID")
                            .setCircularRegion(
                                    config.center.latitude,
                                    config.center.longitude,
                                    config.radiusMeters
                            )
                            .setExpirationDuration(
                                    if (config.expireInMs > 0) config.expireInMs
                                    else Geofence.NEVER_EXPIRE
                            )
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                            .build()

            val request =
                    GeofencingRequest.Builder()
                            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                            .addGeofence(geofence)
                            .build()

            client.addGeofences(request, pendingIntent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeAllGeofences(): Result<Unit> {
        return try {
            client.removeGeofences(pendingIntent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class MachineControllerImpl(private val geofenceSystem: GeofenceSystem) :
        ViewModel(), MachineController {

    private val _state = MutableStateFlow(MachineState())
    override val state: StateFlow<MachineState> = _state.asStateFlow()

    override fun processInput(lat: String, lng: String, rad: String) {
        viewModelScope.launch {
            val dLat = lat.toDoubleOrNull()
            val dLng = lng.toDoubleOrNull()
            val fRad = rad.toFloatOrNull()

            if (dLat == null || dLng == null || fRad == null) {
                _state.value = _state.value.copy(statusMessage = "Error: Invalid Input")
                return@launch
            }

            _state.value = _state.value.copy(statusMessage = "Registering Geofence...")

            val result = geofenceSystem.addGeofence(GeofenceConfig(GeoCoordinate(dLat, dLng), fRad))

            if (result.isSuccess) {
                _state.value = _state.value.copy(statusMessage = "Geofence Active.")
            } else {
                _state.value =
                        _state.value.copy(
                                statusMessage = "Error: ${result.exceptionOrNull()?.message}"
                        )
            }
        }
    }

    override fun onGeofenceTriggered() {
        _state.value = _state.value.copy(alarmActive = true, statusMessage = "ALARM TRIGGERED!")
    }

    override fun onSilenceRequested() {
        _state.value = _state.value.copy(alarmActive = false, statusMessage = "Silenced.")
    }

    override fun onSnoozeRequested() {
        viewModelScope.launch {
            _state.value = _state.value.copy(alarmActive = false, statusMessage = "Snoozing...")
            delay(10000)
            _state.value = _state.value.copy(alarmActive = true, statusMessage = "Snooze Expired!")
        }
    }
}
