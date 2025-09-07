package com.example.swimminganalysisapplication.ui.menucomments

import android.util.Log // デバッグ用
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import com.example.swimminganalysisapplication.ui.practicemenu.Comment
import com.example.swimminganalysisapplication.ui.practicemenu.PracticeMenuItemDisplay
// ★★★ 正しい PracticeMenuRepository をインポート ★★★
import com.example.swimminganalysisapplication.ui.practicemenu.PracticeMenuRepository
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ★★★ このファイル内にあった PracticeMenuRepository の object 定義は削除されているはずです ★★★
// object PracticeMenuRepository { ... } // ← これがないことを確認！

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCommentsScreen(
    navController: NavController,
    menuId: String?,
) {
    // デバッグログ
    Log.d("MenuCommentsScreen", "Screen Composed. Received menuId: $menuId")
    Log.d("MenuCommentsScreen", "Shared PracticeMenuRepository instance: ${PracticeMenuRepository.hashCode()}") // インスタンス確認
    Log.d("MenuCommentsScreen", "Menus in shared repository: ${PracticeMenuRepository.menus.map { it.id to it.title }}")

    // 共有された PracticeMenuRepository インスタンスを使用
    val menu = remember(menuId) { PracticeMenuRepository.findMenuById(menuId) }

    Log.d("MenuCommentsScreen", "Menu found by ID '$menuId': ${menu?.title}")

    if (menu == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("エラー") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("メニューが見つかりませんでした。 (ID: $menuId)")
            }
        }
        return
    }

    var newCommentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${menu.title} - コメント") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = { AccountActionsMenu(navController = navController) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("コメントを追加...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                val comment = Comment(authorName = "現在のユーザー", text = newCommentText) // 仮の投稿者
                                PracticeMenuRepository.addCommentToMenu(menuId, comment) // 正しいリポジトリのメソッドを呼ぶ
                                newCommentText = ""
                            }
                        },
                        enabled = newCommentText.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "コメント送信")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            if (menu.comments.isEmpty()) {
                item {
                    Text(
                        "まだコメントはありません。",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(menu.comments.sortedByDescending { it.timestamp }, key = { it.id }) { comment -> // コメントを新しい順にソートする例
                    CommentItem(comment)
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(comment.authorName, style = MaterialTheme.typography.titleSmall)
                Text(
                    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(comment.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
