package com.campus.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.campus.platform.navigation.CampusNavGraph
import com.campus.platform.ui.theme.CampusPlatformTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Held here so onNewIntent can navigate when app is already running. */
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle notification deep link that launched the activity
        val targetRoute = intent.getStringExtra("fcm_route")
        val notificationId = intent.getStringExtra("notification_id")

        setContent {
            CampusPlatformTheme {
                val nc = rememberNavController()
                navController = nc

                CampusNavGraph(navController = nc)

                // Navigate to deep-link route on initial launch.
                // Must use LaunchedEffect — calling navigate() directly in
                // composition is a side-effect that may cause issues with
                // recomposition and navigation state.
                if (targetRoute != null) {
                    LaunchedEffect(targetRoute) {
                        nc.navigate(targetRoute)
                        // Clear extras to prevent re-navigation on config change
                        intent.removeExtra("fcm_route")
                        intent.removeExtra("notification_id")
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val route = intent.getStringExtra("fcm_route") ?: return
        navController?.navigate(route)
    }
}
