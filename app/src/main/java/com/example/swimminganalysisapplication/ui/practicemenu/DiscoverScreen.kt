package com.example.swimminganalysisapplication.ui.practicemenu

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Menu
import com.example.swimminganalysisapplication.navigation.AppDestinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    navController: NavController,
    viewModel: DiscoverViewModel
) {
    val menus by viewModel.publicMenus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val forkResult by viewModel.forkResult.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(forkResult) {
        forkResult?.let { result ->
            result.onSuccess {
                Toast.makeText(context, "メニューを自分のリストにコピーしました", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "メニューのコピーに失敗しました: ${it.message}", Toast.LENGTH_LONG).show()
            }
            viewModel.consumeForkResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("メニューを探す") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
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
                Text(text = "公開されている練習メニューがありません。")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(menus) { menu ->
                        PublicMenuCard(
                            menu = menu,
                            onClick = {
                                val route = AppDestinations.CREATE_PRACTICE_MENU_ROUTE.replace("{menuId}", menu.menuId.toString())
                                navController.navigate(route)
                            },
                            onForkClick = {
                                viewModel.forkMenu(menu.menuId)
                            },
                            onCommentClick = {
                                val route = AppDestinations.MENU_COMMENTS_WITH_ARG_ROUTE.replace("{menuId}", menu.menuId.toString())
                                navController.navigate(route)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PublicMenuCard(
    menu: Menu,
    onClick: () -> Unit,
    onForkClick: () -> Unit,
    onCommentClick: () -> Unit
) {
    val displayDescription = menu.menuDescription?.split("---items---")?.getOrNull(0)?.trim() ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // ★★★ ここを修正: nullの場合の代替テキストを指定 ★★★
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

            IconButton(onClick = onCommentClick) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "コメントを見る",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onForkClick) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "このメニューを自分のリストにコピーする",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}