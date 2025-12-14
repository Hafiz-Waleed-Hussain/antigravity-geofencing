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
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var rad by remember { mutableStateOf("100") }

    // AI Note: UI is function of State
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Flavor: Machine (AI Optimized)", style = MaterialTheme.typography.headlineMedium)

        TextField(value = lat, onValueChange = { lat = it }, label = { Text("Lat") })
        TextField(value = lng, onValueChange = { lng = it }, label = { Text("Lng") })
        TextField(value = rad, onValueChange = { rad = it }, label = { Text("Rad") })

        Button(onClick = { controller.processInput(lat, lng, rad) }) {
            Text("Inject Configuration")
        }

        Text("Status: ${state.statusMessage}")

        if (state.alarmActive) {
            Text("ALARM ACTIVE", color = MaterialTheme.colorScheme.error)
            Button(onClick = { controller.onSilenceRequested() }) { Text("Silence") }
            Button(onClick = { controller.onSnoozeRequested() }) { Text("Snooze") }
        }
    }
}
