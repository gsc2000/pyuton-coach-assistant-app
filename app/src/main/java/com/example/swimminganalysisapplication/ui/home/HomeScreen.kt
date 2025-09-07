package com.example.swimminganalysisapplication.ui.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp // ExitToApp アイコンをインポート
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Settings // Settings アイコンをインポート
import androidx.compose.material3.*
import androidx.compose.runtime.* // remember, mutableStateOf, getValue, setValue をインポート
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations

private const val TAG_HOME = "HomeScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var showMenu by remember { mutableStateOf(false) } // ドロップダウンメニューの表示状態を管理

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pyuton Coach Assistant") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    Box { // IconButtonとDropdownMenuを同じBox内に配置して位置を調整
                        IconButton(onClick = { showMenu = true }) { // クリックでメニューを表示
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "アカウント情報"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false } // メニュー外をタップで非表示
                        ) {
                            DropdownMenuItem(
                                text = { Text("設定") },
                                onClick = {
                                    showMenu = false
                                    Log.d(TAG_HOME, "Settings option clicked.")
                                    // TODO: 設定画面への遷移を実装
                                    // navController.navigate("settings_screen_route")
                                },
                                leadingIcon = { // アイコンを追加 (オプション)
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = "設定アイコン"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("ログアウト") },
                                onClick = {
                                    showMenu = false
                                    Log.d(TAG_HOME, "Logout option clicked.")
                                    navController.navigate(AppDestinations.LOGIN_SCREEN_ROUTE) {
                                        popUpTo(AppDestinations.HOME_SCREEN_ROUTE) {
                                            inclusive = true // ホーム画面もスタックから消す
                                        }
                                        // または popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                        // launchSingleTop = true // LOGIN_SCREEN_ROUTEが既にスタックにあれば再利用
                                    }
                                },
                                leadingIcon = { // アイコンを追加 (オプション)
                                    Icon(
                                        Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "ログアウトアイコン"
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { this.contentDescription = "アプリロゴ - 各種スポーツアイコン" }
            ) {
                Icon(
                    imageVector = Icons.Filled.Pool,
                    contentDescription = "スイミングアイコン",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = "フィットネスアイコン",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.DirectionsRun,
                    contentDescription = "ランニングアイコン",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "ようこそ！",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "AIが導き、つながりが広げる\nコーチングの未来",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            HomeNavigationCard(
                title = "動画解析を開始",
                icon = Icons.Filled.Analytics,
                onClick = { navController.navigate(AppDestinations.VIDEO_SCREEN_ROUTE) }
            )

            HomeNavigationCard(
                title = "選手一覧",
                icon = Icons.AutoMirrored.Filled.ListAlt,
                onClick = { navController.navigate(AppDestinations.SWIMMERS_SCREEN_ROUTE) }
            )

            HomeNavigationCard(
                title = "練習メニュー",
                icon = Icons.Filled.PostAdd,
                onClick = { navController.navigate(AppDestinations.PRACTICE_LIST_SCREEN_ROUTE) }
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeNavigationCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
