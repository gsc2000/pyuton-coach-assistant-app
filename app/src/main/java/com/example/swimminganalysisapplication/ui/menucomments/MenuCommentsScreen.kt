package com.example.swimminganalysisapplication.ui.menucomments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
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
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCommentsScreen(
    navController: NavController,
    viewModel: MenuCommentsViewModel = viewModel()
) {
    val threads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var newCommentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("コメント") }, // ViewModelからメニュータイトルを取得できればより良い
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
                                viewModel.postNewThread(newCommentText)
                                newCommentText = ""
                            }
                        },
                        enabled = newCommentText.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "スレッド開始")
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
            if (isLoading) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(threads) { thread ->
                        ThreadItem(
                            parent = thread.parent,
                            replies = thread.replies,
                            onReply = { parentChatId, text ->
                                viewModel.postReply(parentChatId, text)
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
                                onReply(parent.chatThreadId ?: 0, replyText)
                                replyText = ""
                                showReplyInput = false
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "返信を送信")
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
            Text(comment.userId.toString(), style = MaterialTheme.typography.titleSmall) // Temporarily display user ID
            Text(
                text = try {
                    // Parse ISO 8601 format string
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                    val date = isoFormat.parse(comment.chatSentAt)
                    // Convert to desired format
                    val displayFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    date?.let { d -> displayFormat.format(d) } ?: "Unknown Date"
                } catch (e: Exception) {
                    // Fallback for parsing error or if chatSentAt is not in the expected format
                    comment.chatSentAt
                },
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(comment.chatContent, style = MaterialTheme.typography.bodyMedium)
    }
}
