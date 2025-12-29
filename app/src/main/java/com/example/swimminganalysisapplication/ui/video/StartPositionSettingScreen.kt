package com.example.swimminganalysisapplication.ui.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.swimminganalysisapplication.util.AppLog

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
    
    // 位置合わせポイント（ビデオ上で表示される位置）- SavedStateHandle から復元
    var alignmentPoint1Ms by remember { 
        mutableStateOf(savedStateHandle?.get<Long>("alignmentPoint1Ms") ?: initialStart1Ms) 
    }
    var alignmentPoint2Ms by remember { 
        mutableStateOf(savedStateHandle?.get<Long>("alignmentPoint2Ms") ?: initialStart2Ms) 
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

    // ExoPlayer instances - 遅延初期化でメモリ節約
    var exoPlayer1Preview by remember { mutableStateOf<ExoPlayer?>(null) }
    var exoPlayer2Preview by remember { mutableStateOf<ExoPlayer?>(null) }
    
    // ExoPlayerの初期化を遅延させる
    fun ensurePlayer1() {
        if (exoPlayer1Preview == null && video1UriString != null) {
            exoPlayer1Preview = ExoPlayer.Builder(context)
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(video1UriString)))
                    prepare()
                    playWhenReady = false
                }
        }
    }
    
    fun ensurePlayer2() {
        if (exoPlayer2Preview == null && video2UriString != null) {
            exoPlayer2Preview = ExoPlayer.Builder(context)
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(Uri.parse(video2UriString)))
                    prepare()
                    playWhenReady = false
                }
        }
    }
    
    // ビデオ1の初期化とシーク（画面起動時のみ）
    LaunchedEffect(Unit) {
        ensurePlayer1()
        exoPlayer1Preview?.seekTo(alignmentPoint1Ms)
    }
    
    // ビデオ2の初期化とシーク（画面起動時のみ）
    LaunchedEffect(Unit) {
        ensurePlayer2()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
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
                    durationMs = duration1Ms
                )
            } else if (video1UriString != null) {
                Text("ビデオ1: 長さ情報なし", style = MaterialTheme.typography.titleMedium)
            }

            // Video 2 Settings
            if (video2UriString != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("ビデオ2 位置合わせ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
                
                if (duration2Ms > 0) {
                    VideoPreviewAndSlider(
                        exoPlayer = exoPlayer2Preview,
                        alignmentPointMs = alignmentPoint2Ms,
                        onAlignmentPointChange = { newValue ->
                            alignmentPoint2Ms = newValue
                            exoPlayer2Preview?.seekTo(newValue)
                        },
                        durationMs = duration2Ms
                    )
                } else {
                    Text("ビデオ2: 長さ情報なし")
                }
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
    durationMs: Long
) {
    // スライダードラッグ中の一時的な値（更新頻度削減のため）
    var tempSliderValue by remember { mutableStateOf(alignmentPointMs) }
    var isSliderDragging by remember { mutableStateOf(false) }
    
    // スライダーが離された時のみ実際の値を更新
    val displayedValue = if (isSliderDragging) tempSliderValue else alignmentPointMs
    
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
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("位置合わせ: ${formatTime(displayedValue)} / 総時間: ${formatTime(durationMs)}")
        
        // 位置合わせポイントスライダー（ドラッグ中は更新を保留）
        Slider(
            value = if (durationMs > 0) displayedValue.toFloat() / durationMs.toFloat() else 0f,
            onValueChange = { newValue -> 
                isSliderDragging = true
                tempSliderValue = (newValue * durationMs).toLong()
            },
            onValueChangeFinished = {
                isSliderDragging = false
                onAlignmentPointChange(tempSliderValue)
            },
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