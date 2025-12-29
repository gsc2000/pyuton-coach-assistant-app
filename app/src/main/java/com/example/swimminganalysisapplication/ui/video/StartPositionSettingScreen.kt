package com.example.swimminganalysisapplication.ui.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu // ★ AccountActionsMenu をインポート
import com.example.swimminganalysisapplication.ui.theme.CustomTopAppBarHeight
import com.example.swimminganalysisapplication.ui.theme.getCustomTopAppBarColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import android.util.Log
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Data class for crop rectangle (normalized coordinates 0.0 to 1.0)
@Parcelize
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) : Parcelable

// Drag mode for crop overlay
private enum class DragMode {
    None, TopLeft, TopRight, BottomLeft, BottomRight,
    Left, Right, Top, Bottom, Move
}

// Helper function to format time (can be moved to a common utils file if used elsewhere)
private fun formatTime(millis: Long): String {
    if (millis == C.TIME_UNSET || millis < 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartPositionSettingScreen(
    navController: NavController,
    video1UriString: String?,
    video2UriString: String?,
    initialStart1Ms: Long,
    initialStart2Ms: Long,
    duration1Ms: Long,
    duration2Ms: Long
) {
    val context = LocalContext.current

    // SavedStateHandle を取得して状態を保持
    val savedStateHandle = remember { navController.previousBackStackEntry?.savedStateHandle }
    val currentSavedStateHandle = remember { navController.currentBackStackEntry?.savedStateHandle }
    
    // 位置合わせポイント（ビデオ上で表示される位置）- SavedStateHandle から復元
    var alignmentPoint1Ms by remember { 
        mutableStateOf(savedStateHandle?.get<Long>("alignmentPoint1Ms") ?: initialStart1Ms) 
    }
    var alignmentPoint2Ms by remember { 
        mutableStateOf(savedStateHandle?.get<Long>("alignmentPoint2Ms") ?: initialStart2Ms) 
    }

    // トリミング領域の状態 - SavedStateHandle から復元（currentとpreviousの両方を確認）
    var cropRect1 by remember {
        mutableStateOf(
            try {
                currentSavedStateHandle?.get<CropRect>("cropRect1") 
                    ?: savedStateHandle?.get<CropRect>("cropRect1") 
                    ?: CropRect()
            } catch (e: Exception) {
                Log.e("StartPositionSetting", "Failed to restore cropRect1", e)
                CropRect()
            }
        )
    }
    var cropRect2 by remember {
        mutableStateOf(
            try {
                currentSavedStateHandle?.get<CropRect>("cropRect2") 
                    ?: savedStateHandle?.get<CropRect>("cropRect2") 
                    ?: CropRect()
            } catch (e: Exception) {
                Log.e("StartPositionSetting", "Failed to restore cropRect2", e)
                CropRect()
            }
        )
    }

    // 共通のオフセット値（ミリ秒） - SavedStateHandle から復元（offset1Ms に格納）
    val offsetMsFromHandle = savedStateHandle?.get<Long>("offset1Ms") ?: 0L
    
    // 共通のオフセット設定（秒単位、少数第一位まで）
    var startOffsetSec by remember { 
        mutableStateOf(offsetMsFromHandle.toDouble() / 1000.0)
    }
    
    // オフセットの最大値を計算（両ビデオの位置合わせポイントの小さい方）
    val maxOffsetMs = kotlin.math.min(alignmentPoint1Ms, alignmentPoint2Ms)
    val maxOffsetSec = maxOffsetMs.toDouble() / 1000.0

    // ExoPlayer instances for preview (optional, but good for UX)
    // These players will only be used for seeking to show a frame, not for playback.
    val exoPlayer1Preview = remember {
        video1UriString?.let { ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(it)))
            prepare()
            playWhenReady = false
        }}
    }
    val exoPlayer2Preview = remember {
        video2UriString?.let { ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(it)))
            prepare()
            playWhenReady = false
        }}
    }
    
    // ビデオのアスペクト比を取得
    var videoAspectRatio1 by remember { mutableStateOf<Float?>(null) }
    var videoAspectRatio2 by remember { mutableStateOf<Float?>(null) }
    
    LaunchedEffect(exoPlayer1Preview) {
        exoPlayer1Preview?.let { player ->
            while (player.videoFormat == null) {
                kotlinx.coroutines.delay(100)
            }
            player.videoFormat?.let { format ->
                Log.d("StartPositionSetting", "Video1 format - width: ${format.width}, height: ${format.height}, rotationDegrees: ${format.rotationDegrees}, pixelWidthHeightRatio: ${format.pixelWidthHeightRatio}")
                // rotationDegreesが90または270度の場合、widthとheightを入れ替える
                val (effectiveWidth, effectiveHeight) = if (format.rotationDegrees == 90 || format.rotationDegrees == 270) {
                    Pair(format.height, format.width)
                } else {
                    Pair(format.width, format.height)
                }
                videoAspectRatio1 = (effectiveWidth.toFloat() * format.pixelWidthHeightRatio) / effectiveHeight.toFloat()
                Log.d("StartPositionSetting", "Video1 aspect ratio: $videoAspectRatio1 (effectiveWidth: $effectiveWidth, effectiveHeight: $effectiveHeight)")
            }
        }
    }
    
    LaunchedEffect(exoPlayer2Preview) {
        exoPlayer2Preview?.let { player ->
            while (player.videoFormat == null) {
                kotlinx.coroutines.delay(100)
            }
            player.videoFormat?.let { format ->
                Log.d("StartPositionSetting", "Video2 format - width: ${format.width}, height: ${format.height}, rotationDegrees: ${format.rotationDegrees}, pixelWidthHeightRatio: ${format.pixelWidthHeightRatio}")
                // rotationDegreesが90または270度の場合、widthとheightを入れ替える
                val (effectiveWidth, effectiveHeight) = if (format.rotationDegrees == 90 || format.rotationDegrees == 270) {
                    Pair(format.height, format.width)
                } else {
                    Pair(format.width, format.height)
                }
                videoAspectRatio2 = (effectiveWidth.toFloat() * format.pixelWidthHeightRatio) / effectiveHeight.toFloat()
                Log.d("StartPositionSetting", "Video2 aspect ratio: $videoAspectRatio2 (effectiveWidth: $effectiveWidth, effectiveHeight: $effectiveHeight)")
            }
        }
    }

    // Seek preview players when slider changes
    LaunchedEffect(alignmentPoint1Ms) {
        exoPlayer1Preview?.seekTo(alignmentPoint1Ms)
    }
    LaunchedEffect(alignmentPoint2Ms) {
        exoPlayer2Preview?.seekTo(alignmentPoint2Ms)
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer1Preview?.release()
            exoPlayer2Preview?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("開始位置設定") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    Button(onClick = {
                        // 状態を SavedStateHandle に保存
                        savedStateHandle?.set("alignmentPoint1Ms", alignmentPoint1Ms)
                        savedStateHandle?.set("alignmentPoint2Ms", alignmentPoint2Ms)
                        
                        // オフセットをミリ秒に変換（共通）
                        val offsetMs = (startOffsetSec * 1000.0).toLong()
                        
                        // 共通オフセットを保存（offset1Ms に格納）
                        savedStateHandle?.set("offset1Ms", offsetMs)
                        
                        // トリミング領域を保存
                        try {
                            savedStateHandle?.set("cropRect1", cropRect1)
                            savedStateHandle?.set("cropRect2", cropRect2)
                            navController.previousBackStackEntry?.savedStateHandle?.set("cropRect1", cropRect1)
                            navController.previousBackStackEntry?.savedStateHandle?.set("cropRect2", cropRect2)
                        } catch (e: Exception) {
                            Log.e("StartPositionSetting", "Failed to save cropRect", e)
                        }
                        
                        // 実際の開始位置 = 位置合わせポイント - オフセット
                        val actualStart1Ms = (alignmentPoint1Ms.toLong() - offsetMs).coerceAtLeast(0L)
                        val actualStart2Ms = (alignmentPoint2Ms.toLong() - offsetMs).coerceAtLeast(0L)
                        navController.previousBackStackEntry?.savedStateHandle?.set("newStart1Ms", actualStart1Ms)
                        navController.previousBackStackEntry?.savedStateHandle?.set("newStart2Ms", actualStart2Ms)
                        navController.popBackStack()
                    }) {
                        Text("完了")
                    }
                    AccountActionsMenu(navController = navController) // ★ AccountActionsMenu を追加
                },
                colors = getCustomTopAppBarColors(),
                modifier = Modifier.heightIn(max = CustomTopAppBarHeight)
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        var isScrollEnabled by remember { mutableStateOf(true) }
        
        LaunchedEffect(isScrollEnabled) {
            Log.d("StartPositionSetting", "Scroll enabled state changed: $isScrollEnabled")
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState, enabled = isScrollEnabled),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // 共通のオフセット設定（最上部）
            Text("開始位置オフセット", style = MaterialTheme.typography.titleMedium)
            OffsetInputField(
                offsetSec = startOffsetSec,
                onOffsetChange = { startOffsetSec = it },
                maxOffsetSec = maxOffsetSec
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            
            // Video 1 Settings
            if (video1UriString != null && duration1Ms > 0) {
                Text("ビデオ1 位置合わせ", style = MaterialTheme.typography.titleMedium)
                VideoPreviewAndSlider(
                    exoPlayer = exoPlayer1Preview,
                    alignmentPointMs = alignmentPoint1Ms,
                    onAlignmentPointChange = { alignmentPoint1Ms = it },
                    durationMs = duration1Ms,
                    cropRect = cropRect1,
                    onCropRectChange = { cropRect1 = it },
                    onDragStateChange = { isDragging -> isScrollEnabled = !isDragging },
                    videoAspectRatio = videoAspectRatio1
                )
            } else if (video1UriString != null) {
                Text("ビデオ1: 長さ情報なし", style = MaterialTheme.typography.titleMedium)
            }


            // Video 2 Settings
            if (video2UriString != null && duration2Ms > 0) {
                Text("ビデオ2 位置合わせ", style = MaterialTheme.typography.titleMedium)
                VideoPreviewAndSlider(
                    exoPlayer = exoPlayer2Preview,
                    alignmentPointMs = alignmentPoint2Ms,
                    onAlignmentPointChange = { alignmentPoint2Ms = it },
                    durationMs = duration2Ms,
                    cropRect = cropRect2,
                    onCropRectChange = { cropRect2 = it },
                    onDragStateChange = { isDragging -> isScrollEnabled = !isDragging },
                    videoAspectRatio = videoAspectRatio2
                )
            } else if (video2UriString != null) {
                Text("ビデオ2: 長さ情報なし", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VideoPreviewAndSlider(
    exoPlayer: ExoPlayer?,
    alignmentPointMs: Long,
    onAlignmentPointChange: (Long) -> Unit,
    durationMs: Long,
    cropRect: CropRect = CropRect(),
    onCropRectChange: (CropRect) -> Unit = {},
    onDragStateChange: (Boolean) -> Unit = {},
    videoAspectRatio: Float? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (exoPlayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer; useController = false } },
                    modifier = Modifier.fillMaxSize()
                )
                
                CropOverlay(
                    cropRect = cropRect,
                    onCropRectChange = onCropRectChange,
                    onDragStateChange = onDragStateChange,
                    videoAspectRatio = videoAspectRatio ?: (16f / 9f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("位置合わせ: ${formatTime(alignmentPointMs)} / 総時間: ${formatTime(durationMs)}")
        
        // 位置合わせポイントスライダー
        Slider(
            value = if (durationMs > 0) alignmentPointMs.toFloat() / durationMs.toFloat() else 0f,
            onValueChange = { newValue -> onAlignmentPointChange((newValue * durationMs).toLong()) },
            valueRange = 0f..(if (durationMs > 0) 1f else 0f),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { 
                val newPoint = (alignmentPointMs - 33L).coerceAtLeast(0L)
                onAlignmentPointChange(newPoint)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前フレーム")
            }
            IconButton(onClick = { 
                val newPoint = (alignmentPointMs + 33L).coerceAtMost(durationMs)
                onAlignmentPointChange(newPoint)
            }, modifier = Modifier.graphicsLayer(scaleX = -1f)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "次フレーム")
            }
        }
    }
}

@Composable
private fun OffsetInputField(
    offsetSec: Double,
    onOffsetChange: (Double) -> Unit,
    maxOffsetSec: Double
) {
    var inputText by remember { mutableStateOf(String.format("%.1f", offsetSec)) }
    
    LaunchedEffect(offsetSec) {
        inputText = String.format("%.1f", offsetSec)
    }
    
    fun updateOffset(newValue: Double) {
        val clamped = newValue.coerceIn(0.0, maxOffsetSec)
        onOffsetChange(clamped)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { updateOffset(offsetSec - 0.1) }) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "減少")
            }
            
            TextField(
                value = inputText,
                onValueChange = { newValue ->
                    inputText = newValue
                    val parsed = newValue.toDoubleOrNull()
                    if (parsed != null) {
                        // 小数第二位以下は切り捨て
                        val truncated = (parsed * 10).toLong() / 10.0
                        updateOffset(truncated)
                    }
                },
                label = { Text("秒") },
                modifier = Modifier
                    .width(100.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            
            IconButton(onClick = { updateOffset(offsetSec + 0.1) }) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "増加")
            }
        }
    }
}

@Composable
private fun CropOverlay(
    cropRect: CropRect,
    onCropRectChange: (CropRect) -> Unit,
    onDragStateChange: (Boolean) -> Unit = {},
    videoAspectRatio: Float = 16f / 9f,
    modifier: Modifier = Modifier
) {
    val handleSize = 24.dp
    val minSize = 0.1f // 最小サイズ 10%
    
    // アスペクト比をログ出力
    LaunchedEffect(videoAspectRatio) {
        Log.d("CropOverlay", "Using video aspect ratio: $videoAspectRatio")
    }
    
    // ドラッグ中のハンドルタイプを記憶
    var dragMode by remember { mutableStateOf<DragMode>(DragMode.None) }
    
    // 現在のcropRectを保持する状態
    var currentCropRect by remember { mutableStateOf(cropRect) }
    
    // cropRectが外部から変更されたら更新
    LaunchedEffect(cropRect) {
        currentCropRect = cropRect
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {  // Unitを使用してpointerInputを固定
                detectDragGestures(
                    onDragStart = { offset ->
                        Log.d("CropOverlay", "=== DRAG START ===")
                        onDragStateChange(true) // ドラッグ開始、スクロールを無効化
                        Log.d("CropOverlay", "Scroll disabled")
                        val width = size.width.toFloat()
                        val height = size.height.toFloat()
                        
                        // タッチ開始位置を正規化座標に変換
                        val touchX = offset.x / width
                        val touchY = offset.y / height
                        
                        Log.d("CropOverlay", "Touch at: (${offset.x}, ${offset.y}) px")
                        Log.d("CropOverlay", "Touch normalized: ($touchX, $touchY)")
                        Log.d("CropOverlay", "Size: ${width}x${height} px")
                        Log.d("CropOverlay", "CropRect: L=${currentCropRect.left}, T=${currentCropRect.top}, R=${currentCropRect.right}, B=${currentCropRect.bottom}")
                        
                        // ハンドル検出の閾値を大きくする
                        val threshold = 0.15f
                        
                        // 各ハンドルまでの距離を計算
                        val distToLeft = abs(touchX - currentCropRect.left)
                        val distToRight = abs(touchX - currentCropRect.right)
                        val distToTop = abs(touchY - currentCropRect.top)
                        val distToBottom = abs(touchY - currentCropRect.bottom)
                        
                        Log.d("CropOverlay", "Distances: L=$distToLeft, R=$distToRight, T=$distToTop, B=$distToBottom (threshold=$threshold)")
                        
                        // どのハンドルを掴んだかを判定（優先順位：コーナー > エッジ > 移動）
                        val isNearLeft = distToLeft < threshold
                        val isNearRight = distToRight < threshold
                        val isNearTop = distToTop < threshold
                        val isNearBottom = distToBottom < threshold
                        
                        Log.d("CropOverlay", "Near flags: L=$isNearLeft, R=$isNearRight, T=$isNearTop, B=$isNearBottom")
                        
                        dragMode = when {
                            isNearLeft && isNearTop -> DragMode.TopLeft
                            isNearRight && isNearTop -> DragMode.TopRight
                            isNearLeft && isNearBottom -> DragMode.BottomLeft
                            isNearRight && isNearBottom -> DragMode.BottomRight
                            // 辺のハンドルを削除し、四隅のみに
                            touchX >= currentCropRect.left && touchX <= currentCropRect.right && 
                            touchY >= currentCropRect.top && touchY <= currentCropRect.bottom -> DragMode.Move
                            else -> DragMode.None
                        }
                        
                        Log.d("CropOverlay", "DragMode selected: $dragMode")
                    },
                    onDragEnd = {
                        Log.d("CropOverlay", "=== DRAG END ===")
                        dragMode = DragMode.None
                        onDragStateChange(false) // ドラッグ終了、スクロールを有効化
                        Log.d("CropOverlay", "Scroll enabled, dragMode reset to None")
                    },
                    onDragCancel = {
                        Log.d("CropOverlay", "=== DRAG CANCELLED ===")
                        dragMode = DragMode.None
                        onDragStateChange(false) // ドラッグキャンセル、スクロールを有効化
                        Log.d("CropOverlay", "Scroll enabled, dragMode reset to None")
                    }
                ) { change, dragAmount ->
                    Log.d("CropOverlay", "--- Drag event: dragAmount=(${dragAmount.x}, ${dragAmount.y}) px, dragMode=$dragMode")
                    change.consume()
                    
                    if (dragMode == DragMode.None) {
                        Log.d("CropOverlay", "WARNING: Ignoring drag - mode is None")
                        return@detectDragGestures
                    }
                    
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    
                    // ドラッグ量を正規化座標に変換
                    val dx = dragAmount.x / width
                    val dy = dragAmount.y / height
                    
                    Log.d("CropOverlay", "Drag normalized: dx=$dx, dy=$dy")
                    
                    var newLeft = currentCropRect.left
                    var newTop = currentCropRect.top
                    var newRight = currentCropRect.right
                    var newBottom = currentCropRect.bottom
                    
                    Log.d("CropOverlay", "Before: L=$newLeft, T=$newTop, R=$newRight, B=$newBottom")
                    
                    when (dragMode) {
                        DragMode.TopLeft -> {
                            // 右下を固定点として、左上を動かす
                            val fixedX = currentCropRect.right
                            val fixedY = currentCropRect.bottom
                            
                            // ドラッグ量の絶対値を比較して、主方向を判断
                            val absDx = kotlin.math.abs(dx)
                            val absDy = kotlin.math.abs(dy)
                            
                            var newLeftPos: Float
                            var newTopPos: Float
                            
                            if (absDx > absDy) {
                                // 横方向のドラッグが主：幅を基準に高さを計算
                                newLeftPos = (currentCropRect.left + dx).coerceIn(0f, (fixedX - minSize).coerceAtLeast(0f))
                                val rectWidth = fixedX - newLeftPos
                                val rectHeight = rectWidth / videoAspectRatio
                                newTopPos = (fixedY - rectHeight).coerceIn(0f, (fixedY - minSize).coerceAtLeast(0f))
                                
                                // 高さが範囲外の場合は幅を調整
                                if (newTopPos <= 0f) {
                                    newTopPos = 0f
                                    val actualHeight = fixedY - newTopPos
                                    val actualWidth = actualHeight * videoAspectRatio
                                    newLeftPos = (fixedX - actualWidth).coerceAtLeast(0f)
                                }
                            } else {
                                // 縦方向のドラッグが主：高さを基準に幅を計算
                                newTopPos = (currentCropRect.top + dy).coerceIn(0f, (fixedY - minSize).coerceAtLeast(0f))
                                val rectHeight = fixedY - newTopPos
                                val rectWidth = rectHeight * videoAspectRatio
                                newLeftPos = (fixedX - rectWidth).coerceIn(0f, (fixedX - minSize).coerceAtLeast(0f))
                                
                                // 幅が範囲外の場合は高さを調整
                                if (newLeftPos <= 0f) {
                                    newLeftPos = 0f
                                    val actualWidth = fixedX - newLeftPos
                                    val actualHeight = actualWidth / videoAspectRatio
                                    newTopPos = (fixedY - actualHeight).coerceAtLeast(0f)
                                }
                            }
                            
                            newLeft = newLeftPos
                            newTop = newTopPos
                            newRight = fixedX
                            newBottom = fixedY
                            Log.d("CropOverlay", "TopLeft: fixed point (${fixedX}, ${fixedY})")
                        }
                        DragMode.TopRight -> {
                            // 左下を固定点として、右上を動かす
                            val fixedX = currentCropRect.left
                            val fixedY = currentCropRect.bottom
                            
                            val absDx = kotlin.math.abs(dx)
                            val absDy = kotlin.math.abs(dy)
                            
                            var newRightPos: Float
                            var newTopPos: Float
                            
                            if (absDx > absDy) {
                                // 横方向のドラッグが主
                                newRightPos = (currentCropRect.right + dx).coerceIn((fixedX + minSize).coerceAtMost(1f), 1f)
                                val rectWidth = newRightPos - fixedX
                                val rectHeight = rectWidth / videoAspectRatio
                                newTopPos = (fixedY - rectHeight).coerceIn(0f, (fixedY - minSize).coerceAtLeast(0f))
                                
                                if (newTopPos <= 0f) {
                                    newTopPos = 0f
                                    val actualHeight = fixedY - newTopPos
                                    val actualWidth = actualHeight * videoAspectRatio
                                    newRightPos = (fixedX + actualWidth).coerceAtMost(1f)
                                }
                            } else {
                                // 縦方向のドラッグが主
                                newTopPos = (currentCropRect.top + dy).coerceIn(0f, (fixedY - minSize).coerceAtLeast(0f))
                                val rectHeight = fixedY - newTopPos
                                val rectWidth = rectHeight * videoAspectRatio
                                newRightPos = (fixedX + rectWidth).coerceIn((fixedX + minSize).coerceAtMost(1f), 1f)
                                
                                if (newRightPos >= 1f) {
                                    newRightPos = 1f
                                    val actualWidth = newRightPos - fixedX
                                    val actualHeight = actualWidth / videoAspectRatio
                                    newTopPos = (fixedY - actualHeight).coerceAtLeast(0f)
                                }
                            }
                            
                            newLeft = fixedX
                            newTop = newTopPos
                            newRight = newRightPos
                            newBottom = fixedY
                            Log.d("CropOverlay", "TopRight: fixed point (${fixedX}, ${fixedY})")
                        }
                        DragMode.BottomLeft -> {
                            // 右上を固定点として、左下を動かす
                            val fixedX = currentCropRect.right
                            val fixedY = currentCropRect.top
                            
                            val absDx = kotlin.math.abs(dx)
                            val absDy = kotlin.math.abs(dy)
                            
                            var newLeftPos: Float
                            var newBottomPos: Float
                            
                            if (absDx > absDy) {
                                // 横方向のドラッグが主
                                newLeftPos = (currentCropRect.left + dx).coerceIn(0f, (fixedX - minSize).coerceAtLeast(0f))
                                val rectWidth = fixedX - newLeftPos
                                val rectHeight = rectWidth / videoAspectRatio
                                newBottomPos = (fixedY + rectHeight).coerceIn((fixedY + minSize).coerceAtMost(1f), 1f)
                                
                                if (newBottomPos >= 1f) {
                                    newBottomPos = 1f
                                    val actualHeight = newBottomPos - fixedY
                                    val actualWidth = actualHeight * videoAspectRatio
                                    newLeftPos = (fixedX - actualWidth).coerceAtLeast(0f)
                                }
                            } else {
                                // 縦方向のドラッグが主
                                newBottomPos = (currentCropRect.bottom + dy).coerceIn((fixedY + minSize).coerceAtMost(1f), 1f)
                                val rectHeight = newBottomPos - fixedY
                                val rectWidth = rectHeight * videoAspectRatio
                                newLeftPos = (fixedX - rectWidth).coerceIn(0f, (fixedX - minSize).coerceAtLeast(0f))
                                
                                if (newLeftPos <= 0f) {
                                    newLeftPos = 0f
                                    val actualWidth = fixedX - newLeftPos
                                    val actualHeight = actualWidth / videoAspectRatio
                                    newBottomPos = (fixedY + actualHeight).coerceAtMost(1f)
                                }
                            }
                            
                            newLeft = newLeftPos
                            newTop = fixedY
                            newRight = fixedX
                            newBottom = newBottomPos
                            Log.d("CropOverlay", "BottomLeft: fixed point (${fixedX}, ${fixedY})")
                        }
                        DragMode.BottomRight -> {
                            // 左上を固定点として、右下を動かす
                            val fixedX = currentCropRect.left
                            val fixedY = currentCropRect.top
                            
                            val absDx = kotlin.math.abs(dx)
                            val absDy = kotlin.math.abs(dy)
                            
                            var newRightPos: Float
                            var newBottomPos: Float
                            
                            if (absDx > absDy) {
                                // 横方向のドラッグが主
                                newRightPos = (currentCropRect.right + dx).coerceIn((fixedX + minSize).coerceAtMost(1f), 1f)
                                val rectWidth = newRightPos - fixedX
                                val rectHeight = rectWidth / videoAspectRatio
                                newBottomPos = (fixedY + rectHeight).coerceIn((fixedY + minSize).coerceAtMost(1f), 1f)
                                
                                if (newBottomPos >= 1f) {
                                    newBottomPos = 1f
                                    val actualHeight = newBottomPos - fixedY
                                    val actualWidth = actualHeight * videoAspectRatio
                                    newRightPos = (fixedX + actualWidth).coerceAtMost(1f)
                                }
                            } else {
                                // 縦方向のドラッグが主
                                newBottomPos = (currentCropRect.bottom + dy).coerceIn((fixedY + minSize).coerceAtMost(1f), 1f)
                                val rectHeight = newBottomPos - fixedY
                                val rectWidth = rectHeight * videoAspectRatio
                                newRightPos = (fixedX + rectWidth).coerceIn((fixedX + minSize).coerceAtMost(1f), 1f)
                                
                                if (newRightPos >= 1f) {
                                    newRightPos = 1f
                                    val actualWidth = newRightPos - fixedX
                                    val actualHeight = actualWidth / videoAspectRatio
                                    newBottomPos = (fixedY + actualHeight).coerceAtMost(1f)
                                }
                            }
                            
                            newLeft = fixedX
                            newTop = fixedY
                            newRight = newRightPos
                            newBottom = newBottomPos
                            Log.d("CropOverlay", "BottomRight: fixed point (${fixedX}, ${fixedY})")
                        }
                        DragMode.Move -> {
                            val widthRect = newRight - newLeft
                            val heightRect = newBottom - newTop
                            
                            newLeft = (newLeft + dx).coerceIn(0f, (1f - widthRect).coerceAtLeast(0f))
                            newRight = newLeft + widthRect
                            newTop = (newTop + dy).coerceIn(0f, (1f - heightRect).coerceAtLeast(0f))
                            newBottom = newTop + heightRect
                            Log.d("CropOverlay", "Move applied")
                        }
                        DragMode.None -> {
                            Log.d("CropOverlay", "ERROR: None mode in drag handler")
                        }
                        // 辺のハンドル（Left, Right, Top, Bottom）は削除
                        else -> {
                            Log.d("CropOverlay", "WARNING: Unsupported drag mode: $dragMode")
                        }
                    }
                    
                    Log.d("CropOverlay", "After:  L=$newLeft, T=$newTop, R=$newRight, B=$newBottom")
                    
                    val newCropRect = CropRect(newLeft, newTop, newRight, newBottom)
                    currentCropRect = newCropRect  // ローカル状態を即座に更新
                    Log.d("CropOverlay", "Calling onCropRectChange with: $newCropRect")
                    onCropRectChange(newCropRect)
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            
            val leftPx = currentCropRect.left * width
            val topPx = currentCropRect.top * height
            val rightPx = currentCropRect.right * width
            val bottomPx = currentCropRect.bottom * height
            
            // トリミング領域外の半透明オーバーレイを描画
            val path = Path().apply {
                // 外側の矩形
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, width, height))
                // 内側の矩形（トリミング領域）を逆方向で追加
                addRect(androidx.compose.ui.geometry.Rect(leftPx, topPx, rightPx, bottomPx))
            }
            
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.5f)
            )
            
            // トリミング枠線を描画
            drawRect(
                color = Color.White,
                topLeft = Offset(leftPx, topPx),
                size = Size(rightPx - leftPx, bottomPx - topPx),
                style = Stroke(width = 3.dp.toPx())
            )
            
            // コーナーのハンドルを描画（四隅のみ）
            val cornerRadius = 10.dp.toPx()
            val cornerColor = Color.White
            
            // 左上
            drawCircle(cornerColor, radius = cornerRadius, center = Offset(leftPx, topPx))
            // 右上
            drawCircle(cornerColor, radius = cornerRadius, center = Offset(rightPx, topPx))
            // 左下
            drawCircle(cornerColor, radius = cornerRadius, center = Offset(leftPx, bottomPx))
            // 右下
            drawCircle(cornerColor, radius = cornerRadius, center = Offset(rightPx, bottomPx))
        }
    }
}