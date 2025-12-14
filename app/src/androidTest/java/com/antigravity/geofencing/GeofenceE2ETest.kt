package com.antigravity.geofencing

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GeofenceE2ETest {

    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule =
            GrantPermissionRule.grant(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )

    // Note: POST_NOTIFICATIONS needs SDK 33 check. GrantPermissionRule might fail if permission
    // doesn't exist on device.
    // For simplicity in this demo environment running on API 34, we assume it exists or use UI
    // interaction if needed.
    // However, GrantPermissionRule.grant ignores unknown permissions on some versions, but crashes
    // on others.
    // Let's rely on the app asking and us granting, OR just grant it if we can.

    @Test
    fun testManualGeofenceCreation() {
        // 1. Enter Latitude
        onView(withId(R.id.et_latitude)).perform(replaceText("37.4220"), closeSoftKeyboard())

        // 2. Enter Longitude
        onView(withId(R.id.et_longitude)).perform(replaceText("-122.0841"), closeSoftKeyboard())

        // 3. Enter Radius explicitly
        onView(withId(R.id.et_radius)).perform(replaceText("200.0"), closeSoftKeyboard())

        // 4. Click Set Geofence
        onView(withId(R.id.btn_set_geofence)).perform(click())

        // 5. Verify Status updates to "Geofence Active!" (or similar)
        // We might need to wait a sec for async registration
        Thread.sleep(1000)
        onView(withId(R.id.tv_status)).check(matches(withText(containsString("Active"))))
    }
}
