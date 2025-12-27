package com.example.swimminganalysisapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.swimminganalysisapplication.ui.analysis.AnalysisDetailScreen
import com.example.swimminganalysisapplication.ui.analysis.AnalysisListScreen
import com.example.swimminganalysisapplication.ui.analysis.AnalysisProgressScreen
import com.example.swimminganalysisapplication.ui.analysis.AnalysisProgressViewModelFactory
import com.example.swimminganalysisapplication.ui.analysis.AnalysisViewModel
import com.example.swimminganalysisapplication.ui.analysis.AnalysisViewModelFactory
import com.example.swimminganalysisapplication.ui.analysis.SingleAnalysisResultScreen
import com.example.swimminganalysisapplication.ui.analysis.SingleAnalysisResultViewModelFactory
import com.example.swimminganalysisapplication.ui.analysis.SingleAnalysisSetupScreen
import com.example.swimminganalysisapplication.ui.analysis.SingleAnalysisSetupViewModelFactory
import com.example.swimminganalysisapplication.ui.home.HomeScreen
import com.example.swimminganalysisapplication.ui.login.CreateAccountScreen
import com.example.swimminganalysisapplication.ui.login.LoginScreen
import com.example.swimminganalysisapplication.ui.login.LoginViewModelFactory
import com.example.swimminganalysisapplication.ui.menucomments.MenuCommentsScreen
import com.example.swimminganalysisapplication.ui.menucomments.MenuCommentsViewModelFactory
import com.example.swimminganalysisapplication.ui.player.PlayerEditScreen
import com.example.swimminganalysisapplication.ui.player.PlayerListScreen
import com.example.swimminganalysisapplication.ui.player.PlayerViewModelFactory
import com.example.swimminganalysisapplication.ui.practicemenu.CreatePracticeMenuScreen
import com.example.swimminganalysisapplication.ui.practicemenu.CreatePracticeMenuViewModelFactory
import com.example.swimminganalysisapplication.ui.practicemenu.DiscoverScreen
import com.example.swimminganalysisapplication.ui.practicemenu.DiscoverViewModelFactory
import com.example.swimminganalysisapplication.ui.practicemenu.FavoritesScreen
import com.example.swimminganalysisapplication.ui.practicemenu.PracticeListScreen
import com.example.swimminganalysisapplication.ui.practicemenu.PracticeListViewModelFactory
import com.example.swimminganalysisapplication.ui.theme.SwimmingAnalysisApplicationTheme
import com.example.swimminganalysisapplication.ui.video.StartPositionSettingScreen
import com.example.swimminganalysisapplication.ui.video.VideoScreen
import com.example.swimminganalysisapplication.ui.video.ProjectListScreen

class MainActivity : ComponentActivity() {

    private val swimmingRepository by lazy { SwimmingRepository(RetrofitClient.getInstance(applicationContext)) }
    private val userPreferences: UserPreferences by lazy { UserPreferences(applicationContext) }
    private val createPracticeMenuViewModelFactory by lazy { CreatePracticeMenuViewModelFactory(swimmingRepository) }
    private val loginViewModelFactory: LoginViewModelFactory by lazy { LoginViewModelFactory(swimmingRepository, userPreferences) }
    private val playerViewModelFactory: PlayerViewModelFactory by lazy { PlayerViewModelFactory(swimmingRepository, userPreferences) }
    private val singleAnalysisSetupViewModelFactory: SingleAnalysisSetupViewModelFactory by lazy { SingleAnalysisSetupViewModelFactory(swimmingRepository, userPreferences) }
    private val practiceListViewModelFactory: PracticeListViewModelFactory by lazy { PracticeListViewModelFactory(swimmingRepository) }
    private val discoverViewModelFactory: DiscoverViewModelFactory by lazy { DiscoverViewModelFactory(swimmingRepository) }
    private val analysisViewModelFactory: AnalysisViewModelFactory by lazy { AnalysisViewModelFactory(swimmingRepository) }


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
                    AppNavigationHost()
                }
            }
        }
    }

    @Composable
    private fun AppNavigationHost() {
        val navController = rememberNavController()
        val selectedNavItem = remember { mutableStateOf(0) }
        
        // Observe current route to update bottom nav selection
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        
        Scaffold(
            bottomBar = {
                // Only show bottom nav if not on login/create account screens
                if (currentRoute?.startsWith(AppDestinations.LOGIN_SCREEN_ROUTE) != true &&
                    currentRoute?.startsWith(AppDestinations.CREATE_ACCOUNT_SCREEN_ROUTE) != true) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Home, contentDescription = "HOME") },
                            label = { Text("HOME") },
                            selected = selectedNavItem.value == 0,
                            onClick = {
                                selectedNavItem.value = 0
                                navController.navigate(AppDestinations.HOME_SCREEN_ROUTE) {
                                    popUpTo(AppDestinations.HOME_SCREEN_ROUTE) { inclusive = true }
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Analytics, contentDescription = "単体解析") },
                            label = { Text("単体解析") },
                            selected = selectedNavItem.value == 1,
                            onClick = {
                                selectedNavItem.value = 1
                                navController.navigate(AppDestinations.SINGLE_ANALYSIS_SETUP_ROUTE) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Compare, contentDescription = "比較解析") },
                            label = { Text("比較解析") },
                            selected = selectedNavItem.value == 2,
                            onClick = {
                                selectedNavItem.value = 2
                                navController.navigate(AppDestinations.PROJECT_LIST_SCREEN_ROUTE) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.PostAdd, contentDescription = "練習メニュー") },
                            label = { Text("練習メニュー") },
                            selected = selectedNavItem.value == 3,
                            onClick = {
                                selectedNavItem.value = 3
                                navController.navigate(AppDestinations.PRACTICE_LIST_SCREEN_ROUTE) {
                                    launchSingleTop = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "選手管理") },
                            label = { Text("選手管理") },
                            selected = selectedNavItem.value == 4,
                            onClick = {
                                selectedNavItem.value = 4
                                navController.navigate(AppDestinations.PLAYER_LIST_SCREEN_ROUTE) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                NavHost(
                                navController = navController,
                                startDestination = AppDestinations.LOGIN_SCREEN_ROUTE
                            ) {
                        composable(
                            route = AppDestinations.MENU_COMMENTS_WITH_ARG_ROUTE,
                            arguments = listOf(navArgument("menuId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val menuIdString = backStackEntry.arguments?.getString("menuId")

                            if (menuIdString != null) {
                                Log.d("MainActivity", "NavHost: Navigated to menu_comments with menuId (String): $menuIdString")
                                val factory = MenuCommentsViewModelFactory(swimmingRepository, menuIdString)
                                MenuCommentsScreen(
                                    navController = navController,
                                    viewModel = viewModel(factory = factory)
                                )
                            } else {
                                Log.e("MainActivity", "NavHost: menuIdString is null for MENU_COMMENTS_WITH_ARG_ROUTE.")
                                navController.popBackStack()
                            }
                        }
                        composable(AppDestinations.LOGIN_SCREEN_ROUTE) {
                            LoginScreen(
                                navController = navController,
                                viewModel = viewModel(factory = loginViewModelFactory)
                            )
                        }
                        composable(AppDestinations.CREATE_ACCOUNT_SCREEN_ROUTE) {
                            CreateAccountScreen(onNavigateBack = { navController.popBackStack() })
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
                        composable(AppDestinations.DISCOVER_SCREEN_ROUTE) {
                            DiscoverScreen(
                                navController = navController,
                                viewModel = viewModel(factory = discoverViewModelFactory)
                            )
                        }
                        composable(
                            // ★★★ ここを修正: 不正な文字列結合を削除 ★★★
                            route = AppDestinations.CREATE_PRACTICE_MENU_ROUTE,
                            arguments = listOf(navArgument("menuId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            })
                        ) { backStackEntry ->
                            val menuId = backStackEntry.arguments?.getString("menuId")
                            CreatePracticeMenuScreen(
                                navController = navController,
                                menuId = menuId,
                                viewModel = viewModel(factory = createPracticeMenuViewModelFactory)
                            )
                        }
                        composable(AppDestinations.PRACTICE_LIST_SCREEN_ROUTE) {
                            PracticeListScreen(
                                navController = navController,
                                viewModel = viewModel(factory = practiceListViewModelFactory)
                            )
                        }
                        composable(AppDestinations.FAVORITES_SCREEN_ROUTE) {
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
                        composable(AppDestinations.PLAYER_EDIT_SCREEN_ROUTE) { // For creating new player
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
                                navController = navController,
                                viewModel = viewModel(factory = analysisViewModelFactory)
                            )
                        }
                        composable(
                            route = "${AppDestinations.ANALYSIS_DETAIL_SCREEN_ROUTE}/{videoId}/{videoUuid}",
                            arguments = listOf(
                                navArgument("videoId") { type = NavType.IntType },
                                navArgument("videoUuid") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val videoId = backStackEntry.arguments?.getInt("videoId")
                            val videoUuid = backStackEntry.arguments?.getString("videoUuid")
                            if (videoId != null && videoUuid != null) {
                                AnalysisDetailScreen(
                                    videoId = videoId,
                                    videoUuid = videoUuid,
                                    navController = navController,
                                    viewModel = viewModel(factory = analysisViewModelFactory)
                                )
                            } else {
                                navController.popBackStack()
                            }
                        }
                        composable(AppDestinations.SINGLE_ANALYSIS_SETUP_ROUTE) {
                            SingleAnalysisSetupScreen(
                                viewModel = viewModel(factory = singleAnalysisSetupViewModelFactory),
                                navController = navController
                            )
                        }
                        composable(
                            route = "${AppDestinations.ANALYSIS_PROGRESS_ROUTE}/{analysisId}",
                            arguments = listOf(navArgument("analysisId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val analysisId = backStackEntry.arguments?.getInt("analysisId")
                            if (analysisId != null) {
                                val factory = AnalysisProgressViewModelFactory(swimmingRepository, analysisId)
                                AnalysisProgressScreen(
                                    viewModel = viewModel(factory = factory),
                                    navController = navController
                                )
                            } else {
                                Log.e("MainActivity", "NavHost: analysisId is null for ANALYSIS_PROGRESS_ROUTE.")
                            }
                        }
                        composable(
                            route = "${AppDestinations.SINGLE_ANALYSIS_RESULT_ROUTE}/{analysisId}",
                            arguments = listOf(navArgument("analysisId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val analysisId = backStackEntry.arguments?.getInt("analysisId")
                            if (analysisId != null) {
                                val factory = SingleAnalysisResultViewModelFactory(swimmingRepository, analysisId)
                                SingleAnalysisResultScreen(
                                    viewModel = viewModel(factory = factory),
                                    navController = navController
                                )
                            }
                        }
                    composable(AppDestinations.PROJECT_LIST_SCREEN_ROUTE) {
                        ProjectListScreen(
                            navController = navController
                        )
                    }
                    composable(
                        route = AppDestinations.PROJECT_LOAD_ROUTE,
                        arguments = listOf(navArgument("projectId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val projectId = backStackEntry.arguments?.getInt("projectId")
                        if (projectId != null) {
                            VideoScreen(navController = navController, projectId = projectId)
                        } else {
                            VideoScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}