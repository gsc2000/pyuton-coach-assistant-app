package com.example.swimminganalysisapplication.ui.menucomments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Chat
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuCommentsScreen(
    navController: NavController,
    viewModel: MenuCommentsViewModel
) {
    val commentThreads by viewModel.commentThreads.collectAsState()
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
            if (isLoading && commentThreads.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (error != null) {
                Text(
                    text = "エラー: $error",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (commentThreads.isEmpty()) {
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
                    items(commentThreads, key = { it.parent.chatId }) { thread ->
                        ThreadCard(
                            thread = thread,
                            isPosting = isLoading,
                            onExpandToggle = { viewModel.toggleThreadExpansion(thread.parent.chatId) },
                            onReply = { parentId, text -> viewModel.postComment(text, parentId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadCard(
    thread: CommentThread,
    isPosting: Boolean,
    onExpandToggle: () -> Unit,
    onReply: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            ParentComment(
                thread = thread,
                onExpandToggle = onExpandToggle
            )

            if (thread.isExpanded) {
                ReplyInput(
                    isPosting = isPosting,
                    onReply = { text -> onReply(thread.parent.chatId, text) }
                )

                if (thread.replies.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }

                thread.replies.forEach { reply ->
                    ReplyComment(reply = reply)
                }
            }
        }
    }
}

@Composable
fun ParentComment(
    thread: CommentThread,
    onExpandToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onExpandToggle)
            .padding(16.dp)
    ) {
        CommentItem(comment = thread.parent)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = "Replies",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            val replyTextStr = when {
                thread.replies.isNotEmpty() -> "${thread.replies.size}件の返信"
                thread.isExpanded -> "返信する"
                else -> "返信を見る"
            }
            Text(
                text = replyTextStr,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ReplyInput(isPosting: Boolean, onReply: (String) -> Unit) {
    var replyText by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
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
                    onReply(replyText)
                    replyText = ""
                }
            },
            enabled = replyText.isNotBlank() && !isPosting
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "返信を送信")
        }
    }
}

@Composable
fun ReplyComment(reply: Chat) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SubdirectoryArrowRight,
                contentDescription = "Reply",
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CommentItem(comment = reply, modifier = Modifier.weight(1f))
        }
        Divider(modifier = Modifier.padding(start = 52.dp)) // Icon width + padding
    }
}

@Composable
fun CommentItem(comment: Chat, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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

fun String.toFormattedDateString(): String {
    val possibleFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    possibleFormats.forEach { format ->
        try {
            val parser = SimpleDateFormat(format, Locale.getDefault())
            parser.parse(this)?.let {
                val displayFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                return displayFormat.format(it)
            }
        } catch (e: Exception) {
            // Continue to next format
        }
    }
    return this.take(16)
}