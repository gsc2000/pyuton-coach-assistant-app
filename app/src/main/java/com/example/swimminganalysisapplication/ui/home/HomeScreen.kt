package com.example.swimminganalysisapplication.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import com.example.swimminganalysisapplication.ui.common.FeatureLockDialog
import com.example.swimminganalysisapplication.ui.theme.CustomTopAppBarHeight
import com.example.swimminganalysisapplication.ui.theme.getCustomTopAppBarColors
import com.example.swimminganalysisapplication.data.storage.UserPreferences
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

private const val TAG_HOME = "HomeScreen"

data class QuickAccessItem(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val requiresAuth: Boolean = false
)

data class RecentActivity(
    val title: String,
    val subtitle: String,
    val time: String,
    val icon: ImageVector
)

data class NotificationItem(
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val isGuestUser by userPreferences.isGuestUser.collectAsState(initial = false)
    var showLockDialog by remember { mutableStateOf(false) }

    // クイックアクセスアイテム
    val quickAccessItems = listOf(
        QuickAccessItem("動画比較", Icons.Filled.Compare, AppDestinations.PROJECT_LIST_SCREEN_ROUTE),
        QuickAccessItem("単体解析", Icons.Filled.Analytics, AppDestinations.SINGLE_ANALYSIS_SETUP_ROUTE, true),
        QuickAccessItem("練習メニュー", Icons.Filled.PostAdd, AppDestinations.PRACTICE_LIST_SCREEN_ROUTE, true),
        QuickAccessItem("選手管理", Icons.Filled.AccountCircle, AppDestinations.PLAYER_LIST_SCREEN_ROUTE, true)
    )

    // サンプル最近のアクティビティ（後で実データに置き換え）
    val recentActivities = remember {
        listOf(
            RecentActivity("動画解析完了", "バタフライ - 25m", "2時間前", Icons.Filled.CheckCircle),
            RecentActivity("練習メニュー作成", "週間トレーニングプラン", "1日前", Icons.Filled.PostAdd),
            RecentActivity("選手データ更新", "田中選手のプロフィール", "2日前", Icons.Filled.Person)
        )
    }

    // サンプル通知（後で実データに置き換え）
    val notifications = remember {
        listOf(
            NotificationItem("新機能追加", "AI解析機能が向上しました", "3時間前", false),
            NotificationItem("システムメンテナンス", "明日23:00-24:00にメンテナンスを実施", "5時間前", false),
            NotificationItem("解析完了", "動画解析が完了しました", "1日前", true)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ホーム") },
                colors = getCustomTopAppBarColors(),
                actions = {
                    IconButton(onClick = { /* 通知画面へ遷移 */ }) {
                        BadgedBox(
                            badge = {
                                if (notifications.count { !it.isRead } > 0) {
                                    Badge { Text("${notifications.count { !it.isRead }}") }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "通知")
                        }
                    }
                    AccountActionsMenu(navController = navController)
                },
                modifier = Modifier.heightIn(max = CustomTopAppBarHeight)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // ウェルカムセクション
            item {
                WelcomeCard(isGuestUser)
            }

            // クイックアクセスセクション
            item {
                Text(
                    text = "クイックアクセス",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quickAccessItems) { item ->
                        QuickAccessCard(
                            item = item,
                            isGuestUser = isGuestUser,
                            onClick = {
                                if (item.requiresAuth && isGuestUser) {
                                    showLockDialog = true
                                } else {
                                    navController.navigate(item.route)
                                }
                            }
                        )
                    }
                }
            }

            // 最近のアクティビティセクション
            item {
                Text(
                    text = "最近のアクティビティ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(recentActivities.take(3)) { activity ->
                RecentActivityCard(activity)
            }

            // 通知セクション
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "お知らせ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    TextButton(onClick = { /* 全ての通知を見る */ }) {
                        Text("すべて見る")
                    }
                }
            }

            items(notifications.take(2)) { notification ->
                NotificationCard(notification)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ロック機能ダイアログ
    FeatureLockDialog(
        isOpen = showLockDialog,
        onDismiss = { showLockDialog = false },
        onNavigateToSignUp = {
            navController.navigate(AppDestinations.CREATE_ACCOUNT_SCREEN_ROUTE)
        }
    )
}

@Composable
fun WelcomeCard(isGuestUser: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isGuestUser) "ゲストモード" else "ようこそ！",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isGuestUser) 
                        "会員登録で全機能を利用可能" 
                    else 
                        "今日も最高のトレーニングを",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun QuickAccessCard(
    item: QuickAccessItem,
    isGuestUser: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(32.dp),
                    tint = if (item.requiresAuth && isGuestUser) 
                        MaterialTheme.colorScheme.outline 
                    else 
                        MaterialTheme.colorScheme.primary
                )
                if (item.requiresAuth && isGuestUser) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "ロック",
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecentActivityCard(activity: RecentActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = activity.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(8.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = activity.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = activity.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
