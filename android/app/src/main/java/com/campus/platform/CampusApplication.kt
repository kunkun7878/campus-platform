package com.campus.platform

import android.app.Application
import com.campus.platform.push.ForegroundTracker
import com.campus.platform.push.NotificationChannelSetup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CampusApplication : Application() {

    @Inject lateinit var channelSetup: NotificationChannelSetup
    @Inject lateinit var foregroundTracker: ForegroundTracker

    override fun onCreate() {
        super.onCreate()

        // Create FCM notification channels (API 26+)
        channelSetup.createChannels(this)

        // Track foreground/background state for FCM notification suppression
        registerActivityLifecycleCallbacks(foregroundTracker.callbacks)
    }
}
