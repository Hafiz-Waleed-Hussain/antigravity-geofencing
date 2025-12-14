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
import io.reactivex.rxjava3.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private val viewModel: MviViewModel by viewModels()
    private val disposables = CompositeDisposable()

    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var etRad: EditText
    private lateinit var btnSet: Button
    private lateinit var btnSilence: Button
    private lateinit var btnSnooze: Button
    private lateinit var tvStatus: TextView
    private lateinit var layoutAlarm: LinearLayout

    private lateinit var geofencingClient: GeofencingClient

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
        setContentView(R.layout.activity_main) // We reuse the MVP layout logic but different file

        initViews()
        geofencingClient = LocationServices.getGeofencingClient(this)

        bindViewModel()

        MviGeofenceReceiver.listener = { viewModel.processIntent(MviIntent.GeofenceEntered) }
    }

    private fun initViews() {
        etLat = findViewById(R.id.et_latitude)
        etLng = findViewById(R.id.et_longitude)
        etRad = findViewById(R.id.et_radius)
        btnSet = findViewById(R.id.btn_set_geofence)
        btnSilence = findViewById(R.id.btn_silence)
        btnSnooze = findViewById(R.id.btn_snooze)
        tvStatus = findViewById(R.id.tv_status)
        layoutAlarm = findViewById(R.id.layout_alarm_controls)

        // Manual "RxBinding" for clicks
        btnSet.setOnClickListener {
            val lat = etLat.text.toString().toDoubleOrNull()
            val lng = etLng.text.toString().toDoubleOrNull()
            val rad = etRad.text.toString().toFloatOrNull()
            if (lat != null && lng != null && rad != null) {
                addGeofence(lat, lng, rad)
                viewModel.processIntent(MviIntent.SetGeofence(lat, lng, rad))
            } else {
                Toast.makeText(this, "Invalid Input", Toast.LENGTH_SHORT).show()
            }
        }

        btnSilence.setOnClickListener { viewModel.processIntent(MviIntent.SilenceAlarm) }
        btnSnooze.setOnClickListener { viewModel.processIntent(MviIntent.SnoozeAlarm) }
    }

    private fun bindViewModel() {
        disposables.add(viewModel.states.subscribe { state -> render(state) })
    }

    private fun render(state: MviViewState) {
        tvStatus.text = "Status: ${state.status}"
        layoutAlarm.visibility = if (state.isAlarmPlaying) View.VISIBLE else View.GONE

        if (state.error != null) {
            Toast.makeText(this, "Error: ${state.error.message}", Toast.LENGTH_SHORT).show()
        }

        // MVI implies we render the whole state every time.
    }

    // Side Effect: Geofencing is "Fire and Forget" from the UI perspective here, but effectively
    // it's a side effect.
    @SuppressLint("MissingPermission")
    private fun addGeofence(lat: Double, lng: Double, radius: Float) {
        // Permissions check omitted for brevity in snippet but strictly required
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1002
            )
            return
        }

        val geofence =
                Geofence.Builder()
                        .setRequestId("GEOFENCE_ID_MVI")
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
        disposables.clear()
    }
}
