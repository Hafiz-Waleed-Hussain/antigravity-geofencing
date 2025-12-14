package com.antigravity.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private val viewModel: FlowViewModel by viewModels()
    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, FlowReceiver::class.java)
        intent.action = "com.antigravity.geofencing.ACTION_GEOFENCE_EVENT_FLOW"
        PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        geofencingClient = LocationServices.getGeofencingClient(this)
        FlowReceiver.eventCallback = { viewModel.onGeofenceEntered() }

        setContent {
            MaterialTheme {
                MainScreen(
                        viewModel = viewModel,
                        onSetGeofence = { lat, lng, rad -> addGeofence(lat, lng, rad) }
                )
            }
        }
    }

    // Simplification: In a real app we'd keep logic out of Activity, but the GeofenceClient
    // requires Context/Activity integration
    private fun addGeofence(lat: Double, lng: Double, radius: Float) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1004
            )
            return
        }

        val geofence =
                Geofence.Builder()
                        .setRequestId("GEOFENCE_ID_FLOW")
                        .setCircularRegion(lat, lng, radius)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build()

        val request =
                GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofence(geofence)
                        .build()

        geofencingClient.addGeofences(request, geofencePendingIntent).addOnSuccessListener {
            viewModel.setGeofence(lat, lng, radius)
        }
    }
}

@Composable
fun MainScreen(viewModel: FlowViewModel, onSetGeofence: (Double, Double, Float) -> Unit) {
    val state by viewModel.state.collectAsState()
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var rad by remember { mutableStateOf("100") }

    Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
    ) {
        Text("MVVM Flow + Compose", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
                value = lat,
                onValueChange = { lat = it },
                label = { Text("Latitude") },
                modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
                value = lng,
                onValueChange = { lng = it },
                label = { Text("Longitude") },
                modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
                value = rad,
                onValueChange = { rad = it },
                label = { Text("Radius") },
                modifier = Modifier.fillMaxWidth()
        )

        Button(
                onClick = {
                    val dLat = lat.toDoubleOrNull()
                    val dLng = lng.toDoubleOrNull()
                    val fRad = rad.toFloatOrNull()
                    if (dLat != null && dLng != null && fRad != null) {
                        onSetGeofence(dLat, dLng, fRad)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("Set Geofence") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Status: ${state.status}")

        if (state.isAlarmVisible) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                    "ALARM PLAYING!",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.headlineSmall
            )
            Button(onClick = { viewModel.silence() }, modifier = Modifier.fillMaxWidth()) {
                Text("Silence")
            }
            Button(onClick = { viewModel.snooze() }, modifier = Modifier.fillMaxWidth()) {
                Text("Snooze")
            }
        }
    }
}
