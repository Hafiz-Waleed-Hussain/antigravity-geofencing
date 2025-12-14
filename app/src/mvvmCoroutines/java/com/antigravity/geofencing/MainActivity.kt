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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.UUID

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val viewModel: CoroutinesViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var geofencingClient: GeofencingClient

    private var selectedLocation: com.google.android.gms.maps.model.LatLng? = null
    private var googleMap: com.google.android.gms.maps.GoogleMap? = null

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

        // Ensure receiver compat
        val flags =
                if (true) android.content.Context.RECEIVER_EXPORTED
                else android.content.Context.RECEIVER_NOT_EXPORTED
        registerReceiver(workerReceiver, IntentFilter("ACTION_WORKER_ALARM"), flags)

        setupObservations()
        setupListeners()

        val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map_fragment) as
                        com.google.android.gms.maps.SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        val defaultLocation = LatLng(37.4220, -122.0841)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

        googleMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            binding.tvSelectedLocation.text =
                    String.format("Selected: %.4f, %.4f", latLng.latitude, latLng.longitude)

            // Sync with EditTexts
            binding.etLatitude.setText(latLng.latitude.toString())
            binding.etLongitude.setText(latLng.longitude.toString())
        }
    }

    private fun setupListeners() {
        binding.btnSetGeofence.setOnClickListener {
            val latStr = binding.etLatitude.text.toString()
            val lngStr = binding.etLongitude.text.toString()
            val radStr = binding.etRadius.text.toString()

            val lat = latStr.toDoubleOrNull()
            val lng = lngStr.toDoubleOrNull()
            val rad = radStr.toFloatOrNull()

            if (lat != null && lng != null && rad != null) {
                // Check permissions here using helper or checking directly before calling logic
                // But addGeofence handles request.
                // We rely on addGeofence to return cleanly.
                // Issue: addGeofence logic is decoupled.
                // Fix: Check permission HERE.
                if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val requestId = java.util.UUID.randomUUID().toString()
                    addGeofence(requestId, lat, lng, rad)
                    viewModel.setGeofence(requestId, lat, lng, rad)
                } else {
                    val requestId = java.util.UUID.randomUUID().toString()
                    addGeofence(requestId, lat, lng, rad)
                    // Note: In real app, we might retry setGeofence after permission grant,
                    // but for now this triggers the system permission flow.
                }
            } else {
                // Toast.makeText(this, "Invalid Input", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnSilence.setOnClickListener { viewModel.stopAlarm() }
        binding.btnSnooze.setOnClickListener { viewModel.snoozeAlarm() }
        binding.btnHistory.setOnClickListener { viewModel.onHistoryClicked() }
    }

    private fun setupObservations() {
        viewModel.status.observe(this) { binding.tvStatus.text = it }
        viewModel.isAlarmPlaying.observe(this) { playing ->
            binding.layoutAlarmControls.visibility = if (playing) View.VISIBLE else View.GONE
            if (playing) {
                AlarmUtils.startAlarm(this)
                AlarmUtils.sendNotification(this, "Geofence Entered", "You have entered the zone!")
            } else {
                AlarmUtils.stopAlarm()
            }
        }
        viewModel.navigateToHistory.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, GeofenceHistoryActivity::class.java))
                viewModel.onHistoryNavigated()
            }
        }
    }

    private fun addGeofence(requestId: String, lat: Double, lng: Double, radius: Float) {
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
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1003)
            viewModel.onPermissionRequired()
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= 29 &&
                        ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    1004
            )
            viewModel.onPermissionRequired()
            return
        }

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

        geofencingClient.addGeofences(request, geofencePendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(workerReceiver)
    }
}
