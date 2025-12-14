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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val viewModel: MvvmViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var geofencingClient: GeofencingClient

    private var googleMap: com.google.android.gms.maps.GoogleMap? = null

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
                // Fix destructuring for DataRequest (no componentN functions)
                addGeofence(it.requestId, it.lat, it.lng, it.rad)
                // In real app, consume event properly
            }
        }

        GeofenceBroadcastReceiver.listener = { viewModel.onGeofenceEntered() }

        viewModel.isAlarmVisible.observe(this) { isVisible ->
            if (isVisible) {
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

        val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map_fragment) as
                        com.google.android.gms.maps.SupportMapFragment
        mapFragment.getMapAsync(this)

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager =
                    getSystemService(android.content.Context.LOCATION_SERVICE) as
                            android.location.LocationManager
            locationManager.requestLocationUpdates(
                    android.location.LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    locationListener
            )
        }
    }

    private val locationListener =
            android.location.LocationListener { location ->
                viewModel.updateCurrentLocation(location.latitude, location.longitude)
            }

    override fun onDestroy() {
        super.onDestroy()
        val locationManager =
                getSystemService(android.content.Context.LOCATION_SERVICE) as
                        android.location.LocationManager
        locationManager.removeUpdates(locationListener)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap // Keep the class member updated
        val defaultLocation = LatLng(37.4220, -122.0841)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

        googleMap.setOnMapClickListener { latLng ->
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))

            // Update ViewModel via Two-Way DataBinding or direct set
            // Since we used @={viewModel.latitude}, setting the LiveData updates the UI
            viewModel.latitude.value = latLng.latitude.toString()
            viewModel.longitude.value = latLng.longitude.toString()
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
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1001)
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
                    1002
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

        geofencingClient.addGeofences(request, geofencePendingIntent).run {
            addOnSuccessListener { viewModel.onGeofenceSetSuccess(requestId, lat, lng, radius) }
            addOnFailureListener { viewModel.onGeofenceSetFailure(it.message ?: "Unknown Error") }
        }
    }
}
