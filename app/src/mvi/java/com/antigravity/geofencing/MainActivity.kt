package com.antigravity.geofencing

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.rxjava3.disposables.CompositeDisposable

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private val viewModel: MviViewModel by viewModels()
    private val disposables = CompositeDisposable()

    private var selectedLocation: com.google.android.gms.maps.model.LatLng? = null

    private lateinit var etRad: EditText
    private lateinit var btnSet: Button
    private lateinit var btnSilence: Button
    private lateinit var btnSnooze: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvSelectedLocation: TextView
    private lateinit var layoutAlarm: LinearLayout

    private lateinit var geofencingClient: GeofencingClient
    private var googleMap: com.google.android.gms.maps.GoogleMap? = null

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, MviGeofenceReceiver::class.java)
        intent.action = "com.antigravity.geofencing.ACTION_GEOFENCE_EVENT_MVI"
        PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        geofencingClient = LocationServices.getGeofencingClient(this)

        bindViewModel()

        MviGeofenceReceiver.listener = { viewModel.processIntent(MviIntent.GeofenceEntered) }

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
            tvSelectedLocation.text =
                    String.format("Selected: %.4f, %.4f", latLng.latitude, latLng.longitude)

            // Sync Input
            findViewById<EditText>(R.id.et_latitude).setText(latLng.latitude.toString())
            findViewById<EditText>(R.id.et_longitude).setText(latLng.longitude.toString())
        }
    }

    private fun initViews() {
        tvSelectedLocation = findViewById(R.id.tv_selected_location)
        etRad = findViewById(R.id.et_radius)
        btnSet = findViewById(R.id.btn_set_geofence)
        btnSilence = findViewById(R.id.btn_silence)
        btnSnooze = findViewById(R.id.btn_snooze)
        tvStatus = findViewById(R.id.tv_status)
        layoutAlarm = findViewById(R.id.layout_alarm_controls)

        btnSet.setOnClickListener {
            val latStr = findViewById<EditText>(R.id.et_latitude).text.toString()
            val lngStr = findViewById<EditText>(R.id.et_longitude).text.toString()
            val radStr = etRad.text.toString()

            val lat = latStr.toDoubleOrNull()
            val lng = lngStr.toDoubleOrNull()
            val rad = radStr.toFloatOrNull()

            if (lat != null && lng != null && rad != null) {
                // Add geofence (Side effect)
                val requestId = addGeofence(lat, lng, rad)

                if (requestId != null) {
                    // Process Intent
                    viewModel.processIntent(MviIntent.SetGeofence(requestId, lat, lng, rad))
                }
            } else {
                Toast.makeText(this, "Valid coordinates required", Toast.LENGTH_SHORT).show()
            }
        }

        btnSilence.setOnClickListener { viewModel.processIntent(MviIntent.SilenceAlarm) }
        btnSnooze.setOnClickListener { viewModel.processIntent(MviIntent.SnoozeAlarm) }
        findViewById<Button>(R.id.btn_history).setOnClickListener {
            viewModel.processIntent(MviIntent.ViewHistory)
        }
    }

    private fun bindViewModel() {
        disposables.add(viewModel.states.subscribe { state -> render(state) })
    }

    private fun render(state: MviViewState) {
        tvStatus.text = "Status: ${state.status}"
        layoutAlarm.visibility = if (state.isAlarmPlaying) View.VISIBLE else View.GONE

        if (state.navigateToHistory) {
            startActivity(Intent(this, GeofenceHistoryActivity::class.java))
            viewModel.processIntent(MviIntent.HistoryNavigated)
        }

        if (state.isAlarmPlaying) {
            AlarmUtils.startAlarm(this)
            if (state.status == "ENTERED ZONE!") { // Simple check to avoid spamming notification
                AlarmUtils.sendNotification(this, "Geofence Entered", "You have entered the zone!")
            }
        } else {
            AlarmUtils.stopAlarm()
        }

        if (state.error != null) {
            Toast.makeText(this, "Error: ${state.error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(lat: Double, lng: Double, radius: Float): String? {
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
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
            return null
        }

        // MVI flavor missed background location check entirely. Adding it for robustness.
        if (android.os.Build.VERSION.SDK_INT >= 29 &&
                        ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    101
            )
            return null
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

        geofencingClient.addGeofences(request, geofencePendingIntent)

        // Return ID so caller can pass it to VM
        return requestId
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}
