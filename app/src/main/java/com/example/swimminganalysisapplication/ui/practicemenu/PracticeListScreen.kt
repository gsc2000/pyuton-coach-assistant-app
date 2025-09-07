package com.example.swimminganalysisapplication.ui.practicemenu

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import java.util.UUID

// Comment, PracticeMenuItemDisplay, PracticeMenuRepository の定義は前回のコードと同じなので省略
// ... (Data classes and PracticeMenuRepository object definition from your previous code) ...
// (Ensure these are at the top level of this file or correctly imported if in separate files)
// Comment data class (このファイル内または別ファイルで定義)
data class Comment(
    val id: String = UUID.randomUUID().toString(),
    val authorName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

// PracticeMenuItemDisplay data class (このファイル内または別ファイルで定義)
data class PracticeMenuItemDisplay(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    var isPublic: Boolean = false,
    var tags: List<String> = emptyList(),
    var authorName: String = "自分",
    var isForked: Boolean = false,
    var forkedFromMenuId: String? = null,
    var forkedFromAuthorName: String? = null,
    var comments: MutableList<Comment> = mutableStateListOf()
)

// PracticeMenuRepository object (このファイル内または別ファイルで定義し、正しくインポート)
object PracticeMenuRepository {
    var menus: MutableList<PracticeMenuItemDisplay> = mutableStateListOf()
    var favoritedMenuOriginalIds: MutableList<String> = mutableStateListOf()

    fun findMenuById(menuId: String?): PracticeMenuItemDisplay? {
        return menus.find { it.id == menuId }
    }
    fun addCommentToMenu(menuId: String?, comment: Comment) {
        findMenuById(menuId)?.comments?.add(comment)
    }
    fun isFavorite(originalMenuId: String): Boolean {
        return favoritedMenuOriginalIds.contains(originalMenuId)
    }
    fun toggleFavorite(originalMenuId: String) {
        if (isFavorite(originalMenuId)) {
            favoritedMenuOriginalIds.remove(originalMenuId)
            Log.d("Favorites", "Removed $originalMenuId from favorites")
        } else {
            favoritedMenuOriginalIds.add(originalMenuId)
            Log.d("Favorites", "Added $originalMenuId to favorites")
        }
    }
}


private const val TAG = "PracticeListScreen" // TAG定義

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeListScreen(navController: NavController) {
    var practiceMenus by remember {
        mutableStateOf(listOf(
            PracticeMenuItemDisplay(title = "メニュー1: バタフライ 100m x 5", authorName = "自分"),
            PracticeMenuItemDisplay(title = "メニュー2: クロール 200m x 3", authorName = "自分", isPublic = true),
            PracticeMenuItemDisplay(title = "フォーク済みサンプル", authorName = "自分", isForked = true, forkedFromAuthorName = "すごいコーチ")
        ))
    }
    LaunchedEffect(Unit) { // 初期データを一度だけリポジトリにセット
        if (PracticeMenuRepository.menus.isEmpty()) { // リポジトリが空の場合のみ初期化
            PracticeMenuRepository.menus.addAll(practiceMenus)
            Log.d(TAG, "Initial practice menus set in repository from PracticeListScreen. Count: ${PracticeMenuRepository.menus.size}")
        }
    }
    // practiceMenus の変更をリポジトリに同期する LaunchedEffect
    LaunchedEffect(practiceMenus) {
        // practiceMenus が変更されたら、リポジトリの内容を完全に置き換えるか、
        // 差分更新するかは設計による。ここでは単純に全置き換え。
        PracticeMenuRepository.menus.clear()
        PracticeMenuRepository.menus.addAll(practiceMenus)
        Log.d(TAG, "PracticeMenuRepository.menus updated. New count: ${PracticeMenuRepository.menus.size}")
    }


    val currentBackStackEntry = navController.currentBackStackEntry
    DisposableEffect(currentBackStackEntry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d(TAG, "PracticeListScreen ON_RESUME detected.")
                val savedStateHandle = currentBackStackEntry?.savedStateHandle
                Log.d(TAG, "Current SavedStateHandle on resume: $savedStateHandle. Contains 'newMenuForked': ${savedStateHandle?.contains("newMenuForked")}")

                if (savedStateHandle != null) {
                    Log.d(TAG, "Keys currently in SavedStateHandle: ${savedStateHandle.keys()}")
                    // Forked menu logic
                    if (savedStateHandle.get<Boolean>("newMenuForked") == true) { // null許容でないgetを使用
                        Log.d(TAG, "'newMenuForked' is true. Processing fork.")
                        val title = savedStateHandle.get<String>("forkedMenuTitle") ?: "フォークされたメニュー"
                        val description = savedStateHandle.get<String>("forkedMenuDescription")
                        val originalAuthor = savedStateHandle.get<String>("forkedMenuOriginalAuthor") ?: "不明な作成者"
                        val originalId = savedStateHandle.get<String>("forkedMenuOriginalId")
                        Log.d(TAG, "Forked data from SavedStateHandle: Title='$title', Desc='$description', Author='$originalAuthor', OriginalID='$originalId'")

                        val newForkedMenu = PracticeMenuItemDisplay(
                            title = title,
                            description = description,
                            isPublic = false,
                            authorName = "自分",
                            isForked = true,
                            forkedFromMenuId = originalId,
                            forkedFromAuthorName = originalAuthor
                        )

                        if (practiceMenus.none { it.forkedFromMenuId == newForkedMenu.forkedFromMenuId && newForkedMenu.forkedFromMenuId != null }) {
                            practiceMenus = practiceMenus + newForkedMenu
                            Log.d(TAG, "New forked menu added to practiceMenus. New practiceMenus count: ${practiceMenus.size}")
                        } else {
                            Log.d(TAG, "Menu with originalId '$originalId' seems to be already forked or duplicate fork attempt.")
                        }

                        // Clean up SavedStateHandle
                        savedStateHandle.remove<Boolean>("newMenuForked")
                        savedStateHandle.remove<String>("forkedMenuTitle")
                        savedStateHandle.remove<String>("forkedMenuDescription")
                        savedStateHandle.remove<String>("forkedMenuOriginalAuthor")
                        savedStateHandle.remove<String>("forkedMenuOriginalId")
                        Log.d(TAG, "Cleaned up SavedStateHandle for forked menu. Remaining keys: ${savedStateHandle.keys()}")
                    } else {
                        Log.d(TAG, "'newMenuForked' key not found or its value is not true. Contained keys: ${savedStateHandle.keys().filter { it.startsWith("forked") || it == "newMenuForked" }}")
                    }

                    // Newly created menu logic (if any, as per your original code)
                    if (savedStateHandle.get<Boolean>("newPracticeMenuAdded") == true) {
                        // ... your logic for newly created menus ...
                        Log.d(TAG, "Processing newPracticeMenuAdded.")
                        savedStateHandle.remove<Boolean>("newPracticeMenuAdded")
                        // ... remove other related keys ...
                    }

                } else {
                    Log.d(TAG, "SavedStateHandle is null on resume.")
                }
            }
        }
        currentBackStackEntry?.lifecycle?.addObserver(observer)
        Log.d(TAG, "Lifecycle observer added to NavBackStackEntry's lifecycle.")
        onDispose {
            currentBackStackEntry?.lifecycle?.removeObserver(observer)
            Log.d(TAG, "Lifecycle observer removed from NavBackStackEntry's lifecycle.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("練習メニュー 一覧") },
                navigationIcon = {
                    if (navController.previousBackStackEntry != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(AppDestinations.FAVORITES_SCREEN_ROUTE) }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "お気に入り一覧")
                    }
                    IconButton(onClick = { navController.navigate(AppDestinations.DISCOVER_SCREEN_ROUTE) }) {
                        Icon(Icons.Filled.Search, contentDescription = "公開メニューを探す")
                    }
                    AccountActionsMenu(navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                                val routeWithArgument = AppDestinations.MENU_COMMENTS_WITH_ARG_ROUTE.replace("{menuId}", menu.id)
                                Log.d(TAG, "Navigating to comments for menuId: ${menu.id} with route: $routeWithArgument")
                                navController.navigate(routeWithArgument)
                            }
                        )
                        HorizontalDivider()
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
                .clickable(onClick = onEditClick)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                item.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                if (item.authorName == "自分") {
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
                                leadingIcon = { Switch(checked = item.isPublic, onCheckedChange = { onPublishStatusChange(it) }) }
                            )
                            if (item.isPublic) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("タグ (公開時のみ編集可)", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall)
                                if (item.tags.isNotEmpty()) {
                                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        item.tags.forEach { tag ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(tag, modifier = Modifier.weight(1f))
                                                IconButton(onClick = { onTagRemoved(tag) }) {
                                                    Icon(Icons.Filled.Close, contentDescription = "タグ「$tag」を削除")
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text("タグはまだありません", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newTagText,
                                        onValueChange = { newTagText = it },
                                        label = { Text("新しいタグ") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(
                                        onClick = {
                                            if (newTagText.isNotBlank()) {
                                                onTagAdded(newTagText)
                                                newTagText = ""
                                            }
                                        },
                                        enabled = newTagText.isNotBlank()
                                    ) {
                                        Text("追加")
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
