package com.antigravity.geofencing

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MvpPresenter : MvpContract.Presenter {

    private val presenterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var view: MvpContract.View? = null
    // Legacy Handler for delay (2010 style)
    private val handler = Handler(Looper.getMainLooper())
    private var snoozeRunnable: Runnable? = null

    override fun attachView(view: MvpContract.View) {
        this.view = view
    }

    override fun detachView() {
        this.view = null
        if (snoozeRunnable != null) {
            handler.removeCallbacks(snoozeRunnable!!)
        }
        presenterScope.cancel()
    }

    override fun onHistoryClicked() {
        view?.navigateToHistory()
    }

    override fun onSetGeofenceClicked() {
        val latStr = view?.getLatitude()
        val lngStr = view?.getLongitude()
        val radStr = view?.getRadius()

        if (latStr.isNullOrEmpty() || lngStr.isNullOrEmpty() || radStr.isNullOrEmpty()) {
            view?.showInputError()
            return
        }

        try {
            val lat = latStr.toDouble()
            val lng = lngStr.toDouble()
            val rad = radStr.toFloat()
            val requestId = java.util.UUID.randomUUID().toString()

            view?.addGeofence(requestId, lat, lng, rad)
            view?.showStatus("Geofence Set: $lat, $lng ($rad m)")

            // Save to DB (Coroutine Style)
            presenterScope.launch(Dispatchers.IO) {
                try {
                    val entity =
                            com.antigravity.geofencing.data.GeofenceEntity(
                                    requestId = requestId,
                                    latitude = lat,
                                    longitude = lng,
                                    radius = rad
                            )
                    com.antigravity.geofencing.data.GeofenceRepository.getDao().insert(entity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: NumberFormatException) {
            view?.showInputError()
        }
    }

    // ... rest of logic

    override fun onSilenceClicked() {
        view?.hideAlarm()
        view?.showStatus("Alarm Silenced. Task Complete.")
    }

    override fun onSnoozeClicked() {
        view?.hideAlarm()
        view?.showStatus("Snoozed for 10 minutes...")

        snoozeRunnable = Runnable {
            view?.showAlarm()
            view?.showStatus("Snooze over! Alarm Playing!")
        }
        // 10 minutes
        handler.postDelayed(snoozeRunnable!!, 10 * 60 * 1000)
    }

    // Method called by View when alert is received
    fun onGeofenceEntered() {
        view?.showAlarm()
        view?.showStatus("Entered Geofence!")
    }
}
