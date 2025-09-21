package com.example.swimminganalysisapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.RetrofitClient
import com.example.swimminganalysisapplication.data.storage.UserPreferences
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.analysis.AnalysisListScreen
import com.example.swimminganalysisapplication.ui.analysis.AnalysisProgressScreen
import com.example.swimminganalysisapplication.ui.analysis.AnalysisProgressViewModelFactory
import com.example.swimminganalysisapplication.ui.analysis.SingleAnalysisResultScreen
import com.example.swimminganalysisapplication.ui.analysis.SingleAnalysisResultViewModelFactory
import com.example.swimminganalysisapplication.ui.analysis.SingleAnalysisSetupScreen
import com.example.swimminganalysisapplication.ui.analysis.SingleAnalysisSetupViewModelFactory
import com.example.swimminganalysisapplication.ui.home.HomeScreen
import com.example.swimminganalysisapplication.ui.login.CreateAccountScreen
import com.example.swimminganalysisapplication.ui.login.LoginScreen
import com.example.swimminganalysisapplication.ui.login.LoginViewModelFactory
import com.example.swimminganalysisapplication.ui.menucomments.MenuCommentsScreen
import com.example.swimminganalysisapplication.ui.player.PlayerEditScreen
import com.example.swimminganalysisapplication.ui.player.PlayerListScreen
import com.example.swimminganalysisapplication.ui.player.PlayerViewModelFactory
import com.example.swimminganalysisapplication.ui.practicemenu.CreatePracticeMenuScreen
import com.example.swimminganalysisapplication.ui.practicemenu.CreatePracticeMenuViewModelFactory
import com.example.swimminganalysisapplication.ui.practicemenu.DiscoverScreen
import com.example.swimminganalysisapplication.ui.practicemenu.FavoritesScreen
import com.example.swimminganalysisapplication.ui.practicemenu.PracticeListScreen
import com.example.swimminganalysisapplication.ui.theme.SwimmingAnalysisApplicationTheme
import com.example.swimminganalysisapplication.ui.video.StartPositionSettingScreen
import com.example.swimminganalysisapplication.ui.video.VideoScreen

class MainActivity : ComponentActivity() {

    private val swimmingRepository by lazy { SwimmingRepository(RetrofitClient.getInstance(applicationContext)) }
    private val userPreferences: UserPreferences by lazy { UserPreferences(applicationContext) }
    private val createPracticeMenuViewModelFactory by lazy { CreatePracticeMenuViewModelFactory(swimmingRepository) }
    private val loginViewModelFactory: LoginViewModelFactory by lazy { LoginViewModelFactory(swimmingRepository, userPreferences) }
    private val playerViewModelFactory: PlayerViewModelFactory by lazy { PlayerViewModelFactory(swimmingRepository) }
    private val singleAnalysisSetupViewModelFactory: SingleAnalysisSetupViewModelFactory by lazy { SingleAnalysisSetupViewModelFactory(swimmingRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
                        startDestination = AppDestinations.LOGIN_SCREEN_ROUTE // Start destination changed
                    ) {
                        composable(
                            route = AppDestinations.MENU_COMMENTS_WITH_ARG_ROUTE,
                            arguments = listOf(navArgument("menuId") { type = NavType.StringType; nullable = true })
                        ) { backStackEntry ->
                            val menuId = backStackEntry.arguments?.getString("menuId")
                            MenuCommentsScreen(navController = navController, menuId = menuId)
                        }
                        composable(AppDestinations.LOGIN_SCREEN_ROUTE) { // New route
                            LoginScreen(
                                navController = navController,
                                viewModel = viewModel(factory = loginViewModelFactory)
                            )
                        }
                        composable(AppDestinations.CREATE_ACCOUNT_SCREEN_ROUTE) { // New route
                            CreateAccountScreen(navController = navController)
                        }
                        composable(AppDestinations.HOME_SCREEN_ROUTE) {
                            HomeScreen(navController = navController)
                        }
                        composable(AppDestinations.VIDEO_SCREEN_ROUTE) {
                            VideoScreen(navController = navController)
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
                        // ★ DiscoverScreen のルートを追加
                        composable(AppDestinations.DISCOVER_SCREEN_ROUTE) {
                            DiscoverScreen(navController = navController)
                        }
                        composable(
                            route = AppDestinations.CREATE_PRACTICE_MENU_ROUTE, // 引数ありルート
                            arguments = listOf(navArgument("menuId") { type = NavType.StringType; nullable = true })
                        ) { backStackEntry ->
                            val menuId = backStackEntry.arguments?.getString("menuId")
                            CreatePracticeMenuScreen(
                                navController = navController,
                                practiceMenuId = menuId,
                                viewModel = viewModel(factory = createPracticeMenuViewModelFactory)
                            )
                        }
                        composable(AppDestinations.PRACTICE_LIST_SCREEN_ROUTE) {
                            PracticeListScreen(navController = navController)
                        }
                        composable(AppDestinations.FAVORITES_SCREEN_ROUTE) { // ★ これが追加されていることを確認
                            FavoritesScreen(navController = navController)
                        }
                        composable(AppDestinations.PLAYER_LIST_SCREEN_ROUTE) {
                            PlayerListScreen(
                                navController = navController,
                                viewModel = viewModel(factory = playerViewModelFactory)
                            )
                        }

                        composable(
                            route = "${AppDestinations.PLAYER_EDIT_SCREEN_ROUTE}/{playerId}",
                            arguments = listOf(navArgument("playerId") { type = NavType.StringType; nullable = true })
                        ) { backStackEntry ->
                            PlayerEditScreen(
                                navController = navController,
                                viewModel = viewModel(factory = playerViewModelFactory),
                                playerId = backStackEntry.arguments?.getString("playerId")
                            )
                        }
                        composable(AppDestinations.PLAYER_EDIT_SCREEN_ROUTE) {
                            PlayerEditScreen(
                                navController = navController,
                                viewModel = viewModel(factory = playerViewModelFactory),
                                playerId = null
                            )
                        }
                        composable(
                            route = AppDestinations.ANALYSIS_LIST_SCREEN_ROUTE
                        ) {
                            AnalysisListScreen(
                                navController = navController
                            )
                        }
                        composable(AppDestinations.SINGLE_ANALYSIS_SETUP_ROUTE) {
                            SingleAnalysisSetupScreen(
                                viewModel = viewModel(factory = singleAnalysisSetupViewModelFactory),
                                navController = navController
                            )
                        }
                        composable(
                            route = AppDestinations.ANALYSIS_PROGRESS_ROUTE + "/{analysisId}",
                            arguments = listOf(navArgument("analysisId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val analysisId = backStackEntry.arguments?.getInt("analysisId") ?: -1
                            AnalysisProgressScreen(
                                viewModel = viewModel(
                                    factory = AnalysisProgressViewModelFactory(
                                        swimmingRepository,
                                        analysisId
                                    )
                                ),
                                navController = navController
                            )
                        }
                        composable(
                            route = AppDestinations.SINGLE_ANALYSIS_RESULT_ROUTE + "/{analysisId}",
                            arguments = listOf(navArgument("analysisId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val analysisId = backStackEntry.arguments?.getInt("analysisId") ?: -1
                            SingleAnalysisResultScreen(
                                viewModel = viewModel(
                                    factory = SingleAnalysisResultViewModelFactory(
                                        swimmingRepository,
                                        analysisId
                                    )
                                ),
                                navController = navController
                            )
                        }
                    }
                }
            }
        }
    }
}