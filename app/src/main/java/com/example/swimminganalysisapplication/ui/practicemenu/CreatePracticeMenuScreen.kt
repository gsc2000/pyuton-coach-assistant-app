package com.example.swimminganalysisapplication.ui.practicemenu

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// --- PracticeMenuItemRow Composable (変更なし) ---
@Composable
fun PracticeMenuItemRow(
    item: PracticeMenuItem,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.drillName,
                style = MaterialTheme.typography.titleMedium
            )
            val details = mutableListOf<String>()
            if (item.distance.isNotBlank()) details.add(item.distance)
            if (item.repetitions.isNotBlank()) details.add(item.repetitions)
            if (item.rest.isNotBlank()) details.add("Rest/Cycle: ${item.rest}")
            if (item.notes.isNotBlank()) details.add("Note: ${item.notes.take(30)}${if (item.notes.length > 30) "..." else ""}")

            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" / "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.error)
        }
    }
    Divider()
}

// --- Data classes (変更なし) ---
data class PracticeMenuItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var drillName: String = "",
    var distance: String = "",
    var repetitions: String = "",
    var rest: String = "",
    var notes: String = ""
)

const val CREATE_PRACTICE_MENU_TAG_DRAG_OFFSET_FIX = "CreatePracticeMenuDragOffsetFix"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePracticeMenuScreen(
    navController: NavController,
    menuId: String?, // ★★★ 引数名を `practiceMenuId` から `menuId` に修正 ★★★
    viewModel: CreatePracticeMenuViewModel
) {
    var menuTitle by rememberSaveable { mutableStateOf("") }
    var menuDescription by rememberSaveable { mutableStateOf("") }
    val practiceMenuItems = remember { mutableStateListOf<PracticeMenuItem>() }

    val isEditing = menuId != null // ★★★ `practiceMenuId` を `menuId` に修正 ★★★
    val screenTitle = if (isEditing) "練習メニュー編集" else "練習メニュー作成"

    val uiState by viewModel.uiState.collectAsState()
    val practiceMenu by viewModel.practiceMenu.collectAsState()
    val context = LocalContext.current

    // ★★★ `practiceMenuId` を `menuId` に修正 ★★★
    LaunchedEffect(key1 = menuId) {
        if (menuId != null) {
            menuId.toIntOrNull()?.let {
                viewModel.loadMenu(it)
            }
        }
    }

    LaunchedEffect(key1 = practiceMenu) {
        practiceMenu?.let { menu ->
            menuTitle = menu.menuTitle ?: ""
            val (desc, items) = viewModel.getItemsFromJson(menu.menuDescription)
            menuDescription = desc
            practiceMenuItems.clear()
            practiceMenuItems.addAll(items)
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success -> {
                // NOTE: This can be triggered on both load and save. Consider a more specific state for save success.
                // Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show()
                // navController.popBackStack()
            }
            is UiState.Error -> {
                Toast.makeText(context, "エラー: ${state.message}", Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var tempDrillName by remember { mutableStateOf("") }
    var tempDistance by remember { mutableStateOf("") }
    var tempRepetitions by remember { mutableStateOf("") }
    var tempRest by remember { mutableStateOf("") }
    var tempNotes by remember { mutableStateOf("") }

    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var draggedItemHeightPx by remember { mutableStateOf(0) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }

    val density = LocalDensity.current
    val itemHeights = remember { mutableStateMapOf<String, Int>() }
    val defaultItemHeightPx = with(density) { 75.dp.roundToPx() }

    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }

    if (showAddItemDialog) {
        PracticeItemInputDialog(
            drillName = tempDrillName, onDrillNameChange = { tempDrillName = it },
            distance = tempDistance, onDistanceChange = { tempDistance = it },
            repetitions = tempRepetitions, onRepetitionsChange = { tempRepetitions = it },
            rest = tempRest, onRestChange = { tempRest = it },
            notes = tempNotes, onNotesChange = { tempNotes = it },
            onDismissRequest = { showAddItemDialog = false },
            onConfirm = {
                if (tempDrillName.isNotBlank()) {
                    practiceMenuItems.add(PracticeMenuItem(drillName = tempDrillName, distance = tempDistance, repetitions = tempRepetitions, rest = tempRest, notes = tempNotes))
                    tempDrillName = ""; tempDistance = ""; tempRepetitions = ""; tempRest = ""; tempNotes = ""
                    showAddItemDialog = false
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
                actions = {
                    Button(
                        onClick = {
                            viewModel.saveMenu(
                                title = menuTitle,
                                description = menuDescription,
                                items = practiceMenuItems.toList(),
                                isPublic = false, // TODO: Add UI for this
                                tags = emptyList(), // TODO: Add UI for this
                                // ★★★ `practiceMenuId` を `menuId` に修正 ★★★
                                existingMenuId = menuId?.toIntOrNull()
                            )
                        },
                        enabled = uiState != UiState.Loading
                    ) {
                        if (uiState == UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("保存")
                        }
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
            FloatingActionButton(onClick = {
                tempDrillName = ""; tempDistance = ""; tempRepetitions = ""; tempRest = ""; tempNotes = ""
                showAddItemDialog = true
            }) { Icon(Icons.Filled.Add, "メニュー項目を追加") }
        }
    ) { paddingValuesFromScaffold ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesFromScaffold)
                .padding(horizontal = 16.dp)
                .padding(bottom = 72.dp)
        ) {
            OutlinedTextField(value = menuTitle,onValueChange = { menuTitle = it },label = { Text("メニュータイトル") },modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = menuDescription,onValueChange = { menuDescription = it },label = { Text("メニュー説明 (任意)") },modifier = Modifier.fillMaxWidth(),minLines = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Text("メニュー項目 (${practiceMenuItems.size})",style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (practiceMenuItems.isEmpty() && draggedItemIndex == null) {
                    item {
                        Text(
                            "まだメニュー項目がありません。「+」ボタンで追加してください。",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    itemsIndexed(practiceMenuItems, key = { _, item -> item.id }) { index, menuItem ->
                        val isBeingDragged = draggedItemIndex == index

                        if (dropTargetIndex == index && !isBeingDragged && draggedItemIndex != null) {
                            DropIndicator()
                        }

                        Box(
                            modifier = Modifier
                                .onSizeChanged { size -> itemHeights[menuItem.id] = size.height }
                                .graphicsLayer {
                                    translationY = if (isBeingDragged) dragOffsetY else 0f
                                    shadowElevation = if (isBeingDragged) 8.dp.toPx() else 0f
                                    alpha = if (isBeingDragged && practiceMenuItems.size > 1) 0.9f else 1.0f
                                }
                                .zIndex(if (isBeingDragged) 1f else 0f)
                                .pointerInput(menuItem.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            if (draggedItemIndex == null && practiceMenuItems.size > 1) {
                                                val heightFromMap = itemHeights[menuItem.id]
                                                draggedItemHeightPx = heightFromMap ?: defaultItemHeightPx
                                                draggedItemIndex = index
                                                dragOffsetY = 0f
                                                Log.d(CREATE_PRACTICE_MENU_TAG_DRAG_OFFSET_FIX, "onDragStart: User touched item ID ${menuItem.id} at visual index $index. Height from map: $heightFromMap, Fallback used: ${heightFromMap == null}. Set draggedItemHeightPx: $draggedItemHeightPx")
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            if (draggedItemIndex == index) {
                                                change.consume()
                                                dragOffsetY += dragAmount.y

                                                var currentItemOriginalTopY = 0f
                                                for(i in 0 until (draggedItemIndex ?: 0) ) {
                                                    currentItemOriginalTopY += (itemHeights[practiceMenuItems[i].id] ?: defaultItemHeightPx)
                                                }

                                                val draggedItemCurrentCenterY = currentItemOriginalTopY + dragOffsetY + (draggedItemHeightPx / 2f)
                                                var newDropTargetIndex = practiceMenuItems.size
                                                var scanY = 0f
                                                for (i in 0 until practiceMenuItems.size) {
                                                    val currentItemSlotHeight = itemHeights[practiceMenuItems[i].id] ?: defaultItemHeightPx
                                                    val slotCenterY = scanY + currentItemSlotHeight / 2f
                                                    if (draggedItemCurrentCenterY < slotCenterY) {
                                                        newDropTargetIndex = i
                                                        break
                                                    }
                                                    scanY += currentItemSlotHeight
                                                }
                                                dropTargetIndex = newDropTargetIndex

                                                val scrollThresholdDp = 56.dp
                                                val scrollSpeedPx = 10f
                                                val scrollThresholdPx = with(density) { scrollThresholdDp.toPx() }

                                                var draggedItemActualTopYInList = 0f
                                                for(i in 0 until (draggedItemIndex ?: 0)) {
                                                    draggedItemActualTopYInList += (itemHeights[practiceMenuItems[i].id] ?: defaultItemHeightPx)
                                                }

                                                val draggedItemVisualTopInViewport = (draggedItemActualTopYInList + dragOffsetY) - lazyListState.firstVisibleItemScrollOffset
                                                val draggedItemVisualBottomInViewport = draggedItemVisualTopInViewport + draggedItemHeightPx
                                                val viewportHeight = lazyListState.layoutInfo.viewportSize.height

                                                var performScroll: Float? = null
                                                if (viewportHeight > 0) {
                                                    if (draggedItemVisualTopInViewport < scrollThresholdPx) {
                                                        performScroll = -scrollSpeedPx
                                                    } else if (draggedItemVisualBottomInViewport > viewportHeight - scrollThresholdPx) {
                                                        performScroll = scrollSpeedPx
                                                    }
                                                }

                                                if (performScroll != null) {
                                                    if (autoScrollJob == null || !autoScrollJob!!.isActive) {
                                                        autoScrollJob = scope.launch {
                                                            while (isActive) {
                                                                lazyListState.scrollBy(performScroll)
                                                                delay(16)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    autoScrollJob?.cancel()
                                                    autoScrollJob = null
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            autoScrollJob?.cancel()
                                            autoScrollJob = null
                                            draggedItemIndex?.let { startIndex ->
                                                val finalTargetIndex = dropTargetIndex ?: startIndex
                                                if (startIndex != finalTargetIndex && finalTargetIndex <= practiceMenuItems.size) {
                                                    val itemToMove = practiceMenuItems.removeAt(startIndex)
                                                    val actualInsertIndex = if (finalTargetIndex > startIndex && finalTargetIndex > 0) {
                                                        (finalTargetIndex -1).coerceIn(0, practiceMenuItems.size)
                                                    } else {
                                                        finalTargetIndex.coerceIn(0, practiceMenuItems.size)
                                                    }
                                                    practiceMenuItems.add(actualInsertIndex, itemToMove)
                                                }
                                            }
                                            draggedItemIndex = null
                                            dragOffsetY = 0f
                                            dropTargetIndex = null
                                        },
                                        onDragCancel = {
                                            autoScrollJob?.cancel()
                                            autoScrollJob = null
                                            draggedItemIndex = null
                                            dragOffsetY = 0f
                                            dropTargetIndex = null
                                        }
                                    )
                                }
                        ) {
                            PracticeMenuItemRow(
                                item = menuItem,
                                onDelete = {
                                    if (draggedItemIndex != index) {
                                        itemHeights.remove(practiceMenuItems[index].id)
                                        practiceMenuItems.removeAt(index)
                                        if (dropTargetIndex != null && dropTargetIndex!! >= practiceMenuItems.size) {
                                            dropTargetIndex = null
                                        }
                                    }
                                }
                            )
                        }

                        if (index == practiceMenuItems.lastIndex && dropTargetIndex == practiceMenuItems.size && draggedItemIndex != null && !isBeingDragged) {
                            DropIndicator()
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DropIndicator() {
    Divider(
        color = MaterialTheme.colorScheme.primary,
        thickness = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
    )
}


@Composable
fun PracticeItemInputDialog(
    drillName: String, onDrillNameChange: (String) -> Unit,
    distance: String, onDistanceChange: (String) -> Unit,
    repetitions: String, onRepetitionsChange: (String) -> Unit,
    rest: String, onRestChange: (String) -> Unit,
    notes: String, onNotesChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("メニュー項目を追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = drillName,
                    onValueChange = onDrillNameChange,
                    label = { Text("種目/ドリル名 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = distance,
                        onValueChange = onDistanceChange,
                        label = { Text("距離") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = repetitions,
                        onValueChange = onRepetitionsChange,
                        label = { Text("本数/セット") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = rest,
                    onValueChange = onRestChange,
                    label = { Text("休憩/サイクル") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("メモ (任意)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("完了") } },
        dismissButton = { Button(onClick = onDismissRequest) { Text("キャンセル") } }
    )
}
