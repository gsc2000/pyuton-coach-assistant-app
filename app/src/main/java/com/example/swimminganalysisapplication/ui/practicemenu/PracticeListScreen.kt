package com.example.swimminganalysisapplication.ui.practicemenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.swimminganalysisapplication.ui.theme.CustomTopAppBarHeight
import com.example.swimminganalysisapplication.ui.theme.getCustomTopAppBarColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Menu
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(
    navController: NavController,
    viewModel: PracticeListViewModel = viewModel()
) {
    val practiceMenus by viewModel.practiceMenus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPracticeMenus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("練習メニュー一覧") },
                colors = getCustomTopAppBarColors(),
                modifier = Modifier.heightIn(max = CustomTopAppBarHeight),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(AppDestinations.DISCOVER_SCREEN_ROUTE) }) {
                        Icon(Icons.Default.Search, contentDescription = "メニューを探す")
                    }
                    AccountActionsMenu(navController = navController)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(AppDestinations.CREATE_PRACTICE_MENU_ROUTE_BASE) }) {
                Icon(Icons.Default.Add, contentDescription = "練習メニューを作成")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (practiceMenus.isEmpty()) {
                Text(text = "練習メニューがありません。")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(practiceMenus) { menu ->
                        PracticeMenuCard(
                            menu = menu,
                            onClick = {
                                val route = AppDestinations.CREATE_PRACTICE_MENU_ROUTE.replace("{menuId}", menu.menuId.toString())
                                navController.navigate(route)
                            },
                            // ★★★ コメント画面への遷移を追加 ★★★
                            onCommentClick = {
                                val route = AppDestinations.MENU_COMMENTS_WITH_ARG_ROUTE.replace("{menuId}", menu.menuId.toString())
                                navController.navigate(route)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeMenuCard(
    menu: Menu,
    onClick: () -> Unit,
    onCommentClick: () -> Unit // ★★★ パラメータを追加 ★★★
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // ★★★ Rowで囲み、ボタンを配置 ★★★
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = menu.menuTitle ?: "（無題）",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = menu.menuDescription?.split("---items---")?.getOrNull(0)?.trim() ?: "説明がありません",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // ★★★ コメントボタンを追加 ★★★
            IconButton(onClick = onCommentClick) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "コメントを見る",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}