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
    private val requestPermissionLauncher =
            registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts
                            .RequestMultiplePermissions()
            ) {}

    private val requestBackgroundLauncher =
            registerForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) {}

    private fun addGeofence(lat: Double, lng: Double, radius: Float) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED ||
                        (android.os.Build.VERSION.SDK_INT >= 33 &&
                                ActivityCompat.checkSelfPermission(
                                        this,
                                        Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED)
        ) {
            val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            requestPermissionLauncher.launch(permissions.toTypedArray())
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= 29 &&
                        ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestBackgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            return
        }

        // Generate Unique ID
        val requestId = java.util.UUID.randomUUID().toString()

        val geofence =
                Geofence.Builder()
                        .setRequestId(requestId)
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
            viewModel.setGeofence(requestId, lat, lng, radius)
        }
    }
}

@Composable
fun MainScreen(viewModel: FlowViewModel, onSetGeofence: (Double, Double, Float) -> Unit) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.isAlarmVisible) {
        if (state.isAlarmVisible) {
            AlarmUtils.startAlarm(context)
            AlarmUtils.sendNotification(context, "Geofence Entered", "You have entered the zone!")
        } else {
            AlarmUtils.stopAlarm()
        }
    }

    // State for Text Inputs
    var latText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("37.4220") }
    var lngText by
            androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("-122.0841") }
    var rad by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("100.0") }

    // Map State
    var selectedLocation by remember {
        mutableStateOf<com.google.android.gms.maps.model.LatLng?>(null)
    }

    val defaultLocation = com.google.android.gms.maps.model.LatLng(37.4220, -122.0841) // Googleplex
    val cameraPositionState =
            com.google.maps.android.compose.rememberCameraPositionState {
                position =
                        com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                                defaultLocation,
                                15f
                        )
            }

    // Sync Map Click to Text
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let {
            latText = it.latitude.toString()
            lngText = it.longitude.toString()
        }
    }

    Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Top
    ) {
        Text("MVVM Flow + Google Maps", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .weight(1f) // Fill available space
                                .padding(bottom = 16.dp)
        ) {
            com.google.maps.android.compose.GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng -> selectedLocation = latLng }
            ) {
                selectedLocation?.let {
                    com.google.maps.android.compose.Marker(
                            state = com.google.maps.android.compose.MarkerState(position = it),
                            title = "Selected Location"
                    )
                }
            }
        }

        // Manual Input Fields
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
                value = rad,
                onValueChange = { rad = it },
                label = { Text("Radius (meters)") },
                modifier = Modifier.fillMaxWidth()
        )

        Button(
                onClick = {
                    val lat = latText.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    val fRad = rad.toFloatOrNull()
                    if (lat != null && lng != null && fRad != null) {
                        onSetGeofence(lat, lng, fRad)
                    }
                },
                enabled = latText.isNotEmpty() && lngText.isNotEmpty(),
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

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.onHistoryClicked() }, modifier = Modifier.fillMaxWidth()) {
            Text("View History")
        }

        if (state.navigateToHistory) {
            LaunchedEffect(Unit) {
                context.startActivity(Intent(context, GeofenceHistoryActivity::class.java))
                viewModel.onHistoryNavigated()
            }
        }
    }
}
