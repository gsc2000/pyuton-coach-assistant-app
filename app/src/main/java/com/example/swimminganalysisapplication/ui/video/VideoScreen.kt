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
import androidx.compose.material.icons.automirrored.filled.ArrowBack // For Back Arrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api // For TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold // For Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar // For TopAppBar
import androidx.compose.material3.TopAppBarDefaults // For TopAppBar colors
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations // Ensure this is imported
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private const val TAG = "VideoScreen"

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
    currentPosition: Long,
    duration: Long,
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
                            useController = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                if (duration > 0L) {
                    LinearProgressIndicator(
                        progress = { currentPosition.toFloat() / duration.toFloat() },
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
    context: Context,
    videoIndex: Int,
    takeVideoLauncher1: ActivityResultLauncher<Intent>,
    requestCameraPermissionLauncher1: ActivityResultLauncher<String>,
    takeVideoLauncher2: ActivityResultLauncher<Intent>,
    requestCameraPermissionLauncher2: ActivityResultLauncher<String>
) {
    val permission = Manifest.permission.CAMERA
    val launcher = if (videoIndex == 1) takeVideoLauncher1 else takeVideoLauncher2
    val permLauncher = if (videoIndex == 1) requestCameraPermissionLauncher1 else requestCameraPermissionLauncher2

    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (takeVideoIntent.resolveActivity(context.packageManager) != null) {
            launcher.launch(takeVideoIntent)
        } else {
            Log.e(TAG, "No activity found to handle ACTION_VIDEO_CAPTURE for video $videoIndex")
        }
    } else {
        permLauncher.launch(permission)
    }
}

private fun launchGalleryAction(
    context: Context,
    videoIndex: Int,
    selectVideoLauncher1: ActivityResultLauncher<String>,
    requestStoragePermissionLauncher1: ActivityResultLauncher<String>,
    selectVideoLauncher2: ActivityResultLauncher<String>,
    requestStoragePermissionLauncher2: ActivityResultLauncher<String>
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
    val launcher = if (videoIndex == 1) selectVideoLauncher1 else selectVideoLauncher2
    val permLauncher = if (videoIndex == 1) requestStoragePermissionLauncher1 else requestStoragePermissionLauncher2

    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        launcher.launch("video/*")
    } else {
        permLauncher.launch(permission)
    }
}

@Composable
private fun VideoSourceChooserDialog(
    onDismissRequest: () -> Unit,
    onTakeVideoClick: () -> Unit,
    onSelectVideoClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "ビデオソースを選択") },
        text = { Text(text = "どのようにビデオを読み込みますか？") },
        confirmButton = {
            Button(onClick = { onTakeVideoClick(); onDismissRequest() }) { Text("動画を撮影") }
        },
        dismissButton = {
            Button(onClick = { onSelectVideoClick(); onDismissRequest() }) { Text("ギャラリーから選択") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class) // For TopAppBar
@Composable
fun VideoScreen(navController: NavController) {
    val context = LocalContext.current

    var videoUri1 by remember { mutableStateOf<Uri?>(null) }
    var exoPlayer1 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio1 by remember { mutableStateOf<Float?>(null) }
    var individualCurrentPosition1 by remember { mutableStateOf(0L) }
    var individualDuration1 by remember { mutableStateOf(0L) }

    var videoUri2 by remember { mutableStateOf<Uri?>(null) }
    var exoPlayer2 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio2 by remember { mutableStateOf<Float?>(null) }
    var individualCurrentPosition2 by remember { mutableStateOf(0L) }
    var individualDuration2 by remember { mutableStateOf(0L) }

    var isPlaying by remember { mutableStateOf(false) }
    var isHorizontalLayout by remember { mutableStateOf(true) }

    var maxDuration by remember { mutableStateOf(0L) }
    var currentPosition by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    var showVideoSourceDialog by remember { mutableStateOf(false) }
    var videoPlayerTargetForDialog by remember { mutableStateOf(0) }


    val updateIndividualDurations = {
        individualDuration1 = exoPlayer1?.duration?.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        individualDuration2 = exoPlayer2?.duration?.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
    }

    val updateSharedMaxDuration = {
        updateIndividualDurations()
        maxDuration = max(individualDuration1, individualDuration2)
        if (currentPosition > maxDuration && maxDuration > 0L) currentPosition = maxDuration
        else if (maxDuration == 0L) currentPosition = 0L
    }

    val takeVideoLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let { videoUri1 = it } }
    val selectVideoLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> videoUri1 = uri }
    val takeVideoLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let { videoUri2 = it } }
    val selectVideoLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> videoUri2 = uri }

    val requestCameraPermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            if (takeVideoIntent.resolveActivity(context.packageManager) != null) {
                takeVideoLauncher1.launch(takeVideoIntent)
            } else { Log.e(TAG, "No activity to handle ACTION_VIDEO_CAPTURE for video 1 (permission callback)") }
        }
    }
    val requestStoragePermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { selectVideoLauncher1.launch("video/*") }
    }
    val requestCameraPermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            if (takeVideoIntent.resolveActivity(context.packageManager) != null) {
                takeVideoLauncher2.launch(takeVideoIntent)
            } else { Log.e(TAG, "No activity to handle ACTION_VIDEO_CAPTURE for video 2 (permission callback)") }
        }
    }
    val requestStoragePermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { selectVideoLauncher2.launch("video/*") }
    }

    LaunchedEffect(videoUri1) {
        exoPlayer1?.release(); exoPlayer1 = null
        individualCurrentPosition1 = 0L; individualDuration1 = 0L
        if (videoUri1 != null) {
            exoPlayer1 = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri1!!))
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) { if (videoSize.width > 0 && videoSize.height > 0) videoAspectRatio1 = videoSize.width.toFloat() / videoSize.height.toFloat() }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) updateSharedMaxDuration()
                        else if (playbackState == Player.STATE_ENDED) {
                            individualCurrentPosition1 = individualDuration1
                            if (exoPlayer2 == null || exoPlayer2?.playbackState == Player.STATE_ENDED) if(isPlaying) isPlaying = false
                        }
                    }
                    override fun onTimelineChanged(timeline: Timeline, reason: Int) { updateSharedMaxDuration() }
                })
                prepare()
            }
            updateSharedMaxDuration()
        } else { videoAspectRatio1 = null; updateSharedMaxDuration(); if (exoPlayer2 == null) currentPosition = 0L }
    }
    LaunchedEffect(videoUri2) {
        exoPlayer2?.release(); exoPlayer2 = null
        individualCurrentPosition2 = 0L; individualDuration2 = 0L
        if (videoUri2 != null) {
            exoPlayer2 = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri2!!))
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) { if (videoSize.width > 0 && videoSize.height > 0) videoAspectRatio2 = videoSize.width.toFloat() / videoSize.height.toFloat() }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) updateSharedMaxDuration()
                        else if (playbackState == Player.STATE_ENDED) {
                            individualCurrentPosition2 = individualDuration2
                            if (exoPlayer1 == null || exoPlayer1?.playbackState == Player.STATE_ENDED) if(isPlaying) isPlaying = false
                        }
                    }
                    override fun onTimelineChanged(timeline: Timeline, reason: Int) { updateSharedMaxDuration() }
                })
                prepare()
            }
            updateSharedMaxDuration()
        } else { videoAspectRatio2 = null; updateSharedMaxDuration(); if (exoPlayer1 == null) currentPosition = 0L }
    }

    LaunchedEffect(isPlaying, exoPlayer1, exoPlayer2) {
        if (isPlaying) {
            exoPlayer1?.let { player -> val dur = individualDuration1.takeIf{it>0L} ?: player.duration; val target = min(currentPosition, dur); if(player.playbackState == Player.STATE_IDLE) player.prepare(); player.seekTo(target); if (target < dur || (dur==0L && target==0L)) player.play() else player.pause() }
            exoPlayer2?.let { player -> val dur = individualDuration2.takeIf{it>0L} ?: player.duration; val target = min(currentPosition, dur); if(player.playbackState == Player.STATE_IDLE) player.prepare(); player.seekTo(target); if (target < dur || (dur==0L && target==0L)) player.play() else player.pause() }
        } else {
            exoPlayer1?.pause(); exoPlayer2?.pause()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (isPlaying && !isSeeking) {
                val activePlayer = run {
                    val p1 = exoPlayer1; val d1 = individualDuration1; val p2 = exoPlayer2; val d2 = individualDuration2
                    fun canDrive(p: Player?, dur: Long) = p != null && p.playbackState != Player.STATE_ENDED && (p.isPlaying || (dur > 0L && p.currentPosition < dur))
                    if (canDrive(p1, d1)) p1 else if (canDrive(p2, d2)) p2 else if (p1 != null && p1.playbackState != Player.STATE_ENDED) p1 else if (p2 != null && p2.playbackState != Player.STATE_ENDED) p2 else p1 ?: p2
                }
                activePlayer?.currentPosition?.let { pos -> if (pos != C.TIME_UNSET && pos >= 0) { val newPos = if (maxDuration > 0L) min(pos, maxDuration) else pos; if (currentPosition != newPos) currentPosition = newPos } }
            }
            exoPlayer1?.currentPosition?.let { pos -> if (pos != C.TIME_UNSET && pos >= 0) { val newIndPos1 = min(pos, individualDuration1.takeIf { it > 0 } ?: pos); if (individualCurrentPosition1 != newIndPos1) individualCurrentPosition1 = newIndPos1 } }
            exoPlayer2?.currentPosition?.let { pos -> if (pos != C.TIME_UNSET && pos >= 0) { val newIndPos2 = min(pos, individualDuration2.takeIf { it > 0 } ?: pos); if (individualCurrentPosition2 != newIndPos2) individualCurrentPosition2 = newIndPos2 } }
            delay(250L)
        }
    }

    DisposableEffect(Unit) { onDispose { exoPlayer1?.release(); exoPlayer1 = null; exoPlayer2?.release(); exoPlayer2 = null } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("動画解析") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ホームに戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors( // Optional: to style the TopAppBar
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp),       // Your original screen padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f)) // Pushes content below

            Box(modifier = Modifier.weight(3f).fillMaxWidth()) { // Video players area
                if (isHorizontalLayout) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoPlayerBox(exoPlayer1, videoAspectRatio1, "Video 1", individualCurrentPosition1, individualDuration1, onClick = { videoPlayerTargetForDialog = 1; showVideoSourceDialog = true }, modifier = Modifier.weight(1f).fillMaxHeight())
                        VideoPlayerBox(exoPlayer2, videoAspectRatio2, "Video 2", individualCurrentPosition2, individualDuration2, onClick = { videoPlayerTargetForDialog = 2; showVideoSourceDialog = true }, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                } else { // Vertical layout
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoPlayerBox(exoPlayer1, videoAspectRatio1, "Video 1", individualCurrentPosition1, individualDuration1, onClick = { videoPlayerTargetForDialog = 1; showVideoSourceDialog = true }, modifier = Modifier.weight(1f).fillMaxWidth())
                        VideoPlayerBox(exoPlayer2, videoAspectRatio2, "Video 2", individualCurrentPosition2, individualDuration2, onClick = { videoPlayerTargetForDialog = 2; showVideoSourceDialog = true }, modifier = Modifier.weight(1f).fillMaxWidth())
                    }
                }
            }

            // Shared Playback Controls & Layout Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(64.dp)) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (isPlaying) "Pause" else "Play", modifier = Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { isHorizontalLayout = !isHorizontalLayout }, modifier = Modifier.size(56.dp)) {
                    Icon(if (isHorizontalLayout) Icons.Filled.SwapVert else Icons.Filled.SwapHoriz, if (isHorizontalLayout) "Vertical" else "Horizontal", modifier = Modifier.fillMaxSize(), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Shared Progress Bar
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(formatTime(currentPosition), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = if (maxDuration > 0) currentPosition.toFloat() / maxDuration.toFloat() else 0f,
                    onValueChange = { newValue -> isSeeking = true; currentPosition = (newValue * maxDuration).toLong() },
                    onValueChangeFinished = {
                        exoPlayer1?.seekTo(min(currentPosition, individualDuration1.takeIf { it > 0 } ?: currentPosition))
                        exoPlayer2?.seekTo(min(currentPosition, individualDuration2.takeIf { it > 0 } ?: currentPosition))
                        isSeeking = false
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(formatTime(maxDuration), style = MaterialTheme.typography.bodySmall)
            }
            if (isSeeking || (isPlaying && (exoPlayer1?.isLoading == true || exoPlayer2?.isLoading == true))) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }

            // "選手一覧へ" Button is removed.
            // If you need other buttons here later, they can be added.
            // For now, this section is empty.
            Spacer(modifier = Modifier.height(24.dp)) // Add some space at the bottom if needed or remove if not.

        }
    }

    if (showVideoSourceDialog && videoPlayerTargetForDialog != 0) {
        val currentLocalContext = LocalContext.current
        VideoSourceChooserDialog(
            onDismissRequest = { showVideoSourceDialog = false; videoPlayerTargetForDialog = 0 },
            onTakeVideoClick = {
                launchCameraAction(currentLocalContext, videoPlayerTargetForDialog, takeVideoLauncher1, requestCameraPermissionLauncher1, takeVideoLauncher2, requestCameraPermissionLauncher2)
            },
            onSelectVideoClick = {
                launchGalleryAction(currentLocalContext, videoPlayerTargetForDialog, selectVideoLauncher1, requestStoragePermissionLauncher1, selectVideoLauncher2, requestStoragePermissionLauncher2)
            }
        )
    }
}
