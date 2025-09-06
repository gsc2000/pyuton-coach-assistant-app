package com.example.swimminganalysisapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.RetrofitClient
import com.example.swimminganalysisapplication.ui.addswimmer.AddSwimmerScreen
import com.example.swimminganalysisapplication.ui.addswimmer.AddSwimmerViewModelFactory
import com.example.swimminganalysisapplication.ui.home.HomeScreen
import com.example.swimminganalysisapplication.ui.practicemenu.PracticeListScreen // Corrected import
import com.example.swimminganalysisapplication.ui.practicemenu.CreatePracticeMenuScreen
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
                        composable(AppDestinations.HOME_SCREEN_ROUTE) {
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
                            route = "${AppDestinations.START_POSITION_SETTING_ROUTE}/{video1UriString}/{video2UriString}/{initialStart1Ms}/{initialStart2Ms}/{duration1Ms}/{duration2Ms}",
                            arguments = listOf(
                                navArgument("video1UriString") { type = NavType.StringType; nullable = true },
                                navArgument("video2UriString") { type = NavType.StringType; nullable = true },
                                navArgument("initialStart1Ms") { type = NavType.LongType },
                                navArgument("initialStart2Ms") { type = NavType.LongType },
                                navArgument("duration1Ms") { type = NavType.LongType },
                                navArgument("duration2Ms") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val video1UriStringArg = backStackEntry.arguments?.getString("video1UriString").let { if (it == "null") null else it }
                            val video2UriStringArg = backStackEntry.arguments?.getString("video2UriString").let { if (it == "null") null else it }
                            val initialStart1MsArg = backStackEntry.arguments?.getLong("initialStart1Ms") ?: 0L
                            val initialStart2MsArg = backStackEntry.arguments?.getLong("initialStart2Ms") ?: 0L
                            val duration1MsArg = backStackEntry.arguments?.getLong("duration1Ms") ?: 0L
                            val duration2MsArg = backStackEntry.arguments?.getLong("duration2Ms") ?: 0L

                            StartPositionSettingScreen(
                                navController = navController,
                                video1UriString = video1UriStringArg,
                                video2UriString = video2UriStringArg,
                                initialStart1Ms = initialStart1MsArg,
                                initialStart2Ms = initialStart2MsArg,
                                duration1Ms = duration1MsArg,
                                duration2Ms = duration2MsArg
                            )
                        }
                        // CreatePracticeMenuScreen のルート定義を更新
                        composable(
                            route = AppDestinations.CREATE_PRACTICE_MENU_ROUTE,
                            arguments = listOf(
                                navArgument("menuId") {
                                    type = NavType.StringType
                                    nullable = true // menuId はオプション
                                    defaultValue = null // デフォルトはnull（新規作成時）
                                }
                            )
                        ) { backStackEntry ->
                            val menuId = backStackEntry.arguments?.getString("menuId")
                            CreatePracticeMenuScreen(navController = navController, practiceMenuId = menuId)
                        }
                        composable(AppDestinations.PRACTICE_LIST_SCREEN_ROUTE) {
                            PracticeListScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}

