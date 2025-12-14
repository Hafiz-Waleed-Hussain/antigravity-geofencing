package com.antigravity.geofencing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.geofencing.data.GeofenceEntity
import com.antigravity.geofencing.data.GeofenceRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeofenceHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val geofencingClient = LocationServices.getGeofencingClient(this)
        val dao = GeofenceRepository.getDao()

        setContent {
            MaterialTheme {
                val history by dao.getAll().collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()

                HistoryScreen(
                        history = history,
                        onDelete = { entity ->
                            scope.launch {
                                // 1. Remove from System
                                try {
                                    if (entity.requestId.isNotEmpty()) {
                                        geofencingClient.removeGeofences(listOf(entity.requestId))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                // 2. Remove from DB
                                withContext(Dispatchers.IO) { dao.delete(entity) }
                            }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(history: List<GeofenceEntity>, onDelete: (GeofenceEntity) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Geofence History") }) }) { padding ->
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No history captured yet")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(history) { item -> HistoryItem(item, onDelete) }
            }
        }
    }
}

@Composable
fun HistoryItem(entity: GeofenceEntity, onDelete: (GeofenceEntity) -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Lat: ${entity.latitude}", style = MaterialTheme.typography.bodyLarge)
                Text("Lng: ${entity.longitude}", style = MaterialTheme.typography.bodyLarge)
                Text("Radius: ${entity.radius}m", style = MaterialTheme.typography.bodyMedium)
                Text("ID: ${entity.requestId}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { onDelete(entity) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
