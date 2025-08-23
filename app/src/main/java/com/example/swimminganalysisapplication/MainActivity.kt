package com.example.swimminganalysisapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.RetrofitClient
import com.example.swimminganalysisapplication.ui.addswimmer.AddSwimmerScreen
import com.example.swimminganalysisapplication.ui.addswimmer.AddSwimmerViewModelFactory
import com.example.swimminganalysisapplication.ui.swimmers.SwimmersScreen
import com.example.swimminganalysisapplication.ui.swimmers.SwimmersViewModelFactory
import com.example.swimminganalysisapplication.ui.theme.SwimmingAnalysisApplicationTheme
import com.example.swimminganalysisapplication.ui.video.VideoScreen
import com.example.swimminganalysisapplication.navigation.AppDestinations

class MainActivity : ComponentActivity() {

    private val swimmingRepository by lazy { SwimmingRepository(RetrofitClient.instance) }
    private val swimmersViewModelFactory by lazy { SwimmersViewModelFactory(swimmingRepository) }
    private val addSwimmerViewModelFactory by lazy { AddSwimmerViewModelFactory(swimmingRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwimmingAnalysisApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = AppDestinations.VIDEO_SCREEN_ROUTE
                    ) {
                        composable(AppDestinations.VIDEO_SCREEN_ROUTE) {
                            VideoScreen(navController = navController)
                        }
                        composable(AppDestinations.SWIMMERS_SCREEN_ROUTE) {
                            SwimmersScreen(
                                navController = navController,
                                factory = swimmersViewModelFactory
                            )
                        }
                        composable(AppDestinations.ADD_SWIMMER_ROUTE) {
                            AddSwimmerScreen(
                                navController = navController,
                                factory = addSwimmerViewModelFactory
                            )
                        }
                    }
                }
            }
        }
    }
}
