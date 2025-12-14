package com.antigravity.geofencing

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.antigravity.geofencing.databinding.ActivityMainBinding
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private val viewModel: CoroutinesViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, CoroutinesReceiver::class.java)
        intent.action = "com.antigravity.geofencing.ACTION_GEOFENCE_EVENT_COROUTINES"
        PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    // Broadcast from Worker
    private val workerReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    viewModel.onGeofenceEntered()
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Manual viewbinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        geofencingClient = LocationServices.getGeofencingClient(this)
        registerReceiver(workerReceiver, IntentFilter("ACTION_WORKER_ALARM"), RECEIVER_EXPORTED)

        setupObservations()
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSetGeofence.setOnClickListener {
            val lat = binding.etLatitude.text.toString().toDoubleOrNull()
            val lng = binding.etLongitude.text.toString().toDoubleOrNull()
            val rad = binding.etRadius.text.toString().toFloatOrNull()
            if (lat != null && lng != null && rad != null) {
                addGeofence(lat, lng, rad)
                viewModel.setGeofence(lat, lng, rad)
            }
        }
        binding.btnSilence.setOnClickListener { viewModel.stopAlarm() }
        binding.btnSnooze.setOnClickListener { viewModel.snoozeAlarm() }
    }

    private fun setupObservations() {
        viewModel.status.observe(this) { binding.tvStatus.text = it }
        viewModel.isAlarmPlaying.observe(this) {
            binding.layoutAlarmControls.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    private fun addGeofence(lat: Double, lng: Double, radius: Float) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1003
            )
            return
        }

        val geofence =
                Geofence.Builder()
                        .setRequestId("GEOFENCE_ID_COROUTINES")
                        .setCircularRegion(lat, lng, radius)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build()

        val request =
                GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofence(geofence)
                        .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(workerReceiver)
    }
}
