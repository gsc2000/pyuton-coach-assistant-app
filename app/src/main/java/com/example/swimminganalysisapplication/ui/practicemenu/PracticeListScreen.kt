package com.example.swimminganalysisapplication.ui.practicemenu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
// ★★★ ここから追加 ★★★
import androidx.compose.material.icons.filled.TravelExplore
// ★★★ ここまで追加 ★★★
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Menu
import com.example.swimminganalysisapplication.navigation.AppDestinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(
    navController: NavController,
    viewModel: PracticeListViewModel
) {
    val menus by viewModel.practiceMenus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("練習メニュー 一覧") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                // ★★★ ここから修正 ★★★
                actions = {
                    IconButton(onClick = { navController.navigate(AppDestinations.DISCOVER_SCREEN_ROUTE) }) {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = "他のユーザーのメニューを探す",
                            tint = Color.White // アイコンの色を戻るボタンと合わせる
                        )
                    }
                },
                // ★★★ ここまで修正 ★★★
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate("${AppDestinations.CREATE_PRACTICE_MENU_ROUTE}?menuId=-1")
            }) {
                Icon(Icons.Filled.Add, contentDescription = "練習メニューを作成")
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
            } else if (menus.isEmpty()) {
                Text(text = "練習メニューがありません。")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(menus) { menu ->
                        PracticeMenuItem(
                            menu = menu,
                            onItemClick = {
                                // カード全体をクリックしたら編集画面へ
                                navController.navigate("${AppDestinations.CREATE_PRACTICE_MENU_ROUTE}?menuId=${menu.menuId}")
                            },
                            onCommentClick = {
                                // コメントアイコンをクリックしたらコメント画面へ
                                navController.navigate(
                                    AppDestinations.MENU_COMMENTS_WITH_ARG_ROUTE.replace("{menuId}", menu.menuId.toString())
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeMenuItem(
    menu: Menu,
    onItemClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    // 説明文から ---items--- 以降を取り除く
    val displayDescription = menu.menuDescription?.split("---items---")?.getOrNull(0)?.trim() ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick), // カード全体をクリック可能に
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp), // 右のパディングを調整
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側のテキスト部分
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = menu.menuTitle ?: "（無題）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (displayDescription.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = displayDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            // ★★★ 右側のコメントボタン ★★★
            IconButton(onClick = onCommentClick) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "コメント",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}