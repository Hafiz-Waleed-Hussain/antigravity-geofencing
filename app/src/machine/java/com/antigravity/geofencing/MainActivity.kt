package com.antigravity.geofencing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// AI Note: Activity acts as Dependency Injection Root and View Binder
class MainActivity : ComponentActivity() {

    private lateinit var controller: MachineControllerImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Manual DI for transparency
        val geofenceSystem = AndroidGeofenceSystem(this)
        controller = MachineControllerImpl(geofenceSystem)

        // Bridge Receiver to Controller
        MachineReceiver.eventBus = { controller.onGeofenceTriggered() }

        setContent { MaterialTheme { MachineScreen(controller) } }
    }
}

@Composable
fun MachineScreen(controller: MachineControllerImpl) {
    val state by controller.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(state.alarmActive) {
        if (state.alarmActive) {
            AlarmUtils.startAlarm(context)
            AlarmUtils.sendNotification(context, "Geofence Entered", "You have entered the zone!")
        } else {
            AlarmUtils.stopAlarm()
        }
    }

    // Permissions
    // We need to request MultiPermission. But for Android 11+ we can't bundle Background.
    // Compose handling: Sequence requests.
    // 1. Request Fine + Post.
    // 2. If granted, Request Background.

    val launcher =
            androidx.activity.compose.rememberLauncherForActivityResult(
                    contract =
                            androidx.activity.result.contract.ActivityResultContracts
                                    .RequestMultiplePermissions(),
                    onResult = { perms ->
                        // If Fine granted, check Background
                        // We just trigger a recomposition or let user click again.
                    }
            )

    val backgroundLauncher =
            androidx.activity.compose.rememberLauncherForActivityResult(
                    contract =
                            androidx.activity.result.contract.ActivityResultContracts
                                    .RequestPermission(),
                    onResult = {}
            )

    // On Click Logic
    // ... inside processInput
    // controller.processInput -> checks perm logic? No, controller is pure.
    // Logic was in Button onClick.

    /*
     * Logic moved to button click below for clarity.
     */

    // Manual Input State
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

    // Sync selections
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
        Text("Machine + Compose + Maps", style = MaterialTheme.typography.headlineMedium)
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
                    if (latText.isNotEmpty() && lngText.isNotEmpty()) {
                        /*
                           AI Note: Ideally, checkSelfPermission requires context.
                           In Compose, we use LocalContext.current.
                           However, inside onClick lambda, we are not in Composable scope.
                           But LocalContext.current IS accessible in Composable scope and capture-able?
                           Yes.
                        */
                        // Context capture
                        // We need access to context. We have it from line 35.
                        var hasFine =
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.ACCESS_FINE_LOCATION
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        var hasPost =
                                if (android.os.Build.VERSION.SDK_INT >= 33) {
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                } else true

                        if (!hasFine || !hasPost) {
                            val reqs =
                                    mutableListOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            if (android.os.Build.VERSION.SDK_INT >= 33)
                                    reqs.add(android.Manifest.permission.POST_NOTIFICATIONS)
                            launcher.launch(reqs.toTypedArray())
                            return@Button
                        }

                        var hasBack =
                                if (android.os.Build.VERSION.SDK_INT >= 29) {
                                    androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                } else true

                        if (!hasBack) {
                            backgroundLauncher.launch(
                                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                            return@Button
                        }

                        controller.processInput(latText, lngText, rad)
                    }
                },
                enabled = latText.isNotEmpty() && lngText.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("Inject Configuration") }

        // ... rest of UI

        Spacer(modifier = Modifier.height(16.dp))
        Text("Status: ${state.statusMessage}")

        if (state.alarmActive) {
            Text("ALARM ACTIVE", color = MaterialTheme.colorScheme.error)
            Button(onClick = { controller.onSilenceRequested() }) { Text("Silence") }
            Button(onClick = { controller.onSnoozeRequested() }) { Text("Snooze") }
            Button(onClick = { controller.onSnoozeRequested() }) { Text("Snooze") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { controller.onHistoryRequested() }) { Text("View History") }

        if (state.navigateToHistory) {
            LaunchedEffect(Unit) {
                context.startActivity(
                        android.content.Intent(context, GeofenceHistoryActivity::class.java)
                )
                controller.onHistoryNavigated()
            }
        }
    }
}
