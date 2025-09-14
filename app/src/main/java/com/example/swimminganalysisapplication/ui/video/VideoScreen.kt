package com.example.swimminganalysisapplication.ui.video

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings // For Start Position Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu // ★ AccountActionsMenu をインポート
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch // Import launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TAG = "VideoScreen"
private const val SEEK_COMMAND_THRESHOLD_MS = 200 // Threshold to avoid tiny seeks

// Saver for Uri type for rememberSaveable
val UriSaver: Saver<Uri?, String> = Saver(
    save = { uri: Uri? -> uri?.toString() ?: "" },
    restore = { value: String -> if (value.isNotEmpty()) Uri.parse(value) else null }
)

private fun formatTime(millis: Long): String {
    if (millis == C.TIME_UNSET || millis < 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun VideoPlayerBox(
    exoPlayer: ExoPlayer?,
    videoAspectRatio: Float?,
    videoName: String,
    currentPositionInTrimmedView: Long,
    durationOfTrimmedView: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(videoAspectRatio ?: 16f / 9f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false // Ensure controller is off
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (durationOfTrimmedView > 0L) {
                    LinearProgressIndicator(
                        progress = { currentPositionInTrimmedView.toFloat() / durationOfTrimmedView.toFloat() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$videoName: タップして読込", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun launchCameraAction(
    context: Context, videoIndex: Int,
    actualTakeVideoLauncher1: ActivityResultLauncher<Intent>, actualTakeVideoLauncher2: ActivityResultLauncher<Intent>,
    permissionRequestLauncherForVid1: ActivityResultLauncher<String>, permissionRequestLauncherForVid2: ActivityResultLauncher<String>
) {
    val permission = Manifest.permission.CAMERA
    val actionLauncher = if (videoIndex == 1) actualTakeVideoLauncher1 else actualTakeVideoLauncher2
    val permRequester = if (videoIndex == 1) permissionRequestLauncherForVid1 else permissionRequestLauncherForVid2
    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (takeVideoIntent.resolveActivity(context.packageManager) != null) actionLauncher.launch(takeVideoIntent)
        else Log.e(TAG, "No activity for ACTION_VIDEO_CAPTURE video $videoIndex")
    } else permRequester.launch(permission)
}

private fun launchGalleryAction(
    context: Context, videoIndex: Int,
    actualSelectVideoLauncher1: ActivityResultLauncher<String>, actualSelectVideoLauncher2: ActivityResultLauncher<String>,
    permissionRequestLauncherForVid1: ActivityResultLauncher<String>, permissionRequestLauncherForVid2: ActivityResultLauncher<String>
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
    val actionLauncher = if (videoIndex == 1) actualSelectVideoLauncher1 else actualSelectVideoLauncher2
    val permRequester = if (videoIndex == 1) permissionRequestLauncherForVid1 else permissionRequestLauncherForVid2
    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) actionLauncher.launch("video/*")
    else permRequester.launch(permission)
}

@Composable
private fun VideoSourceChooserDialog(onDismissRequest: () -> Unit, onTakeVideoClick: () -> Unit, onSelectVideoClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest, title = { Text("ビデオソースを選択") }, text = { Text("どのようにビデオを読み込みますか？") },
        confirmButton = { Button(onClick = { onTakeVideoClick(); onDismissRequest() }) { Text("動画を撮影") } },
        dismissButton = { Button(onClick = { onSelectVideoClick(); onDismissRequest() }) { Text("ギャラリーから選択") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Get a CoroutineScope

    var videoUri1 by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var exoPlayer1 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio1 by remember { mutableStateOf<Float?>(null) }
    var originalDuration1Ms by remember { mutableStateOf(0L) }
    var startPosition1Ms by rememberSaveable { mutableStateOf(0L) }

    var videoUri2 by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var exoPlayer2 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio2 by remember { mutableStateOf<Float?>(null) }
    var originalDuration2Ms by remember { mutableStateOf(0L) }
    var startPosition2Ms by rememberSaveable { mutableStateOf(0L) }


    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isHorizontalLayout by rememberSaveable { mutableStateOf(true) }

    var sharedCurrentPositionMs by rememberSaveable { mutableStateOf(0L) }
    var sharedMaxDurationMs by rememberSaveable { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) } // Used to prevent position updates during seek

    var showVideoSourceDialog by remember { mutableStateOf(false) }
    var videoPlayerTargetForDialog by remember { mutableStateOf(0) }

    val currentBackStackEntry = navController.currentBackStackEntry
    DisposableEffect(currentBackStackEntry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d(TAG, "ON_RESUME: videoUri1 = $videoUri1, videoUri2 = $videoUri2, startPos1=$startPosition1Ms, startPos2=$startPosition2Ms")
                if (currentBackStackEntry?.savedStateHandle?.contains("newStart1Ms") == true &&
                    currentBackStackEntry.savedStateHandle.contains("newStart2Ms") == true) {
                    Log.d(TAG, "ON_RESUME: Found new start positions in savedStateHandle")
                    val newStart1 = currentBackStackEntry.savedStateHandle.get<Long>("newStart1Ms") ?: startPosition1Ms
                    val newStart2 = currentBackStackEntry.savedStateHandle.get<Long>("newStart2Ms") ?: startPosition2Ms

                    startPosition1Ms = min(newStart1, (originalDuration1Ms - 1L).coerceAtLeast(0L))
                    startPosition2Ms = min(newStart2, (originalDuration2Ms - 1L).coerceAtLeast(0L))
                    Log.d(TAG, "ON_RESUME: Applied new startPos1=$startPosition1Ms, new startPos2=$startPosition2Ms")


                    currentBackStackEntry.savedStateHandle.remove<Long>("newStart1Ms")
                    currentBackStackEntry.savedStateHandle.remove<Long>("newStart2Ms")

                    sharedCurrentPositionMs = 0L
                    isPlaying = false
                }
            }
        }
        currentBackStackEntry?.lifecycle?.addObserver(observer)
        onDispose { currentBackStackEntry?.lifecycle?.removeObserver(observer) }
    }

    val updateSharedMaxDuration = {
        val trimmedDuration1 = (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L)
        val trimmedDuration2 = (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L)
        val newMax =if (exoPlayer1 != null && originalDuration1Ms > 0 && exoPlayer2 != null && originalDuration2Ms > 0) {
            max(trimmedDuration1, trimmedDuration2)
        } else if (exoPlayer1 != null && originalDuration1Ms > 0) {
            trimmedDuration1
        } else if (exoPlayer2 != null && originalDuration2Ms > 0) {
            trimmedDuration2
        } else {
            0L
        }
        if (sharedMaxDurationMs != newMax) {
            Log.d(TAG, "updateSharedMaxDuration: OldMax=$sharedMaxDurationMs, NewMax=$newMax. currentSharedPos=$sharedCurrentPositionMs")
            sharedMaxDurationMs = newMax
            sharedCurrentPositionMs = sharedCurrentPositionMs.coerceIn(0L, newMax) // Ensure current position is within new max
        }
    }
    LaunchedEffect(startPosition1Ms, startPosition2Ms, originalDuration1Ms, originalDuration2Ms, exoPlayer1, exoPlayer2) {
        Log.d(TAG, "LaunchedEffect to updateSharedMaxDuration: dep changed.")
        updateSharedMaxDuration()
    }

    val takeVideoLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "takeVideoLauncher1: URI obtained: $uri")
                videoUri1 = uri; startPosition1Ms = 0L
                try { if ("content" == uri.scheme) context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); Log.d(TAG, "Persisted URI for captured video 1: $uri") }
                catch (e: SecurityException) { Log.e(TAG, "Failed to persist URI for captured video 1: $uri", e) }
            }
        }
    }
    val selectVideoLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            Log.d(TAG, "selectVideoLauncher1: URI obtained: $it")
            videoUri1 = it; startPosition1Ms = 0L
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); Log.d(TAG, "Persisted URI for selected video 1: $it") }
            catch (e: SecurityException) { Log.e(TAG, "Failed to persist URI for selected video 1: $it", e) }
        }
    }
    val takeVideoLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "takeVideoLauncher2: URI obtained: $uri")
                videoUri2 = uri; startPosition2Ms = 0L
                try { if ("content" == uri.scheme) context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); Log.d(TAG, "Persisted URI for captured video 2: $uri") }
                catch (e: SecurityException) { Log.e(TAG, "Failed to persist URI for captured video 2: $uri", e) }
            }
        }
    }
    val selectVideoLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            Log.d(TAG, "selectVideoLauncher2: URI obtained: $it")
            videoUri2 = it; startPosition2Ms = 0L
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); Log.d(TAG, "Persisted URI for selected video 2: $it") }
            catch (e: SecurityException) { Log.e(TAG, "Failed to persist URI for selected video 2: $it", e) }
        }
    }

    lateinit var requestCameraPermissionLauncher1: ActivityResultLauncher<String>
    lateinit var requestStoragePermissionLauncher1: ActivityResultLauncher<String>
    lateinit var requestCameraPermissionLauncher2: ActivityResultLauncher<String>
    lateinit var requestStoragePermissionLauncher2: ActivityResultLauncher<String>

    requestCameraPermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCameraAction(context, 1, takeVideoLauncher1, takeVideoLauncher2, requestCameraPermissionLauncher1, requestCameraPermissionLauncher2)
        else Log.w(TAG, "Camera permission denied for video 1")
    }
    requestStoragePermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchGalleryAction(context, 1, selectVideoLauncher1, selectVideoLauncher2, requestStoragePermissionLauncher1, requestStoragePermissionLauncher2)
        else Log.w(TAG, "Storage permission denied for video 1")
    }
    requestCameraPermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchCameraAction(context, 2, takeVideoLauncher1, takeVideoLauncher2, requestCameraPermissionLauncher1, requestCameraPermissionLauncher2)
        else Log.w(TAG, "Camera permission denied for video 2")
    }
    requestStoragePermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) launchGalleryAction(context, 2, selectVideoLauncher1, selectVideoLauncher2, requestStoragePermissionLauncher1, requestStoragePermissionLauncher2)
        else Log.w(TAG, "Storage permission denied for video 2")
    }

    fun initializeOrUpdatePlayer(player: ExoPlayer?, uri: Uri?, startPosMs: Long,
                                 setPlayer: (ExoPlayer?) -> Unit, setAspectRatio: (Float?) -> Unit,
                                 setOriginalDuration: (Long) -> Unit) {
        if (uri == null) {
            player?.release()
            setPlayer(null)
            setAspectRatio(null)
            setOriginalDuration(0L)
            updateSharedMaxDuration()
            return
        }

        val newPlayer = player ?: ExoPlayer.Builder(context).build().also {
            it.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val aspectRatio = if (videoSize.height == 0) 16f / 9f else videoSize.width.toFloat() / videoSize.height
                    setAspectRatio(aspectRatio)
                    Log.d(TAG, "Video size changed: ${videoSize.width}x${videoSize.height}, AspectRatio: $aspectRatio")
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (timeline.windowCount > 0) {
                        val window = Timeline.Window()
                        timeline.getWindow(0, window)
                        val duration = window.durationMs
                        if (duration != C.TIME_UNSET && duration > 0) {
                            setOriginalDuration(duration)
                            Log.d(TAG, "Timeline changed. Original duration: $duration ms for player of $uri")
                            updateSharedMaxDuration()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                    Log.d(TAG, "ExoPlayer (uri: $uri) onIsPlayingChanged: $isPlayingChange. Current composable isPlaying: $isPlaying")
                }
            })
        }
        setPlayer(newPlayer)
        val mediaItem = MediaItem.fromUri(uri)
        newPlayer.setMediaItem(mediaItem)
        newPlayer.prepare()
        newPlayer.playWhenReady = false
        newPlayer.seekTo(startPosMs)
        Log.d(TAG, "Player initialized/updated for URI: $uri, seeking to $startPosMs ms. playWhenReady initially false.")
    }

    DisposableEffect(videoUri1, exoPlayer1) {
        if (videoUri1 != null && exoPlayer1 == null) {
            initializeOrUpdatePlayer(null, videoUri1, startPosition1Ms, { exoPlayer1 = it }, { videoAspectRatio1 = it }, { originalDuration1Ms = it })
        }
        onDispose {
            if (videoUri1 == null) {
                exoPlayer1?.release()
                exoPlayer1 = null
                Log.d(TAG, "Disposed and released exoPlayer1 because videoUri1 became null.")
            }
        }
    }
    DisposableEffect(videoUri2, exoPlayer2) {
        if (videoUri2 != null && exoPlayer2 == null) {
            initializeOrUpdatePlayer(null, videoUri2, startPosition2Ms, { exoPlayer2 = it }, { videoAspectRatio2 = it }, { originalDuration2Ms = it })
        }
        onDispose {
            if (videoUri2 == null) {
                exoPlayer2?.release()
                exoPlayer2 = null
                Log.d(TAG, "Disposed and released exoPlayer2 because videoUri2 became null.")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "VideoScreen onDispose: Releasing players.")
            exoPlayer1?.release()
            exoPlayer1 = null
            exoPlayer2?.release()
            exoPlayer2 = null
        }
    }

    LaunchedEffect(isPlaying, sharedMaxDurationMs) {
        if (sharedMaxDurationMs <= 0) {
            if (isPlaying) isPlaying = false
            return@LaunchedEffect
        }

        if (isPlaying) {
            var lastUpdateTime = System.currentTimeMillis()
            exoPlayer1?.play()
            exoPlayer2?.play()
            Log.d(TAG, "LaunchedEffect: isPlaying is true. Advancing position. Player1.play() and Player2.play() called.")

            while (isPlaying && sharedCurrentPositionMs < sharedMaxDurationMs) {
                delay(50)
                if (!isPlaying) break

                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - lastUpdateTime
                lastUpdateTime = currentTime

                if (!isSeeking) {
                    val newSharedPosition = (sharedCurrentPositionMs + elapsed).coerceAtMost(sharedMaxDurationMs)
                    if (abs(newSharedPosition - sharedCurrentPositionMs) > 10) {
                        sharedCurrentPositionMs = newSharedPosition
                    }

                    exoPlayer1?.let { player ->
                        val targetPos1 = (startPosition1Ms + sharedCurrentPositionMs).coerceIn(0, originalDuration1Ms)
                        if (abs(player.currentPosition - targetPos1) > SEEK_COMMAND_THRESHOLD_MS + 200) {
                            player.seekTo(targetPos1)
                        }
                    }
                    exoPlayer2?.let { player ->
                        val targetPos2 = (startPosition2Ms + sharedCurrentPositionMs).coerceIn(0, originalDuration2Ms)
                        if (abs(player.currentPosition - targetPos2) > SEEK_COMMAND_THRESHOLD_MS + 200) {
                            player.seekTo(targetPos2)
                        }
                    }
                }
            }
            if (sharedCurrentPositionMs >= sharedMaxDurationMs && isPlaying) {
                isPlaying = false
                Log.d(TAG, "LaunchedEffect: Playback reached end. Setting isPlaying to false.")
            } else if (!isPlaying) {
                Log.d(TAG, "LaunchedEffect: isPlaying became false during loop. Pausing players.")
                exoPlayer1?.pause()
                exoPlayer2?.pause()
            }
        } else {
            Log.d(TAG, "LaunchedEffect: isPlaying is false. Pausing players.")
            exoPlayer1?.pause()
            exoPlayer2?.pause()
        }
    }

    val togglePlayPause = {
        if (exoPlayer1 != null || exoPlayer2 != null) {
            val newIsPlayingState = !isPlaying
            isPlaying = newIsPlayingState

            if (newIsPlayingState) {
                Log.d(TAG, "togglePlayPause: Set isPlaying to TRUE. Telling players to play.")
                if (sharedCurrentPositionMs >= sharedMaxDurationMs && sharedMaxDurationMs > 0) {
                    sharedCurrentPositionMs = 0L
                    exoPlayer1?.seekTo(startPosition1Ms)
                    exoPlayer2?.seekTo(startPosition2Ms)
                    Log.d(TAG, "Playback reset to start as it was at the end.")
                }
                exoPlayer1?.play()
                exoPlayer2?.play()
            } else {
                Log.d(TAG, "togglePlayPause: Set isPlaying to FALSE. Telling players to pause.")
                exoPlayer1?.pause()
                exoPlayer2?.pause()
            }
        }
    }

    val onSeek = { newPositionFraction: Float ->
        if (sharedMaxDurationMs > 0) {
            isSeeking = true
            val newPosition = (newPositionFraction * sharedMaxDurationMs).toLong()
            sharedCurrentPositionMs = newPosition.coerceIn(0L, sharedMaxDurationMs)

            val targetPos1 = (startPosition1Ms + sharedCurrentPositionMs).coerceIn(0, originalDuration1Ms.coerceAtLeast(0L))
            exoPlayer1?.seekTo(targetPos1)

            val targetPos2 = (startPosition2Ms + sharedCurrentPositionMs).coerceIn(0, originalDuration2Ms.coerceAtLeast(0L))
            exoPlayer2?.seekTo(targetPos2)

            Log.d(TAG, "Seeked to: $sharedCurrentPositionMs ms (Player1: $targetPos1, Player2: $targetPos2)")

            coroutineScope.launch { // Use coroutineScope to launch delay
                delay(100)
                isSeeking = false
            }
        }
    }


    if (showVideoSourceDialog) {
        VideoSourceChooserDialog(
            onDismissRequest = { showVideoSourceDialog = false },
            onTakeVideoClick = {
                launchCameraAction(context, videoPlayerTargetForDialog, takeVideoLauncher1, takeVideoLauncher2, requestCameraPermissionLauncher1, requestCameraPermissionLauncher2)
            },
            onSelectVideoClick = {
                launchGalleryAction(context, videoPlayerTargetForDialog, selectVideoLauncher1, selectVideoLauncher2, requestStoragePermissionLauncher1, requestStoragePermissionLauncher2)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("動画比較 & 解析") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") } },
                actions = {
                    IconButton(onClick = { navController.navigate(AppDestinations.ANALYSIS_LIST_SCREEN_ROUTE) }) {
                        Icon(Icons.Filled.Assessment, "分析結果一覧")
                    }
                    IconButton(onClick = {
                        val encodedUri1 = videoUri1?.let { URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString()) } ?: "null"
                        val encodedUri2 = videoUri2?.let { URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString()) } ?: "null"

                        Log.d(TAG, "Navigating to StartPositionSettingScreen with durations: originalDuration1Ms = $originalDuration1Ms, originalDuration2Ms = $originalDuration2Ms, startPosition1Ms = $startPosition1Ms, startPosition2Ms = $startPosition2Ms")

                        navController.navigate(
                            "${AppDestinations.START_POSITION_SETTING_ROUTE}/$encodedUri1/$encodedUri2/$startPosition1Ms/$startPosition2Ms/$originalDuration1Ms/$originalDuration2Ms"
                        )
                    }) {
                        Icon(Icons.Filled.Settings, "開始位置設定")
                    }
                    IconButton(onClick = { isHorizontalLayout = !isHorizontalLayout }) {
                        Icon(if (isHorizontalLayout) Icons.Filled.SwapVert else Icons.Filled.SwapHoriz, if (isHorizontalLayout) "縦並びに変更" else "横並びに変更")
                    }
                    AccountActionsMenu(navController = navController) // ★ AccountActionsMenu を追加
                },
                colors = TopAppBarDefaults.topAppBarColors( // ★ 色設定を更新
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer // ★ onPrimaryContainer を追加
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (isHorizontalLayout) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VideoPlayerBox(
                            exoPlayer = exoPlayer1, videoAspectRatio = videoAspectRatio1, videoName = "ビデオ1",
                            currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L)),
                            durationOfTrimmedView = (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L),
                            onClick = { videoPlayerTargetForDialog = 1; showVideoSourceDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .padding(if (exoPlayer2 != null) PaddingValues(end = 2.dp) else PaddingValues())
                        )
                        if (exoPlayer1 != null && exoPlayer2 != null) Spacer(modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant))
                        VideoPlayerBox(
                            exoPlayer = exoPlayer2, videoAspectRatio = videoAspectRatio2, videoName = "ビデオ2",
                            currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L)),
                            durationOfTrimmedView = (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L),
                            onClick = { videoPlayerTargetForDialog = 2; showVideoSourceDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .padding(if (exoPlayer1 != null) PaddingValues(start = 2.dp) else PaddingValues())
                        )
                    }
                } else { // Vertical layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        VideoPlayerBox(
                            exoPlayer = exoPlayer1, videoAspectRatio = videoAspectRatio1, videoName = "ビデオ1",
                            currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L)),
                            durationOfTrimmedView = (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L),
                            onClick = { videoPlayerTargetForDialog = 1; showVideoSourceDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (exoPlayer1 != null || exoPlayer2 != null) {
                            Spacer(Modifier.height(8.dp))
                        }
                        VideoPlayerBox(
                            exoPlayer = exoPlayer2, videoAspectRatio = videoAspectRatio2, videoName = "ビデオ2",
                            currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L)),
                            durationOfTrimmedView = (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L),
                            onClick = { videoPlayerTargetForDialog = 2; showVideoSourceDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }


            // Controls section
            if (sharedMaxDurationMs > 0) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(formatTime(sharedCurrentPositionMs), style = MaterialTheme.typography.bodySmall)
                        Text(formatTime(sharedMaxDurationMs), style = MaterialTheme.typography.bodySmall)
                    }
                    Slider(
                        value = if (sharedMaxDurationMs > 0) sharedCurrentPositionMs.toFloat() / sharedMaxDurationMs.toFloat() else 0f,
                        onValueChange = { onSeek(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(56.dp)) // Placeholder for controls height when no video
            }


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = togglePlayPause, enabled = sharedMaxDurationMs > 0) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "一時停止" else "再生")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(if (isPlaying) "一時停止" else "再生")
                }
            }
        }
    }
}
