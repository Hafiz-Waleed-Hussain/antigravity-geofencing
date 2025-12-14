package com.antigravity.geofencing

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), MvpContract.View, OnMapReadyCallback {

    private lateinit var presenter: MvpPresenter

    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0
    private var isLocationSelected: Boolean = false

    private lateinit var etRad: EditText
    private lateinit var tvStatus: TextView
    private lateinit var layoutAlarm: LinearLayout
    private lateinit var btnSet: Button
    private var selectedLocation: LatLng? = null
    private lateinit var btnSilence: Button
    private lateinit var btnSnooze: Button
    private lateinit var tvSelectedLocation: TextView

    private var googleMap: com.google.android.gms.maps.GoogleMap? = null

    private var locationManager: LocationManager? = null
    private var pendingIntent: PendingIntent? = null
    private var mediaPlayer: MediaPlayer? = null

    private val PROX_ALERT_INTENT = "com.antigravity.geofencing.PROXIMITY_ALERT"

    private val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val key = LocationManager.KEY_PROXIMITY_ENTERING
                    val entering = intent?.getBooleanExtra(key, false)
                    if (entering == true) {
                        presenter.onGeofenceEntered()
                    } else {
                        // Exiting or unknown
                        Toast.makeText(context, "Exited Geofence", Toast.LENGTH_SHORT).show()
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter = MvpPresenter()
        presenter.attachView(this)

        initViews()

        geofencingClient =
                com.google.android.gms.location.LocationServices.getGeofencingClient(this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Ensure compatibility for RECEIVER_NOT_EXPORTED
        val listenToBroadcastsFromOtherApps = false
        val receiverFlags =
                if (listenToBroadcastsFromOtherApps) {
                    android.content.Context.RECEIVER_EXPORTED
                } else {
                    android.content.Context.RECEIVER_NOT_EXPORTED
                }

        registerReceiver(receiver, IntentFilter(PROX_ALERT_INTENT), receiverFlags)

        val mapFragment =
                supportFragmentManager.findFragmentById(R.id.map_fragment) as
                        com.google.android.gms.maps.SupportMapFragment
        mapFragment.getMapAsync(this)

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    locationListener
            )
        }
    }

    private val locationListener =
            android.location.LocationListener { location ->
                // Use tvSelectedLocation or a new view. To be safe, let's append to tvStatus or use
                // a dedicated one.
                // I will repurpose tvSelectedLocation prefix if user hasn't selected anything,
                // OR better, just update a new line in tvStatus or similar.
                // Actually, let's just create a dynamic TextView or use Toast? No, Toast is
                // annoying.
                // Let's toggle the ActionBar title? Or a TextView.
                // Let's use tvSelectedLocation for now as "Current: ..."
                if (!isLocationSelected) {
                    tvSelectedLocation.text = "Current: ${location.latitude}, ${location.longitude}"
                }
            }

    private fun initViews() {
        // etLat/etLng removed replaced by Map
        tvSelectedLocation = findViewById(R.id.tv_selected_location)
        etRad = findViewById(R.id.et_radius)
        tvStatus = findViewById(R.id.tv_status)
        layoutAlarm = findViewById(R.id.layout_alarm_controls)
        btnSet = findViewById(R.id.btn_set_geofence)
        btnSilence = findViewById(R.id.btn_silence)
        btnSnooze = findViewById(R.id.btn_snooze)

        btnSet.setOnClickListener {
            // Allow manual input without map selection
            presenter.onSetGeofenceClicked()
        }

        findViewById<android.widget.Button>(R.id.btn_history).setOnClickListener {
            presenter.onHistoryClicked()
        }
    }

    private lateinit var geofencingClient: com.google.android.gms.location.GeofencingClient

    override fun navigateToHistory() {
        startActivity(Intent(this, GeofenceHistoryActivity::class.java))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        // Default to Googleplex
        val defaultLocation = LatLng(37.4220, -122.0841)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f))

        googleMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))

            // Update EditTexts
            findViewById<android.widget.EditText>(R.id.et_latitude)
                    .setText(latLng.latitude.toString())
            findViewById<android.widget.EditText>(R.id.et_longitude)
                    .setText(latLng.longitude.toString())
        }
    }

    // Legacy method signature match if needed, but we are calling logic directly
    // Ideally we remove this or fix usage.
    // However, clean solution:
    override fun addGeofence(requestId: String, lat: Double, lng: Double, radius: Float) {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // Permission handling omitted for brevity/redundancy - assuming granted or handled by
            // manual check logic in registerGeofence previously
            // But we should check.
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    123
            )
            return
        }

        val geofence =
                com.google.android.gms.location.Geofence.Builder()
                        .setRequestId(requestId)
                        .setCircularRegion(lat, lng, radius)
                        .setExpirationDuration(
                                com.google.android.gms.location.Geofence.NEVER_EXPIRE
                        )
                        .setTransitionTypes(
                                com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
                        )
                        .build()

        val request =
                com.google.android.gms.location.GeofencingRequest.Builder()
                        .setInitialTrigger(
                                com.google.android.gms.location.GeofencingRequest
                                        .INITIAL_TRIGGER_ENTER
                        )
                        .addGeofence(geofence)
                        .build()

        // Use MVP specific receiver action
        val intent = Intent(PROX_ALERT_INTENT)
        val pendingIntent =
                PendingIntent.getBroadcast(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

        geofencingClient
                .addGeofences(request, pendingIntent)
                .addOnSuccessListener {
                    // Toast.makeText(this, "Added to GeofencingClient", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
    }
    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
        unregisterReceiver(receiver)
        if (pendingIntent != null) {
            locationManager?.removeProximityAlert(pendingIntent!!)
        }
        stopSound()
    }

    override fun showStatus(status: String) {
        tvStatus.text = "Status: $status"
    }

    override fun showAlarm() {
        layoutAlarm.visibility = View.VISIBLE
        playSound()
    }

    override fun hideAlarm() {
        layoutAlarm.visibility = View.GONE
        stopSound()
    }

    private fun playSound() {
        if (mediaPlayer == null) {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, alarmUri)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    private fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun getLatitude(): String =
            findViewById<android.widget.EditText>(R.id.et_latitude).text.toString()
    override fun getLongitude(): String =
            findViewById<android.widget.EditText>(R.id.et_longitude).text.toString()
    override fun getRadius(): String = etRad.text.toString()

    override fun showInputError() {
        Toast.makeText(this, "Invalid Input", Toast.LENGTH_SHORT).show()
    }
}
