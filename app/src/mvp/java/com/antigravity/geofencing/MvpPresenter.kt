package com.antigravity.geofencing

import android.os.Handler
import android.os.Looper

class MvpPresenter : MvpContract.Presenter {

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
            // Validation logic
            val lat = latStr.toDouble()
            val lng = lngStr.toDouble()
            val rad = radStr.toFloat()
            // In a real app we'd pass these back to view to register
            // For MVP, we tell the view "Go ahead and register"
            view?.showStatus("Geofencing set for $lat, $lng ($rad m)")
            // We assume successful registration for now; the View handles the system call
        } catch (e: NumberFormatException) {
            view?.showInputError()
        }
    }

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
