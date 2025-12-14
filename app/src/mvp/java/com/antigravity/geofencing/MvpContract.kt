package com.antigravity.geofencing

interface MvpContract {
    interface View {
        fun showStatus(status: String)
        fun showAlarm()
        fun hideAlarm()
        fun getLatitude(): String
        fun getLongitude(): String
        fun getRadius(): String
        fun showInputError()
        fun navigateToHistory()
        fun addGeofence(requestId: String, lat: Double, lng: Double, radius: Float)
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onSetGeofenceClicked()
        fun onSilenceClicked()
        fun onSnoozeClicked()
        fun onHistoryClicked()
    }
}
