package com.example.swimminganalysisapplication.ui.video

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause // Ensure this is imported
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz // For layout toggle
import androidx.compose.material.icons.filled.SwapVert  // For layout toggle
// import androidx.compose.material.icons.filled.Stop // Ensured removed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

private const val TAG = "VideoScreen"

// Helper function to format time from milliseconds to MM:SS
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
    currentPosition: Long, // New parameter
    duration: Long,        // New parameter
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(videoAspectRatio ?: 16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false // Keep controllers hidden
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            // Individual Simple Progress Bar
            if (duration > 0L) { // Only show if duration is valid
                LinearProgressIndicator(
                    progress = { currentPosition.toFloat() / duration.toFloat() }, // Updated for new ProgressIndicator API
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp) // Slightly thicker for better visibility
                        .padding(horizontal = 2.dp, vertical = 2.dp), // Minimal padding
                    color = MaterialTheme.colorScheme.tertiary, // Different color from shared slider
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // More subtle track
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$videoName Not Loaded", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


@Composable
fun VideoScreen(navController: NavController) {
    val context = LocalContext.current

    // Video 1 States
    var videoUri1 by remember { mutableStateOf<Uri?>(null) }
    var exoPlayer1 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio1 by remember { mutableStateOf<Float?>(null) }
    var individualCurrentPosition1 by remember { mutableStateOf(0L) }
    var individualDuration1 by remember { mutableStateOf(0L) }

    // Video 2 States
    var videoUri2 by remember { mutableStateOf<Uri?>(null) }
    var exoPlayer2 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio2 by remember { mutableStateOf<Float?>(null) }
    var individualCurrentPosition2 by remember { mutableStateOf(0L) }
    var individualDuration2 by remember { mutableStateOf(0L) }

    // Shared playback state
    var isPlaying by remember { mutableStateOf(false) }
    var isHorizontalLayout by remember { mutableStateOf(true) } // true for Row (Horizontal), false for Column (Vertical)


    // Shared progress bar states
    var maxDuration by remember { mutableStateOf(0L) }
    var currentPosition by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }

    val updateIndividualDurations = {
        individualDuration1 = exoPlayer1?.duration?.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
        individualDuration2 = exoPlayer2?.duration?.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
    }

    val updateSharedMaxDuration = {
        updateIndividualDurations()
        val d1Millis = individualDuration1
        val d2Millis = individualDuration2
        val newMax = max(d1Millis, d2Millis)

        if (maxDuration != newMax) {
            maxDuration = newMax
            Log.d(TAG, "Max duration updated: ${formatTime(maxDuration)}")
            if (currentPosition > maxDuration && maxDuration > 0L) {
                currentPosition = maxDuration
            } else if (maxDuration == 0L) {
                currentPosition = 0L
            }
        }
    }

    // --- Launchers ---
    val requestCameraPermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* ... */ }
    val requestStoragePermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* ... */ }
    val takeVideoLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let { videoUri1 = it } }
    val selectVideoLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> videoUri1 = uri }
    val requestCameraPermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* ... */ }
    val requestStoragePermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* ... */ }
    val takeVideoLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let { videoUri2 = it } }
    val selectVideoLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> videoUri2 = uri }

    // ExoPlayer 1 Lifecycle
    LaunchedEffect(videoUri1) {
        exoPlayer1?.release()
        exoPlayer1 = null
        individualCurrentPosition1 = 0L
        individualDuration1 = 0L
        if (videoUri1 != null) {
            exoPlayer1 = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri1!!))
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) { if (videoSize.width > 0 && videoSize.height > 0) videoAspectRatio1 = videoSize.width.toFloat() / videoSize.height.toFloat() }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            updateSharedMaxDuration()
                        } else if (playbackState == Player.STATE_ENDED) {
                            individualCurrentPosition1 = individualDuration1
                            if (exoPlayer2 == null || exoPlayer2?.playbackState == Player.STATE_ENDED) {
                                if(isPlaying) isPlaying = false
                            }
                            Log.d(TAG, "Player 1 ended. isPlaying: $isPlaying")
                        }
                    }
                    override fun onTimelineChanged(timeline: Timeline, reason: Int) { updateSharedMaxDuration() }
                })
                prepare()
            }
            updateSharedMaxDuration()
        } else {
            videoAspectRatio1 = null
            updateSharedMaxDuration()
            if (exoPlayer2 == null) currentPosition = 0L
        }
    }

    // ExoPlayer 2 Lifecycle
    LaunchedEffect(videoUri2) {
        exoPlayer2?.release()
        exoPlayer2 = null
        individualCurrentPosition2 = 0L
        individualDuration2 = 0L
        if (videoUri2 != null) {
            exoPlayer2 = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUri2!!))
                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) { if (videoSize.width > 0 && videoSize.height > 0) videoAspectRatio2 = videoSize.width.toFloat() / videoSize.height.toFloat() }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            updateSharedMaxDuration()
                        } else if (playbackState == Player.STATE_ENDED) {
                            individualCurrentPosition2 = individualDuration2
                            if (exoPlayer1 == null || exoPlayer1?.playbackState == Player.STATE_ENDED) {
                                if(isPlaying) isPlaying = false
                            }
                            Log.d(TAG, "Player 2 ended. isPlaying: $isPlaying")
                        }
                    }
                    override fun onTimelineChanged(timeline: Timeline, reason: Int) { updateSharedMaxDuration() }
                })
                prepare()
            }
            updateSharedMaxDuration()
        } else {
            videoAspectRatio2 = null
            updateSharedMaxDuration()
            if (exoPlayer1 == null) currentPosition = 0L
        }
    }

    // Effect to control playback based on isPlaying state (Reacts primarily to isPlaying changes)
    LaunchedEffect(isPlaying, exoPlayer1, exoPlayer2) { // currentPosition is REMOVED from keys
        if (isPlaying) {
            Log.d(TAG, "COMMAND: PLAY from ${formatTime(currentPosition)}")
            exoPlayer1?.let { player ->
                val playerDuration = individualDuration1.takeIf { it > 0L } ?: player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
                val targetSeekPosition = min(currentPosition, playerDuration)
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.seekTo(targetSeekPosition)
                if (targetSeekPosition < playerDuration || (playerDuration == 0L && targetSeekPosition == 0L)) player.play() else player.pause()
            }
            exoPlayer2?.let { player ->
                val playerDuration = individualDuration2.takeIf { it > 0L } ?: player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
                val targetSeekPosition = min(currentPosition, playerDuration)
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.seekTo(targetSeekPosition)
                if (targetSeekPosition < playerDuration || (playerDuration == 0L && targetSeekPosition == 0L)) player.play() else player.pause()
            }
        } else {
            Log.d(TAG, "COMMAND: PAUSE. Exo1: ${exoPlayer1 != null}, Exo2: ${exoPlayer2 != null}")
            exoPlayer1?.pause()
            exoPlayer2?.pause()
        }
    }

    // Effect to update currentPosition periodically (Polling)
    LaunchedEffect(Unit) {
        while (true) {
            if (isPlaying && !isSeeking) {
                val activePlayerForSharedAdvance = run {
                    val p1 = exoPlayer1; val d1 = individualDuration1
                    val p2 = exoPlayer2; val d2 = individualDuration2
                    fun canDrive(player: Player?, duration: Long): Boolean = player != null && player.playbackState != Player.STATE_ENDED && (player.isPlaying || (duration > 0L && player.currentPosition < duration))
                    when {
                        canDrive(p1, d1) -> p1
                        canDrive(p2, d2) -> p2
                        else -> if (p1 != null && p1.playbackState != Player.STATE_ENDED) p1 else if (p2 != null && p2.playbackState != Player.STATE_ENDED) p2 else p1 ?: p2
                    }
                }
                activePlayerForSharedAdvance?.currentPosition?.let { pos ->
                    if (pos != C.TIME_UNSET && pos >= 0) {
                        val newCurrentPosition = if (maxDuration > 0L) min(pos, maxDuration) else pos
                        if (currentPosition != newCurrentPosition) currentPosition = newCurrentPosition
                    }
                }
            }
            exoPlayer1?.currentPosition?.let { pos -> if (pos != C.TIME_UNSET && pos >= 0) { val newIndPos1 = min(pos, individualDuration1.takeIf { it > 0 } ?: pos); if (individualCurrentPosition1 != newIndPos1) individualCurrentPosition1 = newIndPos1 } }
            exoPlayer2?.currentPosition?.let { pos -> if (pos != C.TIME_UNSET && pos >= 0) { val newIndPos2 = min(pos, individualDuration2.takeIf { it > 0 } ?: pos); if (individualCurrentPosition2 != newIndPos2) individualCurrentPosition2 = newIndPos2 } }
            delay(250L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer1?.release(); exoPlayer1 = null
            exoPlayer2?.release(); exoPlayer2 = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f)) // Pushes all content below it to the bottom part of the screen

        // Video Players Area
        Box(modifier = Modifier.weight(3f).fillMaxWidth()) { // Allocate significant space for videos
            if (isHorizontalLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoPlayerBox(
                        exoPlayer = exoPlayer1,
                        videoAspectRatio = videoAspectRatio1,
                        videoName = "Video 1",
                        currentPosition = individualCurrentPosition1, // << ADD THIS
                        duration = individualDuration1,             // << ADD THIS
                        modifier = Modifier.weight(1f)
                    )
                    VideoPlayerBox(
                        exoPlayer = exoPlayer2,
                        videoAspectRatio = videoAspectRatio2,
                        videoName = "Video 2",
                        currentPosition = individualCurrentPosition2, // << ADD THIS
                        duration = individualDuration2,             // << ADD THIS
                        modifier = Modifier.weight(1f)
                    )
                }
            } else { // Vertical layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoPlayerBox(
                        exoPlayer = exoPlayer1,
                        videoAspectRatio = videoAspectRatio1,
                        videoName = "Video 1",
                        currentPosition = individualCurrentPosition1, // << ADD THIS
                        duration = individualDuration1,             // << ADD THIS
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    VideoPlayerBox(
                        exoPlayer = exoPlayer2,
                        videoAspectRatio = videoAspectRatio2,
                        videoName = "Video 2",
                        currentPosition = individualCurrentPosition2, // << ADD THIS
                        duration = individualDuration2,             // << ADD THIS
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }

        // Shared Playback Controls & Layout Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = { isHorizontalLayout = !isHorizontalLayout },
                modifier = Modifier.size(56.dp) // Slightly smaller than play/pause
            ) {
                Icon(
                    imageVector = if (isHorizontalLayout) Icons.Filled.SwapVert else Icons.Filled.SwapHoriz,
                    contentDescription = if (isHorizontalLayout) "Switch to Vertical Layout" else "Switch to Horizontal Layout",
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Shared Progress Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            Slider(
                value = if (maxDuration > 0) currentPosition.toFloat() / maxDuration.toFloat() else 0f,
                onValueChange = { newValue ->
                    isSeeking = true
                    currentPosition = (newValue * maxDuration).toLong()
                },
                onValueChangeFinished = {
                    Log.d(TAG, "Slider - onValueChangeFinished. Seeking to: ${formatTime(currentPosition)}")
                    exoPlayer1?.seekTo(min(currentPosition, individualDuration1.takeIf { it > 0 } ?: currentPosition))
                    exoPlayer2?.seekTo(min(currentPosition, individualDuration2.takeIf { it > 0 } ?: currentPosition))
                    isSeeking = false
                    // If was playing, resume playback after seek. If paused, remains paused.
                    // The LaunchedEffect(isPlaying, ...) will handle playing if isPlaying is true.
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(formatTime(maxDuration), style = MaterialTheme.typography.bodySmall)
        }
        if (isSeeking || (isPlaying && (exoPlayer1?.isLoading == true || exoPlayer2?.isLoading == true))) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }


        // Video 1 Controls
        Text("Video 1 Controls", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Button(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    if (takeVideoIntent.resolveActivity(context.packageManager) != null) {
                        takeVideoLauncher1.launch(takeVideoIntent)
                    }
                } else {
                    requestCameraPermissionLauncher1.launch(Manifest.permission.CAMERA)
                }
            }) { Text("Take Video 1") }
            Button(onClick = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    selectVideoLauncher1.launch("video/*")
                } else {
                    requestStoragePermissionLauncher1.launch(permission)
                }
            }) { Text("Select Video 1") }
        }
        Text("Pos: ${formatTime(individualCurrentPosition1)} / Dur: ${formatTime(individualDuration1)}", style = MaterialTheme.typography.bodySmall)


        // Video 2 Controls
        Text("Video 2 Controls", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Button(onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    if (takeVideoIntent.resolveActivity(context.packageManager) != null) {
                        takeVideoLauncher2.launch(takeVideoIntent)
                    }
                } else {
                    requestCameraPermissionLauncher2.launch(Manifest.permission.CAMERA)
                }
            }) { Text("Take Video 2") }
            Button(onClick = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    selectVideoLauncher2.launch("video/*")
                } else {
                    requestStoragePermissionLauncher2.launch(permission)
                }
            }) { Text("Select Video 2") }
        }
        Text("Pos: ${formatTime(individualCurrentPosition2)} / Dur: ${formatTime(individualDuration2)}", style = MaterialTheme.typography.bodySmall)


        Button(
            onClick = { navController.navigate("athleteList") },
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth()
        ) {
            Text("選手一覧へ")
        }
    }
}
