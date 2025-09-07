package com.example.swimminganalysisapplication.ui.practicemenu // ★ 修正されたパッケージ宣言

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations // これはパッケージ構成依存
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
// PracticeMenuItemDisplay は同じパッケージなので、明示的なインポートは不要な場合もあるが、
// 念のため記述しておくか、IDEの自動解決に任せる。
// import com.example.swimminganalysisapplication.ui.practicemenu.PracticeMenuItemDisplay
import java.util.UUID

private const val TAG = "DiscoverScreen"

// DiscoverScreenで表示するための仮の公開メニューデータ
data class PublicPracticeMenu( // このデータクラスはDiscoverScreen内でのみ使用
    val originalId: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String?,
    val originalAuthorName: String,
    val tags: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(navController: NavController) {
    val publicMenus = remember {
        listOf(
            PublicPracticeMenu(title = "公開メニューA: 全力スプリントセット", description = "短距離向け高強度トレーニング", originalAuthorName = "山田コーチ", tags = listOf("スプリント", "クロール")),
            PublicPracticeMenu(title = "公開メニューB: 長距離持久力向上", description = "試合後半でもバテない体を作る", originalAuthorName = "鈴木トレーナー", tags = listOf("長距離", "持久力", "IM")),
            PublicPracticeMenu(title = "公開メニューC: テクニックドリル集", description = "各種目のフォーム改善に", originalAuthorName = "高橋指導員", tags = listOf("ドリル", "フォーム改善")),
            PublicPracticeMenu(title = "公開メニューD: 初心者向け基礎練習", description = "水慣れから各種目導入まで", originalAuthorName = "佐藤コーチ", tags = listOf("初心者", "基礎"))
        )
    }

    var showForkConfirmationDialog by remember { mutableStateOf<PublicPracticeMenu?>(null) }

    if (showForkConfirmationDialog != null) {
        val menuToFork = showForkConfirmationDialog!!
        AlertDialog(
            onDismissRequest = { showForkConfirmationDialog = null },
            title = { Text("メニューをフォーク") },
            text = { Text("「${menuToFork.title}」を自分の練習メニューに追加しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        navController.previousBackStackEntry?.savedStateHandle?.set("forkedMenuTitle", menuToFork.title)
                        navController.previousBackStackEntry?.savedStateHandle?.set("forkedMenuDescription", menuToFork.description)
                        navController.previousBackStackEntry?.savedStateHandle?.set("forkedMenuOriginalAuthor", menuToFork.originalAuthorName)
                        navController.previousBackStackEntry?.savedStateHandle?.set("forkedMenuOriginalId", menuToFork.originalId) // 元IDも渡す
                        navController.previousBackStackEntry?.savedStateHandle?.set("newMenuForked", true)

                        Log.d(TAG, "Forking menu: ${menuToFork.title}. Passing data back to PracticeListScreen.")
                        showForkConfirmationDialog = null
                        navController.popBackStack()
                    }
                ) { Text("フォークする") }
            },
            dismissButton = {
                Button(onClick = { showForkConfirmationDialog = null }) { Text("キャンセル") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("公開メニューを探す") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    AccountActionsMenu(navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(publicMenus, key = { it.originalId }) { menu ->
                PublicMenuListItem(
                    menu = menu,
                    onForkClick = {
                        showForkConfirmationDialog = menu
                    }
                )
            }
        }
    }
}

@Composable
fun PublicMenuListItem( // このComposableはDiscoverScreen内でのみ使用
    menu: PublicPracticeMenu,
    onForkClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(menu.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "作成者: ${menu.originalAuthorName}",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic
            )
            if (!menu.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(menu.description, style = MaterialTheme.typography.bodyMedium)
            }
            if (menu.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    menu.tags.take(3).forEach { tag ->
                        SuggestionChip(onClick = { /* No action */ }, label = { Text(tag) })
                    }
                    if (menu.tags.size > 3) {
                        Text("...", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onForkClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("自分のメニューにフォーク")
            }
        }
    }
}

