package com.example.swimminganalysisapplication.ui.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController

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

    var selectedStart1Ms by remember { mutableStateOf(initialStart1Ms) }
    var selectedStart2Ms by remember { mutableStateOf(initialStart2Ms) }

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

    // Seek preview players when slider changes
    LaunchedEffect(selectedStart1Ms) {
        exoPlayer1Preview?.seekTo(selectedStart1Ms)
    }
    LaunchedEffect(selectedStart2Ms) {
        exoPlayer2Preview?.seekTo(selectedStart2Ms)
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
                        navController.previousBackStackEntry?.savedStateHandle?.set("newStart1Ms", selectedStart1Ms)
                        navController.previousBackStackEntry?.savedStateHandle?.set("newStart2Ms", selectedStart2Ms)
                        navController.popBackStack()
                    }) {
                        Text("完了")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video 1 Settings
            if (video1UriString != null && duration1Ms > 0) {
                Text("ビデオ1 開始位置", style = MaterialTheme.typography.titleMedium)
                VideoPreviewAndSlider(
                    exoPlayer = exoPlayer1Preview,
                    selectedStartMs = selectedStart1Ms,
                    durationMs = duration1Ms,
                    onValueChange = { selectedStart1Ms = it }
                )
            } else if (video1UriString != null) {
                Text("ビデオ1: 長さ情報なし", style = MaterialTheme.typography.titleMedium)
            }


            // Video 2 Settings
            if (video2UriString != null && duration2Ms > 0) {
                Text("ビデオ2 開始位置", style = MaterialTheme.typography.titleMedium)
                VideoPreviewAndSlider(
                    exoPlayer = exoPlayer2Preview,
                    selectedStartMs = selectedStart2Ms,
                    durationMs = duration2Ms,
                    onValueChange = { selectedStart2Ms = it }
                )
            } else if (video2UriString != null) {
                Text("ビデオ2: 長さ情報なし", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun VideoPreviewAndSlider(
    exoPlayer: ExoPlayer?,
    selectedStartMs: Long,
    durationMs: Long,
    onValueChange: (Long) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (exoPlayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
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
        Text("開始: ${formatTime(selectedStartMs)} / 総時間: ${formatTime(durationMs)}")
        Slider(
            value = if (durationMs > 0) selectedStartMs.toFloat() / durationMs.toFloat() else 0f,
            onValueChange = { newValue -> onValueChange((newValue * durationMs).toLong()) },
            valueRange = 0f..(if (durationMs > 0) 1f else 0f),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}