package com.campus.platform.push

import android.app.Activity
import android.app.Application
import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether any Activity is currently in the foreground.
 *
 * Used by [CampusMessagingService] to decide:
 *   foreground → skip notification (Realtime already delivered the payload)
 *   background → show system notification with deep-link intent
 *
 * Registered via Application.registerActivityLifecycleCallbacks.
 */
@Singleton
class ForegroundTracker @Inject constructor() {

    @Volatile
    var isInForeground: Boolean = false
        private set

    private var startedCount = 0

    val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedCount++
            if (startedCount == 1) {
                isInForeground = true
            }
        }

        override fun onActivityStopped(activity: Activity) {
            startedCount--
            if (startedCount == 0) {
                isInForeground = false
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}
