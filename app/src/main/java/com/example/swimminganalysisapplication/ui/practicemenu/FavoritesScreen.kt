package com.example.swimminganalysisapplication.ui.practicemenu

import android.util.Log // ★ Log をインポート
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.filled.Star // PublicMenuListItem内で使用
// import androidx.compose.material.icons.outlined.StarBorder // PublicMenuListItem内で使用
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// import androidx.compose.ui.graphics.Color // PublicMenuListItem内で使用
// import androidx.compose.ui.text.font.FontStyle // PublicMenuListItem内で使用
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu // TopAppBar内で使用

private const val TAG = "FavoritesScreen" // ★ TAG定義

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    // このダミーデータは DiscoverScreen と同じもの。プロトタイプ用。
    val allPublicMenus = remember {
        listOf(
            PublicPracticeMenu(originalId="pub1", title = "公開メニューA: 全力スプリントセット", description = "短距離向け高強度トレーニング", originalAuthorName = "山田コーチ", tags = listOf("スプリント", "クロール")),
            PublicPracticeMenu(originalId="pub2", title = "公開メニューB: 長距離持久力向上", description = "試合後半でもバテない体を作る", originalAuthorName = "鈴木トレーナー", tags = listOf("長距離", "持久力", "IM")),
            PublicPracticeMenu(originalId="pub3", title = "公開メニューC: テクニックドリル集", description = "各種目のフォーム改善に", originalAuthorName = "高橋指導員", tags = listOf("ドリル", "フォーム改善")),
            PublicPracticeMenu(originalId="pub4", title = "公開メニューD: 初心者向け基礎練習", description = "水慣れから各種目導入まで", originalAuthorName = "佐藤コーチ", tags = listOf("初心者", "基礎"))
        )
    }

    val favoritedMenus by remember {
        derivedStateOf {
            val favoriteIds = PracticeMenuRepository.favoritedMenuOriginalIds
            allPublicMenus.filter { publicMenu -> favoriteIds.contains(publicMenu.originalId) }
        }
    }

    // ★★★ DiscoverScreen と同様のダイアログ状態と AlertDialog ロジックを追加 ★★★
    var showForkConfirmationDialog by remember { mutableStateOf<PublicPracticeMenu?>(null) }

    if (showForkConfirmationDialog != null) {
        val menuToFork = showForkConfirmationDialog!!
        AlertDialog(
            onDismissRequest = {
                Log.d(TAG, "Fork confirmation dialog dismissed.")
                showForkConfirmationDialog = null
            },
            title = { Text("メニューをフォーク") },
            text = { Text("「${menuToFork.title}」を自分の練習メニューに追加しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        Log.d(TAG, "Confirm fork button clicked for: ${menuToFork.title}")
                        // ★ DiscoverScreen と同じロジックで SavedStateHandle にデータをセット
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            handle["forkedMenuTitle"] = menuToFork.title
                            handle["forkedMenuDescription"] = menuToFork.description
                            handle["forkedMenuOriginalAuthor"] = menuToFork.originalAuthorName
                            handle["forkedMenuOriginalId"] = menuToFork.originalId
                            handle["newMenuForked"] = true
                            Log.d(TAG, "Forking menu: ${menuToFork.title} from FavoritesScreen. Passing data back. Keys set: ${handle.keys()}")
                        } ?: Log.e(TAG, "previousBackStackEntry or savedStateHandle is null when trying to set fork data from FavoritesScreen!")

                        showForkConfirmationDialog = null
                        navController.popBackStack()
                        Log.d(TAG, "Popped back from FavoritesScreen after fork confirmation.")
                    }
                ) { Text("フォークする") }
            },
            dismissButton = {
                Button(onClick = {
                    Log.d(TAG, "Cancel fork button clicked in dialog.")
                    showForkConfirmationDialog = null
                }) { Text("キャンセル") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("お気に入りメニュー") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = { AccountActionsMenu(navController = navController) }, // オプション
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (favoritedMenus.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("お気に入り登録されたメニューはありません。")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favoritedMenus, key = { it.originalId }) { menu ->
                    val isFavorited by remember { // This will always be true here, but needed for the component
                        derivedStateOf { PracticeMenuRepository.isFavorite(menu.originalId) }
                    }

                    PublicMenuListItem(
                        menu = menu,
                        isFavorited = isFavorited,
                        onForkClick = { // ★★★ onForkClick の実装 ★★★
                            Log.d(TAG, "Fork button clicked in list item for: ${menu.title} (ID: ${menu.originalId}) on FavoritesScreen")
                            showForkConfirmationDialog = menu // ダイアログ表示のトリガー
                        },
                        onToggleFavorite = {
                            PracticeMenuRepository.toggleFavorite(menu.originalId)
                            // derivedStateOf によりリストは自動的に再コンポーズされる
                        }
                    )
                }
            }
        }
    }
}

