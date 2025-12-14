package com.antigravity.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.antigravity.geofencing.databinding.ActivityMainBinding
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private val viewModel: MvvmViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        intent.action = "com.antigravity.geofencing.ACTION_GEOFENCE_EVENT"
        PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        geofencingClient = LocationServices.getGeofencingClient(this)

        viewModel.geofenceRequest.observe(this) { request ->
            request?.let {
                addGeofence(it.first, it.second, it.third)
                // reset event (naive simulation of SingleLiveEvent)
                // In real app, we'd clear it or use a proper event wrapper
            }
        }

        // Listen for global events if any (e.g. from receiver)
        // Since receiver goes to a BroadcastReceiver class, we need a way to communicate back to UI
        // logic if the app is open
        // We can use a shared LiveData bus or just a singleton callback.
        // For this demo, let's use a static instance helper in Receiver
        GeofenceBroadcastReceiver.listener = { viewModel.onGeofenceEntered() }
    }

    private fun addGeofence(lat: Double, lng: Double, radius: Float) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    1001
            )
            return
        }

        val geofence =
                Geofence.Builder()
                        .setRequestId("GEOFENCE_ID")
                        .setCircularRegion(lat, lng, radius)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build()

        val request =
                GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofence(geofence)
                        .build()

        geofencingClient.addGeofences(request, geofencePendingIntent).run {
            addOnSuccessListener { viewModel.onGeofenceSetSuccess() }
            addOnFailureListener { viewModel.onGeofenceSetFailure(it.message ?: "Unknown Error") }
        }
    }
}
