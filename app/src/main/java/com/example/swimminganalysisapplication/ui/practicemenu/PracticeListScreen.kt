package com.example.swimminganalysisapplication.ui.practicemenu

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import com.example.swimminganalysisapplication.ui.menucomments.PracticeMenuRepository // PracticeMenuRepositoryのインポートを確認
import java.util.UUID

private const val TAG = "PracticeListScreen"

data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PracticeMenuItemDisplay(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    var isPublic: Boolean = false,
    var tags: List<String> = emptyList(),
    var authorName: String = "自分", // このメニューの現在の所有者
    var isForked: Boolean = false,    // このメニューがフォークされたものか
    var forkedFromMenuId: String? = null, // フォーク元のメニューID
    var forkedFromAuthorName: String? = null, // フォーク元のオリジナル作成者名
    var comments: MutableList<Comment> = mutableStateListOf()
)

// PracticeMenuRepository object (コメント機能やフォーク機能での仮データ共有用)
// 本番ではViewModelや適切なデータ永続化層に置き換える
object PracticeMenuRepository {
    var menus: MutableList<PracticeMenuItemDisplay> = mutableStateListOf()

    fun findMenuById(menuId: String?): PracticeMenuItemDisplay? {
        return menus.find { it.id == menuId }
    }

    fun addCommentToMenu(menuId: String?, comment: Comment) {
        findMenuById(menuId)?.comments?.add(comment)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(navController: NavController) {
    var practiceMenus by remember {
        mutableStateOf(listOf(
            PracticeMenuItemDisplay(title = "メニュー1: バタフライ 100m x 5", description = "インターバル 2:00", isPublic = false, tags = listOf("バタフライ", "短距離"), authorName = "自分", isForked = false),
            PracticeMenuItemDisplay(title = "メニュー2: クロール 200m x 3", description = "レスト 30秒", isPublic = true, tags = listOf("クロール", "中距離", "持久力"), authorName = "自分", isForked = false),
            PracticeMenuItemDisplay(title = "フォーク済みサンプル: 背泳ぎドリル", description = "キック練習", isPublic = false, tags = listOf("ドリル"), authorName = "自分", isForked = true, forkedFromMenuId = "originalDrillId123", forkedFromAuthorName = "すごいコーチ")
        ))
    }

    // プロトタイプ用: Repositoryに現在のリストを同期
    LaunchedEffect(practiceMenus) {
        PracticeMenuRepository.menus = practiceMenus.toMutableList()
    }

    val currentBackStackEntry = navController.currentBackStackEntry
    DisposableEffect(currentBackStackEntry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val savedStateHandle = currentBackStackEntry?.savedStateHandle ?: return@LifecycleEventObserver

                // フォークされたメニューの処理
                if (savedStateHandle.contains("newMenuForked") && savedStateHandle.get<Boolean>("newMenuForked") == true) {
                    val title = savedStateHandle.get<String>("forkedMenuTitle") ?: "フォークされたメニュー"
                    val description = savedStateHandle.get<String>("forkedMenuDescription")
                    val originalAuthor = savedStateHandle.get<String>("forkedMenuOriginalAuthor") ?: "不明な作成者"
                    val originalId = savedStateHandle.get<String>("forkedMenuOriginalId")

                    Log.d(TAG, "Received forked menu data: $title, from $originalAuthor (original ID: $originalId)")

                    val newForkedMenu = PracticeMenuItemDisplay(
                        title = title,
                        description = description,
                        isPublic = false,
                        tags = emptyList(),
                        authorName = "自分",
                        isForked = true,
                        forkedFromMenuId = originalId,
                        forkedFromAuthorName = originalAuthor,
                        comments = mutableStateListOf()
                    )
                    if (practiceMenus.none { it.forkedFromMenuId == newForkedMenu.forkedFromMenuId && newForkedMenu.forkedFromMenuId != null }) {
                        practiceMenus = practiceMenus + newForkedMenu
                        Log.d(TAG, "Forked menu '${newForkedMenu.title}' added to list.")
                    } else if (newForkedMenu.forkedFromMenuId == null && practiceMenus.none {it.id == newForkedMenu.id}) {
                        practiceMenus = practiceMenus + newForkedMenu
                        Log.d(TAG, "Forked menu (no originalId or unique) '${newForkedMenu.title}' added to list.")
                    } else {
                        Log.d(TAG, "Forked menu '${newForkedMenu.title}' (Original ID: $originalId) already exists or ID conflict.")
                    }

                    savedStateHandle.remove<Boolean>("newMenuForked")
                    savedStateHandle.remove<String>("forkedMenuTitle")
                    savedStateHandle.remove<String>("forkedMenuDescription")
                    savedStateHandle.remove<String>("forkedMenuOriginalAuthor")
                    savedStateHandle.remove<String>("forkedMenuOriginalId")
                }

                // CreatePracticeMenuScreenからの通常の新規メニュー追加処理
                if (savedStateHandle.contains("newPracticeMenuAdded") == true && savedStateHandle.get<Boolean>("newPracticeMenuAdded") == true) {
                    val newTitle = savedStateHandle.get<String>("newMenuTitle") ?: "新しいメニュー (タイトルなし)"
                    val newDesc = savedStateHandle.get<String>("newMenuDescription")

                    Log.d(TAG, "New practice menu was added (not forked): $newTitle")
                    val newMenuItem = PracticeMenuItemDisplay(
                        title = newTitle,
                        description = newDesc,
                        isPublic = false,
                        tags = emptyList(),
                        authorName = "自分",
                        isForked = false
                    )
                    if (practiceMenus.none { it.id == newMenuItem.id }) {
                        practiceMenus = practiceMenus + newMenuItem
                        Log.d(TAG, "Newly created menu '${newMenuItem.title}' added to list.")
                    }

                    savedStateHandle.remove<Boolean>("newPracticeMenuAdded")
                    savedStateHandle.remove<String>("newMenuTitle")
                    savedStateHandle.remove<String>("newMenuDescription")
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
                actions = {
                    TextButton(onClick = { navController.navigate(AppDestinations.DISCOVER_SCREEN_ROUTE) }) {
                        Text("探す")
                    }
                    AccountActionsMenu(navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors( // AppBarの色設定
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    navController.navigate(AppDestinations.CREATE_PRACTICE_MENU_ROUTE_BASE)
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
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (practiceMenus.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("練習メニューはまだありません。")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(practiceMenus, key = { _, item -> item.id }) { index, menu ->
                        MenuListItem(
                            item = menu,
                            onEditClick = {
                                navController.navigate("${AppDestinations.CREATE_PRACTICE_MENU_ROUTE_BASE}?menuId=${menu.id}")
                            },
                            onPublishStatusChange = { newStatus ->
                                val updatedMenus = practiceMenus.toMutableList()
                                updatedMenus[index] = menu.copy(isPublic = newStatus)
                                if (!newStatus) {
                                    updatedMenus[index] = updatedMenus[index].copy(tags = emptyList())
                                }
                                practiceMenus = updatedMenus
                            },
                            onTagAdded = { newTag ->
                                if (newTag.isNotBlank() && !menu.tags.contains(newTag)) {
                                    val updatedMenus = practiceMenus.toMutableList()
                                    updatedMenus[index] = menu.copy(tags = menu.tags + newTag)
                                    practiceMenus = updatedMenus
                                }
                            },
                            onTagRemoved = { tagToRemove ->
                                val updatedMenus = practiceMenus.toMutableList()
                                updatedMenus[index] = menu.copy(tags = menu.tags - tagToRemove)
                                practiceMenus = updatedMenus
                            },
                            onCommentClick = {
                                navController.navigate("${AppDestinations.MENU_COMMENTS_ROUTE}/${menu.id}")
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuListItem(
    item: PracticeMenuItemDisplay,
    onEditClick: () -> Unit,
    onPublishStatusChange: (Boolean) -> Unit,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit,
    onCommentClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = {
                    // フォークされたメニューも自分が所有者なので編集可能とする
                    // もしフォーク元へのリンクや読み取り専用ビューにしたい場合はここの挙動を変える
                    onEditClick()
                })
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                if (item.description != null) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.isPublic) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("公開中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        if (item.tags.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            Text("タグ: ${item.tags.take(2).joinToString(", ")}${if (item.tags.size > 2) "..." else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                // フォーク情報と作成者情報の表示
                if (item.isForked && item.forkedFromAuthorName != null) {
                    Text(
                        "フォーク元: ${item.forkedFromAuthorName} (所有者: ${item.authorName})",
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "作成者: ${item.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCommentClick) {
                    Text("コメント (${item.comments.size})")
                }
                // 自分のメニュー（フォークしたものも含む）であれば編集とオプション表示
                if (item.authorName == "自分") { // 将来的には実際のユーザーIDで比較
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "編集", tint = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "その他オプション")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (item.isPublic) "非公開にする" else "公開する") },
                                onClick = {
                                    onPublishStatusChange(!item.isPublic)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Switch(
                                        checked = item.isPublic,
                                        onCheckedChange = {
                                            onPublishStatusChange(it)
                                            // ここでもshowMenu = falseしても良い
                                        }
                                    )
                                }
                            )
                            if (item.isPublic) {
                                if (item.tags.isNotEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        Text("現在のタグ:", style = MaterialTheme.typography.labelMedium)
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            item.tags.forEach { tag ->
                                                InputChip(
                                                    selected = false,
                                                    onClick = { /* No action */ },
                                                    label = { Text(tag) },
                                                    trailingIcon = {
                                                        IconButton(onClick = { onTagRemoved(tag) }, modifier = Modifier.size(18.dp)) {
                                                            Icon(Icons.Filled.Close, contentDescription = "タグ「$tag」を削除")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    OutlinedTextField(
                                        value = newTagText,
                                        onValueChange = { newTagText = it },
                                        label = { Text("新しいタグ") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            if (newTagText.isNotBlank()) {
                                                onTagAdded(newTagText)
                                                newTagText = ""
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.End),
                                        enabled = newTagText.isNotBlank()
                                    ) {
                                        Text("タグ追加")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

