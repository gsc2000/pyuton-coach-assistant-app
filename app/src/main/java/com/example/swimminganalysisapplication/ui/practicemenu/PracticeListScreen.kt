package com.example.swimminganalysisapplication.ui.practicemenu

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items をインポート
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit // Edit アイコンを追加 (オプション)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations
import java.util.UUID // UUIDをインポート

private const val TAG = "PracticeListScreen"

// 各練習メニューを表すデータクラス (仮)
data class PracticeMenuItemDisplay(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null // 詳細表示用に説明も追加 (オプション)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(navController: NavController) {
    // IDを持つデータクラスのリストに変更
    var practiceMenus by remember {
        mutableStateOf(listOf(
            PracticeMenuItemDisplay(title = "メニュー1: バタフライ 100m x 5", description = "インターバル 2:00"),
            PracticeMenuItemDisplay(title = "メニュー2: クロール 200m x 3", description = "レスト 30秒")
        ))
    }

    val currentBackStackEntry = navController.currentBackStackEntry
    DisposableEffect(currentBackStackEntry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (currentBackStackEntry?.savedStateHandle?.contains("newPracticeMenuAdded") == true) {
                    val newMenuAdded = currentBackStackEntry.savedStateHandle.get<Boolean>("newPracticeMenuAdded")
                    if (newMenuAdded == true) {
                        Log.d(TAG, "New practice menu was added, refresh the list.")
                        // TODO: ここで実際に保存されたメニューを取得・更新する
                        // 今回はダミーデータを追加してシミュレート
                        val newTitle = currentBackStackEntry.savedStateHandle.get<String>("newMenuTitle") ?: "新しいメニュー (タイトルなし)"
                        practiceMenus = practiceMenus + PracticeMenuItemDisplay(title = newTitle, description = "新規追加")
                        currentBackStackEntry.savedStateHandle.remove<Boolean>("newPracticeMenuAdded")
                        currentBackStackEntry.savedStateHandle.remove<String>("newMenuTitle")
                    }
                }
            }
        }
        currentBackStackEntry?.lifecycle?.addObserver(observer)
        onDispose { currentBackStackEntry?.lifecycle?.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("練習メニュー 一覧") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton( // FloatingActionButton を ExtendedFloatingActionButton に変更
                onClick = {
                    // 新規作成なのでIDは渡さない（または "new" のような特殊な値を渡す）
                    navController.navigate(AppDestinations.CREATE_PRACTICE_MENU_ROUTE)
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = "メニュー作成アイコン") },
                text = { Text("メニュー作成") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp), // 横パディングを少し削減
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (practiceMenus.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("練習メニューはまだありません。")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp) // リスト上下のパディング
                ) {
                    items(practiceMenus, key = { it.id }) { menu ->
                        MenuListItem(
                            item = menu,
                            onClick = {
                                // 編集画面へ遷移 (menuIdを渡す)
                                navController.navigate("${AppDestinations.CREATE_PRACTICE_MENU_ROUTE}?menuId=${menu.id}")
                                Log.d(TAG, "Tapped on menu: ${menu.title}, id: ${menu.id}")
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun MenuListItem(
    item: PracticeMenuItemDisplay,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            if (item.description != null) {
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Filled.Edit, contentDescription = "編集", tint = MaterialTheme.colorScheme.primary) // 編集アイコン（見た目用）
    }
}
