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

class MainActivity : AppCompatActivity(), MvpContract.View {

    private lateinit var presenter: MvpPresenter

    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var etRad: EditText
    private lateinit var tvStatus: TextView
    private lateinit var layoutAlarm: LinearLayout
    private lateinit var btnSet: Button
    private lateinit var btnSilence: Button
    private lateinit var btnSnooze: Button

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

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        registerReceiver(receiver, IntentFilter(PROX_ALERT_INTENT), RECEIVER_NOT_EXPORTED)
    }

    private fun initViews() {
        etLat = findViewById(R.id.et_latitude)
        etLng = findViewById(R.id.et_longitude)
        etRad = findViewById(R.id.et_radius)
        tvStatus = findViewById(R.id.tv_status)
        layoutAlarm = findViewById(R.id.layout_alarm_controls)
        btnSet = findViewById(R.id.btn_set_geofence)
        btnSilence = findViewById(R.id.btn_silence)
        btnSnooze = findViewById(R.id.btn_snooze)

        btnSet.setOnClickListener {
            presenter.onSetGeofenceClicked()
            registerGeofence()
        }
        btnSilence.setOnClickListener { presenter.onSilenceClicked() }
        btnSnooze.setOnClickListener { presenter.onSnoozeClicked() }
    }

    private fun registerGeofence() {
        try {
            val lat = etLat.text.toString().toDouble()
            val lng = etLng.text.toString().toDouble()
            val rad = etRad.text.toString().toFloat()

            val intent = Intent(PROX_ALERT_INTENT)
            pendingIntent =
                    PendingIntent.getBroadcast(
                            this,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

            // Legacy API: addProximityAlert
            locationManager?.addProximityAlert(lat, realLng, rad, -1, pendingIntent!!)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // ignore parsing errors, handled by presenter logic mostly
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

    override fun getLatitude(): String = etLat.text.toString()
    override fun getLongitude(): String = etLng.text.toString()
    override fun getRadius(): String = etRad.text.toString()

    override fun showInputError() {
        Toast.makeText(this, "Invalid Input", Toast.LENGTH_SHORT).show()
    }
}
