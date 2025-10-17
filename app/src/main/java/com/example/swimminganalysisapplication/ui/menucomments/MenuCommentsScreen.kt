package com.example.swimminganalysisapplication.ui.menucomments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send // ★★★ 推奨されているバージョンに修正 ★★★
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Chat
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCommentsScreen(
    navController: NavController,
    viewModel: MenuCommentsViewModel
) {
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var newCommentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("コメント") },
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
                        label = { Text("新しいスレッドを開始...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                viewModel.postComment(newCommentText, null)
                                newCommentText = ""
                            }
                        },
                        enabled = newCommentText.isNotBlank() && !isLoading
                    ) {
                        // ★★★ 推奨されているバージョンに修正 ★★★
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "スレッド開始")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && threads.isEmpty()) { // 初回ロード中のみ全画面インジケーター
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = "エラー: $error",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (threads.isEmpty()) {
                Text(
                    "まだコメントはありません。",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    reverseLayout = true // 新しいコメントが下に来るように
                ) {
                    items(threads) { thread ->
                        ThreadItem(
                            parent = thread.parent,
                            replies = thread.replies,
                            isPosting = isLoading,
                            onReply = { parentChatId, text ->
                                viewModel.postComment(text, parentChatId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadItem(
    parent: Chat,
    replies: List<Chat>,
    isPosting: Boolean,
    onReply: (Int, String) -> Unit
) {
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 親コメント
            CommentItem(comment = parent)

            // 返信
            if (replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp)
                ) {
                    replies.forEach { reply ->
                        CommentItem(comment = reply)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // 返信ボタン
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { showReplyInput = !showReplyInput }) {
                Text(if (showReplyInput) "キャンセル" else "返信する")
            }

            // 返信入力欄
            if (showReplyInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("返信を追加...") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReply(parent.chatId, replyText)
                                replyText = ""
                                showReplyInput = false
                            }
                        },
                        enabled = replyText.isNotBlank() && !isPosting
                    ) {
                         // ★★★ 推奨されているバージョンに修正 ★★★
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "返信を送信")
                    }
                }
            }
        }
    }
}


@Composable
fun CommentItem(comment: Chat) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO: ViewModelでユーザー名を取得する実装
            Text("User ${comment.userId}", style = MaterialTheme.typography.titleSmall)
            Text(
                text = comment.chatSentAt.toFormattedDateString(),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(comment.chatContent, style = MaterialTheme.typography.bodyMedium)
    }
}

// 日付文字列をパースしてフォーマットするための拡張関数
fun String.toFormattedDateString(): String {
    // 複数の日付フォーマットを試す
    val possibleFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    possibleFormats.forEach { format ->
        try {
            val parser = SimpleDateFormat(format, Locale.getDefault())
            val date = parser.parse(this)
            date?.let {
                val displayFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                return displayFormat.format(it)
            }
        } catch (e: Exception) {
            // パース失敗時は次のフォーマットを試す
        }
    }
    // どのフォーマットにも一致しなかった場合は元の文字列を（少し短くして）返す
    return this.take(16)
}
