package com.example.swimminganalysisapplication.ui.practicemenu

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import java.util.UUID

private const val TAG = "DiscoverScreen" // TAG定義

data class PublicPracticeMenu(
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
            PublicPracticeMenu(originalId="pub1", title = "公開メニューA: 全力スプリントセット", description = "短距離向け高強度トレーニング", originalAuthorName = "山田コーチ", tags = listOf("スプリント", "クロール")),
            PublicPracticeMenu(originalId="pub2", title = "公開メニューB: 長距離持久力向上", description = "試合後半でもバテない体を作る", originalAuthorName = "鈴木トレーナー", tags = listOf("長距離", "持久力", "IM")),
            PublicPracticeMenu(originalId="pub3", title = "公開メニューC: テクニックドリル集", description = "各種目のフォーム改善に", originalAuthorName = "高橋指導員", tags = listOf("ドリル", "フォーム改善")),
            PublicPracticeMenu(originalId="pub4", title = "公開メニューD: 初心者向け基礎練習", description = "水慣れから各種目導入まで", originalAuthorName = "佐藤コーチ", tags = listOf("初心者", "基礎"))
        )
    }

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
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            handle["forkedMenuTitle"] = menuToFork.title // IDEの提案に従い代入演算子を使用
                            handle["forkedMenuDescription"] = menuToFork.description
                            handle["forkedMenuOriginalAuthor"] = menuToFork.originalAuthorName
                            handle["forkedMenuOriginalId"] = menuToFork.originalId
                            handle["newMenuForked"] = true
                            Log.d(TAG, "Forking menu: ${menuToFork.title}. Passing data back via SavedStateHandle. Keys set: ${handle.keys()}")
                        } ?: Log.e(TAG, "previousBackStackEntry or savedStateHandle is null when trying to set fork data!")

                        showForkConfirmationDialog = null
                        navController.popBackStack()
                        Log.d(TAG, "Popped back from DiscoverScreen after fork confirmation.")
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
                val isFavorited by remember {
                    derivedStateOf { PracticeMenuRepository.isFavorite(menu.originalId) }
                }
                PublicMenuListItem(
                    menu = menu,
                    isFavorited = isFavorited,
                    onForkClick = {
                        Log.d(TAG, "Fork button clicked in list item for: ${menu.title} (ID: ${menu.originalId})")
                        showForkConfirmationDialog = menu
                    },
                    onToggleFavorite = {
                        PracticeMenuRepository.toggleFavorite(menu.originalId)
                    }
                )
            }
        }
    }
}

@Composable
fun PublicMenuListItem(
    menu: PublicPracticeMenu,
    isFavorited: Boolean,
    onForkClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(menu.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "作成者: ${menu.originalAuthorName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorited) "お気に入り解除" else "お気に入り登録",
                        tint = if (isFavorited) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

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
                onClick = onForkClick, // ここで onForkClick が呼ばれる
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("自分のメニューにフォーク")
            }
        }
    }
}
