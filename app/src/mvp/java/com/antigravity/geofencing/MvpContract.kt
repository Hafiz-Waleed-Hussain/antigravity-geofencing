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
    }

    interface Presenter {
        fun attachView(view: View)
        fun detachView()
        fun onSetGeofenceClicked()
        fun onSilenceClicked()
        fun onSnoozeClicked()
    }
}
