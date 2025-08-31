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
import com.example.swimminganalysisapplication.ui.home.HomeScreen
import com.example.swimminganalysisapplication.ui.swimmers.SwimmersScreen
import com.example.swimminganalysisapplication.ui.swimmers.SwimmersViewModelFactory
import com.example.swimminganalysisapplication.ui.theme.SwimmingAnalysisApplicationTheme
import com.example.swimminganalysisapplication.ui.video.VideoScreen
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.video.StartPositionSettingScreen
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
                        startDestination = AppDestinations.HOME_SCREEN_ROUTE
                    ) {
                        composable(AppDestinations.HOME_SCREEN_ROUTE) { // Add HomeScreen route
                            HomeScreen(navController = navController)
                        }
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
                        composable(
                            route = AppDestinations.START_POSITION_SETTING_ROUTE +
                                    "?video1Uri={video1Uri}&video2Uri={video2Uri}" +
                                    "&currentStart1Ms={currentStart1Ms}&currentStart2Ms={currentStart2Ms}" +
                                    "&duration1Ms={duration1Ms}&duration2Ms={duration2Ms}",
                            // Arguments definition can be more explicit if needed, but string query params work
                        ) { backStackEntry ->
                            val video1UriString = backStackEntry.arguments?.getString("video1Uri")
                            val video2UriString = backStackEntry.arguments?.getString("video2Uri")
                            val currentStart1Ms = backStackEntry.arguments?.getString("currentStart1Ms")?.toLongOrNull() ?: 0L
                            val currentStart2Ms = backStackEntry.arguments?.getString("currentStart2Ms")?.toLongOrNull() ?: 0L
                            val duration1Ms = backStackEntry.arguments?.getString("duration1Ms")?.toLongOrNull() ?: 0L
                            val duration2Ms = backStackEntry.arguments?.getString("duration2Ms")?.toLongOrNull() ?: 0L

                            StartPositionSettingScreen(
                                navController = navController,
                                video1UriString = video1UriString,
                                video2UriString = video2UriString,
                                initialStart1Ms = currentStart1Ms, initialStart2Ms = currentStart2Ms,
                                duration1Ms = duration1Ms, duration2Ms = duration2Ms
                            )
                        }
                    }
                }
            }
        }
    }
}
