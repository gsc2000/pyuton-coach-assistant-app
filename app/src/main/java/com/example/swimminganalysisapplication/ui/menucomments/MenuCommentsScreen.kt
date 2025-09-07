package com.example.swimminganalysisapplication.ui.menucomments

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
import com.example.swimminganalysisapplication.ui.practicemenu.Comment // ★ Commentをインポート
import com.example.swimminganalysisapplication.ui.practicemenu.PracticeMenuItemDisplay // ★ PracticeMenuItemDisplayをインポート
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 仮のデータ置き場（ViewModelやRepositoryができるまでの代替）
// 実際にはViewModel経由でPracticeListScreenのデータを参照・更新する
object PracticeMenuRepository {
    var menus: MutableList<PracticeMenuItemDisplay> = mutableStateListOf() // PracticeListScreenのpracticeMenusを指すようにしたい

    fun findMenuById(menuId: String?): PracticeMenuItemDisplay? {
        return menus.find { it.id == menuId }
    }

    fun addCommentToMenu(menuId: String?, comment: Comment) {
        findMenuById(menuId)?.comments?.add(comment)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCommentsScreen(
    navController: NavController,
    menuId: String?,
    // この画面からPracticeListScreenのpracticeMenusを直接編集するのは推奨されないが、
    // プロトタイプとして一時的にPracticeMenuRepository経由でアクセスする
) {
    // PracticeListScreenが保持している実際のメニューリストにアクセスする
    // 本来はViewModelでmenuIdに対応するメニューとそのコメントを取得・管理する
    val menu = remember(menuId) { PracticeMenuRepository.findMenuById(menuId) }
    var newCommentText by remember { mutableStateOf("") }

    // menuがnullの場合の処理（エラー表示や前の画面に戻るなど）
    if (menu == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("エラー") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("メニューが見つかりませんでした。")
            }
        }
        return
    }

    // menu.comments を監視してUIを再コンポーズする
    // ただし、PracticeMenuRepository.menus が mutableStateListOf であっても、
    // menu.comments (ただのMutableList) の変更は直接この画面の再コンポーズをトリガーしない可能性がある。
    // そのため、menuオブジェクト自体か、menu.comments.sizeなどをLaunchedEffectのキーにするなどの工夫が必要になる場合がある。
    // 今回はPracticeMenuRepository.menusがPracticeListScreenのrememberされたStateListを指す前提で進める。

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${menu.title} - コメント") },
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
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) { // 入力欄に影をつけて区別しやすくする
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
                                // 仮の投稿者名。実際にはログインユーザー情報を使う
                                val comment = Comment(authorName = "現在のユーザー", text = newCommentText)
                                PracticeMenuRepository.addCommentToMenu(menuId, comment)
                                newCommentText = "" // 入力欄をクリア
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
                .padding(paddingValues) // Scaffoldからのpaddingを適用
                .padding(horizontal = 16.dp), // さらに左右のpaddingを追加
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp) // リスト自体の上下padding
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
                items(menu.comments, key = { it.id }) { comment ->
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
