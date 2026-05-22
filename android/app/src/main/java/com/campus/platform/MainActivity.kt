package com.campus.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.campus.platform.data.auth.AuthRepository
import com.campus.platform.data.school.SchoolRepository
import com.campus.platform.navigation.CampusNavGraph
import com.campus.platform.ui.theme.CampusPlatformTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var schoolRepository: SchoolRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusPlatformTheme {
                val navController = rememberNavController()
                CampusNavGraph(
                    navController = navController,
                    authRepository = authRepository,
                    schoolRepository = schoolRepository,
                )
            }
        }
    }
}
