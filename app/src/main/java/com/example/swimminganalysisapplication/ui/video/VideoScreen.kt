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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings // For Start Position Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TAG = "VideoScreen"
private const val SEEK_COMMAND_THRESHOLD_MS = 200 // Threshold to avoid tiny seeks

// Saver for Uri type for rememberSaveable
val UriSaver: Saver<Uri?, String> = Saver(
    save = { uri: Uri? -> uri?.toString() ?: "" }, // scope は不要でした
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
                    modifier = Modifier.fillMaxSize().padding(8.dp),
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

    // Use rememberSaveable for URIs to persist them across configuration changes and process death.
    var videoUri1 by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var exoPlayer1 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio1 by remember { mutableStateOf<Float?>(null) }
    var originalDuration1Ms by remember { mutableStateOf(0L) }
    var startPosition1Ms by rememberSaveable { mutableStateOf(0L) } // Also make start positions saveable

    var videoUri2 by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var exoPlayer2 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio2 by remember { mutableStateOf<Float?>(null) }
    var originalDuration2Ms by remember { mutableStateOf(0L) }
    var startPosition2Ms by rememberSaveable { mutableStateOf(0L) } // Also make start positions saveable


    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isHorizontalLayout by rememberSaveable { mutableStateOf(true) }

    var sharedCurrentPositionMs by rememberSaveable { mutableStateOf(0L) }
    var sharedMaxDurationMs by rememberSaveable { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

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

                    sharedCurrentPositionMs = 0L // Reset shared position
                    isPlaying = false // Stop playback to apply new start times
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
            sharedCurrentPositionMs = sharedCurrentPositionMs.coerceIn(0L, newMax) // Ensure current is within new bounds
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

    val requestCameraPermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) { val i = Intent(MediaStore.ACTION_VIDEO_CAPTURE); if (i.resolveActivity(context.packageManager)!=null) takeVideoLauncher1.launch(i) } }
    val requestStoragePermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) selectVideoLauncher1.launch("video/*") }
    val requestCameraPermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) { val i = Intent(MediaStore.ACTION_VIDEO_CAPTURE); if (i.resolveActivity(context.packageManager)!=null) takeVideoLauncher2.launch(i) } }
    val requestStoragePermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) selectVideoLauncher2.launch("video/*") }


    LaunchedEffect(videoUri1) {
        Log.d(TAG, "PlayerInit1: videoUri1 changed to $videoUri1. Current exoPlayer1: ${exoPlayer1 != null}")
        exoPlayer1?.release(); exoPlayer1 = null; originalDuration1Ms = 0L
        videoUri1?.let { uri ->
            Log.d(TAG, "PlayerInit1: Initializing ExoPlayer for $uri")
            exoPlayer1 = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(vs: VideoSize) { if (vs.width > 0 && vs.height > 0) videoAspectRatio1 = vs.width.toFloat() / vs.height.toFloat() }
                    override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) { originalDuration1Ms = this@apply.duration.takeIf {it!=C.TIME_UNSET && it>0}?:0L; Log.d(TAG, "PlayerInit1: STATE_READY, duration=$originalDuration1Ms"); updateSharedMaxDuration() } }
                    override fun onTimelineChanged(tl: Timeline, reason: Int) { originalDuration1Ms = this@apply.duration.takeIf {it!=C.TIME_UNSET&&it>0}?:0L; Log.d(TAG, "PlayerInit1: TIMELINE_CHANGED, duration=$originalDuration1Ms"); updateSharedMaxDuration() }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) { Log.e(TAG, "PlayerInit1: ExoPlayer Error: ", error) }
                })
                prepare(); playWhenReady = false
            }
        } ?: run { Log.d(TAG, "PlayerInit1: videoUri1 is null, player not created.") }
        updateSharedMaxDuration()
    }
    LaunchedEffect(videoUri2) {
        Log.d(TAG, "PlayerInit2: videoUri2 changed to $videoUri2. Current exoPlayer2: ${exoPlayer2 != null}")
        exoPlayer2?.release(); exoPlayer2 = null; originalDuration2Ms = 0L
        videoUri2?.let { uri ->
            Log.d(TAG, "PlayerInit2: Initializing ExoPlayer for $uri")
            exoPlayer2 = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(vs: VideoSize) { if (vs.width > 0 && vs.height > 0) videoAspectRatio2 = vs.width.toFloat() / vs.height.toFloat() }
                    override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) { originalDuration2Ms = this@apply.duration.takeIf {it!=C.TIME_UNSET && it>0}?:0L; Log.d(TAG, "PlayerInit2: STATE_READY, duration=$originalDuration2Ms"); updateSharedMaxDuration() } }
                    override fun onTimelineChanged(tl: Timeline, reason: Int) { originalDuration2Ms = this@apply.duration.takeIf {it!=C.TIME_UNSET&&it>0}?:0L; Log.d(TAG, "PlayerInit2: TIMELINE_CHANGED, duration=$originalDuration2Ms"); updateSharedMaxDuration() }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) { Log.e(TAG, "PlayerInit2: ExoPlayer Error: ", error) }
                })
                prepare(); playWhenReady = false
            }
        } ?: run { Log.d(TAG, "PlayerInit2: videoUri2 is null, player not created.") }
        updateSharedMaxDuration()
    }

    LaunchedEffect(isPlaying, sharedCurrentPositionMs, isSeeking, startPosition1Ms, startPosition2Ms) {
        if (isSeeking) {
            exoPlayer1?.pause(); exoPlayer2?.pause()
            return@LaunchedEffect
        }
        val targetPos1 = (startPosition1Ms + sharedCurrentPositionMs).coerceIn(0L, originalDuration1Ms)
        val targetPos2 = (startPosition2Ms + sharedCurrentPositionMs).coerceIn(0L, originalDuration2Ms)

        exoPlayer1?.let { p -> if (abs(p.currentPosition - targetPos1) > SEEK_COMMAND_THRESHOLD_MS) p.seekTo(targetPos1); if (isPlaying && targetPos1 < originalDuration1Ms) p.play() else p.pause() }
        exoPlayer2?.let { p -> if (abs(p.currentPosition - targetPos2) > SEEK_COMMAND_THRESHOLD_MS) p.seekTo(targetPos2); if (isPlaying && targetPos2 < originalDuration2Ms) p.play() else p.pause() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (isPlaying && !isSeeking) {
                val p1RelPos = exoPlayer1?.let { (it.currentPosition - startPosition1Ms).coerceAtLeast(0L) }
                val p2RelPos = exoPlayer2?.let { (it.currentPosition - startPosition2Ms).coerceAtLeast(0L) }
                val potentialNewSharedPos = listOfNotNull(p1RelPos, p2RelPos).maxOrNull()
                if (potentialNewSharedPos != null && potentialNewSharedPos != sharedCurrentPositionMs) {
                    sharedCurrentPositionMs = potentialNewSharedPos.coerceIn(0L, sharedMaxDurationMs)
                }
                if (sharedMaxDurationMs > 0 && sharedCurrentPositionMs >= sharedMaxDurationMs) {
                    sharedCurrentPositionMs = sharedMaxDurationMs; isPlaying = false
                }
            }
            delay(100L)
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer1?.release(); exoPlayer1 = null; exoPlayer2?.release(); exoPlayer2 = null } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("動画解析") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "ホームに戻る") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer, navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer)) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.weight(3f).fillMaxWidth()) {
                val duration1Trimmed = (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L)
                val currentPos1InTrimmed = sharedCurrentPositionMs.coerceAtMost(duration1Trimmed)
                val duration2Trimmed = (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L)
                val currentPos2InTrimmed = sharedCurrentPositionMs.coerceAtMost(duration2Trimmed)

                if (isHorizontalLayout) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoPlayerBox(exoPlayer1, videoAspectRatio1, "Video 1", currentPos1InTrimmed, duration1Trimmed, onClick = { videoPlayerTargetForDialog = 1; showVideoSourceDialog = true }, modifier = Modifier.weight(1f).fillMaxHeight())
                        VideoPlayerBox(exoPlayer2, videoAspectRatio2, "Video 2", currentPos2InTrimmed, duration2Trimmed, onClick = { videoPlayerTargetForDialog = 2; showVideoSourceDialog = true }, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoPlayerBox(exoPlayer1, videoAspectRatio1, "Video 1", currentPos1InTrimmed, duration1Trimmed, onClick = { videoPlayerTargetForDialog = 1; showVideoSourceDialog = true }, modifier = Modifier.weight(1f).fillMaxWidth())
                        VideoPlayerBox(exoPlayer2, videoAspectRatio2, "Video 2", currentPos2InTrimmed, duration2Trimmed, onClick = { videoPlayerTargetForDialog = 2; showVideoSourceDialog = true }, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(64.dp)) { Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (isPlaying) "Pause" else "Play", modifier = Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { isHorizontalLayout = !isHorizontalLayout }, modifier = Modifier.size(56.dp)) { Icon(if (isHorizontalLayout) Icons.Filled.SwapVert else Icons.Filled.SwapHoriz, if (isHorizontalLayout) "Vertical" else "Horizontal", modifier = Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                val settingsEnabled = (videoUri1 != null && originalDuration1Ms > 0) || (videoUri2 != null && originalDuration2Ms > 0)
                IconButton(
                    onClick = {
                        val v1UriEnc = videoUri1?.toString()?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
                        val v2UriEnc = videoUri2?.toString()?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
                        if (settingsEnabled) {
                            var route = AppDestinations.START_POSITION_SETTING_ROUTE
                            route += "?currentStart1Ms=$startPosition1Ms&currentStart2Ms=$startPosition2Ms&duration1Ms=$originalDuration1Ms&duration2Ms=$originalDuration2Ms"
                            v1UriEnc?.let { route += "&video1Uri=$it" }; v2UriEnc?.let { route += "&video2Uri=$it" }
                            Log.d(TAG, "Navigating to StartPositionSettingScreen with route: $route")
                            navController.navigate(route)
                        }
                    },
                    enabled = settingsEnabled,
                    modifier = Modifier.size(56.dp)
                ) { Icon(Icons.Filled.Settings, "開始位置設定", modifier = Modifier.fillMaxSize(), tint = if (settingsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(formatTime(sharedCurrentPositionMs), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = if (sharedMaxDurationMs > 0) sharedCurrentPositionMs.toFloat() / sharedMaxDurationMs.toFloat() else 0f,
                    onValueChange = { newValue -> isSeeking = true; sharedCurrentPositionMs = (newValue * sharedMaxDurationMs).toLong().coerceIn(0L, sharedMaxDurationMs) },
                    onValueChangeFinished = { isSeeking = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(formatTime(sharedMaxDurationMs), style = MaterialTheme.typography.bodySmall)
            }
            if (isSeeking || (isPlaying && (exoPlayer1?.isLoading == true || exoPlayer2?.isLoading == true))) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showVideoSourceDialog && videoPlayerTargetForDialog != 0) {
        val currentLocalContext = LocalContext.current
        VideoSourceChooserDialog(
            onDismissRequest = { showVideoSourceDialog = false; videoPlayerTargetForDialog = 0 },
            onTakeVideoClick = { launchCameraAction(currentLocalContext, videoPlayerTargetForDialog, takeVideoLauncher1, takeVideoLauncher2, requestCameraPermissionLauncher1, requestCameraPermissionLauncher2) },
            onSelectVideoClick = { launchGalleryAction(currentLocalContext, videoPlayerTargetForDialog, selectVideoLauncher1, selectVideoLauncher2, requestStoragePermissionLauncher1, requestStoragePermissionLauncher2) }
        )
    }
}

