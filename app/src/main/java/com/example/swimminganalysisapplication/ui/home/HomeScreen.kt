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
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu

private const val TAG_HOME = "HomeScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AnalysisTypeSelectionDialog(
            onDismissRequest = { showDialog = false },
            onSingleAnalysisClick = {
                showDialog = false
                navController.navigate(AppDestinations.SINGLE_ANALYSIS_SETUP_ROUTE)
            },
            onComparisonAnalysisClick = {
                showDialog = false
                navController.navigate(AppDestinations.VIDEO_SCREEN_ROUTE)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pyuton Coach Assistant") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    AccountActionsMenu(navController = navController)
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
                title = "解析",
                icon = Icons.Filled.Analytics,
                onClick = { showDialog = true }
            )

            HomeNavigationCard(
                title = "練習メニュー",
                icon = Icons.Filled.PostAdd,
                onClick = { navController.navigate(AppDestinations.PRACTICE_LIST_SCREEN_ROUTE) }
            )

            HomeNavigationCard(
                title = "選手管理",
                icon = Icons.Filled.AccountCircle,
                onClick = { navController.navigate(AppDestinations.PLAYER_LIST_SCREEN_ROUTE) }
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
