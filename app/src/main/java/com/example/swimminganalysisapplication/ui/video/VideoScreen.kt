package com.example.swimminganalysisapplication.ui.video

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BorderColor
import com.example.swimminganalysisapplication.util.AppLog
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.storage.AppDatabase
import com.example.swimminganalysisapplication.data.storage.ProjectEntity
import com.example.swimminganalysisapplication.data.storage.ProjectRepository
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import com.example.swimminganalysisapplication.ui.theme.CustomTopAppBarHeight
import com.example.swimminganalysisapplication.ui.theme.getCustomTopAppBarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TAG = "VideoScreen"
private const val SEEK_COMMAND_THRESHOLD_MS = 200 // Threshold to avoid tiny seeks

enum class VideoLayoutMode {
    VERTICAL,
    HORIZONTAL,
    OVERLAY
}

// Saver for Uri type for rememberSaveable
val UriSaver = Saver<Uri?, String>(
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
    isDrawingMode: Boolean,
    onIsDrawingModeChange: (Boolean) -> Unit,
    drawMode: DrawMode,
    onDrawModeChange: (DrawMode) -> Unit,
    layoutMode: VideoLayoutMode,
    onClearLines: () -> Unit,
    lineDrawingView: LineDrawingView,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .clickable(onClick = onClick, enabled = !isDrawingMode)
                .border(
                    width = if (isDrawingMode) 2.dp else 0.dp,
                    color = if (isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                )
                .clip(RectangleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoAspectRatio ?: 16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (exoPlayer != null) {
                    AndroidView(
                        factory = { context ->
                            TextureView(context)
                        },
                        update = { textureView ->
                            exoPlayer.setVideoTextureView(textureView)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    AndroidView(
                        factory = { lineDrawingView },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            view.setDrawingEnabled(isDrawingMode)
                            view.setDrawMode(drawMode)
                            view.setVideoAspectRatio(videoAspectRatio)
                        }
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
        // Place controls outside the video box so they don't overlap the video content
        if (exoPlayer != null) {
            Spacer(modifier = Modifier.height(6.dp))
            DrawingControls(
                layoutMode = layoutMode,
                isDrawingMode = isDrawingMode,
                onIsDrawingModeChange = onIsDrawingModeChange,
                currentMode = drawMode,
                onDrawModeChange = onDrawModeChange,
                onClear = { lineDrawingView.clearCanvas() }, // Call clear on the specific instance
                onDeleteSelected = { lineDrawingView.deleteSelectedShape() }
            )
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
        else AppLog.e(TAG, "No activity for ACTION_VIDEO_CAPTURE video $videoIndex")
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
fun VideoScreen(navController: NavController, projectId: Int? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Get a CoroutineScope

    var videoUri1 by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var exoPlayer1 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio1 by remember { mutableStateOf<Float?>(null) }
    var originalDuration1Ms by remember { mutableStateOf(0L) }
    var startPosition1Ms by rememberSaveable { mutableStateOf(0L) }
    var alignmentPoint1Ms by rememberSaveable { mutableStateOf(0L) }
    var isDrawingMode1 by rememberSaveable { mutableStateOf(false) }
    var drawMode1 by remember { mutableStateOf(DrawMode.FREE) }

    var videoUri2 by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
    var exoPlayer2 by remember { mutableStateOf<ExoPlayer?>(null) }
    var videoAspectRatio2 by remember { mutableStateOf<Float?>(null) }
    var originalDuration2Ms by remember { mutableStateOf(0L) }
    var startPosition2Ms by rememberSaveable { mutableStateOf(0L) }
    var alignmentPoint2Ms by rememberSaveable { mutableStateOf(0L) }
    var isDrawingMode2 by rememberSaveable { mutableStateOf(false) }
    var drawMode2 by remember { mutableStateOf(DrawMode.FREE) }

    // 共通のオフセット値（個別に管理する必要はない）
    var offsetMs by rememberSaveable { mutableStateOf(0L) }

    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var layoutMode by rememberSaveable { mutableStateOf(VideoLayoutMode.HORIZONTAL) }
    var overlayAlpha1 by rememberSaveable { mutableStateOf(1.0f) }
    var overlayAlpha2 by rememberSaveable { mutableStateOf(0.5f) }

    var sharedCurrentPositionMs by rememberSaveable { mutableStateOf(0L) }
    var sharedMaxDurationMs by rememberSaveable { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) } // Used to prevent position updates during seek

    var showVideoSourceDialog by remember { mutableStateOf(false) }
    var videoPlayerTargetForDialog by remember { mutableStateOf(0) }
    
    // Save dialog state
    var showSaveDialog by remember { mutableStateOf(false) }
    var projectNameForSave by remember { mutableStateOf("") }
    var loadedProject by remember { mutableStateOf<com.example.swimminganalysisapplication.data.storage.ProjectEntity?>(null) }
    
    // ON_RESUME trigger for reloading project data
    var onResumeTrigger by remember { mutableStateOf(0) }
    // Flag to prevent DB reload when new start positions are applied from start position setting screen
    var hasNewStartPositions by remember { mutableStateOf(false) }
    
    // Loading state - true until both players are ready and all data is loaded
    var isLoading by remember { mutableStateOf(projectId != null) }  // Start loading if projectId is provided
    
    val lineDrawingView1 = remember { LineDrawingView(context).apply { setStrokeColor(android.graphics.Color.RED) } }
    val lineDrawingView2 = remember { LineDrawingView(context).apply { setStrokeColor(android.graphics.Color.BLUE) } }

    // Load project if projectId is provided
    LaunchedEffect(projectId) {
        if (projectId != null) {
            AppLog.d(TAG, "LaunchedEffect(projectId): Starting project load, projectId=$projectId")
            val db = AppDatabase.getDatabase(context)
            val repository = ProjectRepository(db.projectDao())
            val project = repository.getProjectById(projectId)
            if (project != null) {
                AppLog.d(TAG, "LaunchedEffect(projectId): Loaded project: name=${project.name}")
                AppLog.d(TAG, "LaunchedEffect(projectId): align1=${project.alignmentPoint1Ms}ms, offset=${project.offsetMs}ms, align2=${project.alignmentPoint2Ms}ms")
                AppLog.d(TAG, "LaunchedEffect(projectId): startPos1=${project.startPosition1Ms}ms, startPos2=${project.startPosition2Ms}ms (from DB)")
                videoUri1 = Uri.parse(project.videoUri1)
                videoUri2 = Uri.parse(project.videoUri2)
                // Restore alignment points and common offset
                alignmentPoint1Ms = project.alignmentPoint1Ms
                alignmentPoint2Ms = project.alignmentPoint2Ms
                offsetMs = project.offsetMs
                // Use startPosition from DB directly (don't recalculate)
                startPosition1Ms = project.startPosition1Ms
                startPosition2Ms = project.startPosition2Ms
                AppLog.d(TAG, "LaunchedEffect(projectId): Using startPos1=$startPosition1Ms, startPos2=$startPosition2Ms from DB")
                // remember loaded project for potential overwrite
                loadedProject = project
                // prefill save dialog name
                projectNameForSave = project.name
                
                // Deserialize and restore drawings
                val drawingsData = project.drawingsJson
                val video1Drawings = DrawingSerializer.deserializeShapesFromJson(drawingsData, "video1")
                val video2Drawings = DrawingSerializer.deserializeShapesFromJson(drawingsData, "video2")
                lineDrawingView1.setShapes(video1Drawings)
                lineDrawingView2.setShapes(video2Drawings)
                AppLog.d(TAG, "LaunchedEffect(projectId): Project load complete")
            } else {
                AppLog.d(TAG, "LaunchedEffect(projectId): Project not found")
            }
        }
    }

    val currentBackStackEntry = navController.currentBackStackEntry
    DisposableEffect(currentBackStackEntry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                AppLog.d(TAG, "ON_RESUME: videoUri1 = $videoUri1, videoUri2 = $videoUri2, startPos1=$startPosition1Ms, startPos2=$startPosition2Ms")
                
                // Trigger LaunchedEffect to reload project data
                onResumeTrigger++
                
                if (currentBackStackEntry?.savedStateHandle?.contains("newStart1Ms") == true &&
                    currentBackStackEntry.savedStateHandle.contains("newStart2Ms") == true) {
                    AppLog.d(TAG, "ON_RESUME: Found new start positions in savedStateHandle")
                    val newStart1 = currentBackStackEntry.savedStateHandle.get<Long>("newStart1Ms") ?: startPosition1Ms
                    val newStart2 = currentBackStackEntry.savedStateHandle.get<Long>("newStart2Ms") ?: startPosition2Ms
                    
                    // Restore alignment points and common offset if available
                    val newAlign1 = currentBackStackEntry.savedStateHandle.get<Long>("alignmentPoint1Ms")
                    val newAlign2 = currentBackStackEntry.savedStateHandle.get<Long>("alignmentPoint2Ms")
                    val newOffset = currentBackStackEntry.savedStateHandle.get<Long>("offset1Ms") // offset1Ms contains common offset
                    
                    if (newAlign1 != null && newAlign2 != null && newOffset != null) {
                        alignmentPoint1Ms = newAlign1
                        alignmentPoint2Ms = newAlign2
                        offsetMs = newOffset
                        AppLog.d(TAG, "ON_RESUME: Restored alignment points and offset: align1=$alignmentPoint1Ms, offset=$offsetMs, align2=$alignmentPoint2Ms")
                    }

                    startPosition1Ms = min(newStart1, (originalDuration1Ms - 1L).coerceAtLeast(0L))
                    startPosition2Ms = min(newStart2, (originalDuration2Ms - 1L).coerceAtLeast(0L))
                    AppLog.d(TAG, "ON_RESUME: Applied new startPos1=$startPosition1Ms, new startPos2=$startPosition2Ms")

                    currentBackStackEntry.savedStateHandle.remove<Long>("newStart1Ms")
                    currentBackStackEntry.savedStateHandle.remove<Long>("newStart2Ms")
                    currentBackStackEntry.savedStateHandle.remove<Long>("alignmentPoint1Ms")
                    currentBackStackEntry.savedStateHandle.remove<Long>("alignmentPoint2Ms")
                    currentBackStackEntry.savedStateHandle.remove<Long>("offset1Ms")
                    currentBackStackEntry.savedStateHandle.remove<Long>("offset2Ms")

                    sharedCurrentPositionMs = 0L
                    isPlaying = false
                    // Mark that new start positions have been applied (don't reload from DB)
                    hasNewStartPositions = true
                }
                
                // Restore temporary drawings if they exist
                val tempDrawings = currentBackStackEntry?.savedStateHandle?.get<String>("tempDrawings")
                if (tempDrawings != null) {
                    AppLog.d(TAG, "ON_RESUME: Restoring temporary drawings")
                    val video1Drawings = DrawingSerializer.deserializeShapesFromJson(tempDrawings, "video1")
                    val video2Drawings = DrawingSerializer.deserializeShapesFromJson(tempDrawings, "video2")
                    lineDrawingView1.setShapes(video1Drawings)
                    lineDrawingView2.setShapes(video2Drawings)
                    
                    currentBackStackEntry?.savedStateHandle?.remove<String>("tempDrawings")
                }
            }
        }
        currentBackStackEntry?.lifecycle?.addObserver(observer)
        onDispose { currentBackStackEntry?.lifecycle?.removeObserver(observer) }
    }
    
    // Reload project data from DB when resuming (for latest alignment points and offset)
    // But only if no new start positions were set from the start position setting screen
    LaunchedEffect(onResumeTrigger) {
        if (projectId != null && loadedProject != null && onResumeTrigger > 0 && !hasNewStartPositions) {
            val db = AppDatabase.getDatabase(context)
            val repository = ProjectRepository(db.projectDao())
            val latestProject = repository.getProjectById(projectId)
            if (latestProject != null) {
                AppLog.d(TAG, "LaunchedEffect: Reloading project from DB: align1=${latestProject.alignmentPoint1Ms}, offset=${latestProject.offsetMs}, align2=${latestProject.alignmentPoint2Ms}")
                alignmentPoint1Ms = latestProject.alignmentPoint1Ms
                alignmentPoint2Ms = latestProject.alignmentPoint2Ms
                offsetMs = latestProject.offsetMs
                // Use startPosition from DB directly (don't recalculate)
                startPosition1Ms = latestProject.startPosition1Ms
                startPosition2Ms = latestProject.startPosition2Ms
                AppLog.d(TAG, "LaunchedEffect: Using startPos1=${latestProject.startPosition1Ms}, startPos2=${latestProject.startPosition2Ms} from DB")
                loadedProject = latestProject
            }
        } else if (hasNewStartPositions) {
            AppLog.d(TAG, "LaunchedEffect: Skipping DB reload because new start positions were applied from start position setting screen")
            // Reset flag for next time
            hasNewStartPositions = false
        }
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
            AppLog.d(TAG, "updateSharedMaxDuration: OldMax=$sharedMaxDurationMs, NewMax=$newMax. currentSharedPos=$sharedCurrentPositionMs")
            AppLog.d(TAG, "updateSharedMaxDuration: exoPlayer1=$exoPlayer1, dur1=$originalDuration1Ms, start1=$startPosition1Ms, trim1=$trimmedDuration1")
            AppLog.d(TAG, "updateSharedMaxDuration: exoPlayer2=$exoPlayer2, dur2=$originalDuration2Ms, start2=$startPosition2Ms, trim2=$trimmedDuration2")
            sharedMaxDurationMs = newMax
            sharedCurrentPositionMs = sharedCurrentPositionMs.coerceIn(0L, newMax) // Ensure current position is within new max
        }
    }
    LaunchedEffect(startPosition1Ms, startPosition2Ms, originalDuration1Ms, originalDuration2Ms, exoPlayer1, exoPlayer2) {
        AppLog.d(TAG, "LaunchedEffect to updateSharedMaxDuration triggered: startPos1=$startPosition1Ms, startPos2=$startPosition2Ms, dur1=$originalDuration1Ms, dur2=$originalDuration2Ms, player1=$exoPlayer1, player2=$exoPlayer2")
        updateSharedMaxDuration()
        
        // Check if loading is complete
        // Loading is complete when:
        // 1. projectId was provided (or no project needed)
        // 2. Both players are initialized AND durations are known OR only one video is available with duration
        val isProjectLoaded = projectId == null || loadedProject != null
        val areBothPlayersReady = (videoUri1 != null && exoPlayer1 != null && originalDuration1Ms > 0) ||
                                  (videoUri1 == null)
        val areBothPlayersReady2 = (videoUri2 != null && exoPlayer2 != null && originalDuration2Ms > 0) ||
                                   (videoUri2 == null)
        val isPlaybackReady = (videoUri1 != null || videoUri2 != null) && // At least one video
                              areBothPlayersReady && areBothPlayersReady2 && // Both available videos are ready
                              sharedMaxDurationMs > 0 // Duration is calculated
        
        if (isProjectLoaded && isPlaybackReady && isLoading) {
            AppLog.d(TAG, "LaunchedEffect: Loading complete. Hiding loading screen.")
            isLoading = false
        }
    }

    val takeVideoLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                AppLog.d(TAG, "takeVideoLauncher1: URI obtained: $uri")
                videoUri1 = uri; startPosition1Ms = 0L
                try { if ("content" == uri.scheme) context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); AppLog.d(TAG, "Persisted URI for captured video 1: $uri") }
                catch (e: SecurityException) { AppLog.e(TAG, "Failed to persist URI for captured video 1: $uri", e) }
            }
        }
    }
    val selectVideoLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            AppLog.d(TAG, "selectVideoLauncher1: URI obtained: $it")
            videoUri1 = it; startPosition1Ms = 0L
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); AppLog.d(TAG, "Persisted URI for selected video 1: $it") }
            catch (e: SecurityException) { AppLog.e(TAG, "Failed to persist URI for selected video 1: $it", e) }
        }
    }
    val takeVideoLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                AppLog.d(TAG, "takeVideoLauncher2: URI obtained: $uri")
                videoUri2 = uri; startPosition2Ms = 0L
                try { if ("content" == uri.scheme) context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); AppLog.d(TAG, "Persisted URI for captured video 2: $uri") }
                catch (e: SecurityException) { AppLog.e(TAG, "Failed to persist URI for captured video 2: $uri", e) }
            }
        }
    }
    val selectVideoLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            AppLog.d(TAG, "selectVideoLauncher2: URI obtained: $it")
            videoUri2 = it; startPosition2Ms = 0L
            try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); AppLog.d(TAG, "Persisted URI for selected video 2: $it") }
            catch (e: SecurityException) { AppLog.e(TAG, "Failed to persist URI for selected video 2: $it", e) }
        }
    }

    val requestCameraPermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            if (takeVideoIntent.resolveActivity(context.packageManager) != null) takeVideoLauncher1.launch(takeVideoIntent)
            else AppLog.e(TAG, "No activity for ACTION_VIDEO_CAPTURE video 1")
        }
        else AppLog.w(TAG, "Camera permission denied for video 1")
    }
    val requestStoragePermissionLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) selectVideoLauncher1.launch("video/*")
        else AppLog.w(TAG, "Storage permission denied for video 1")
    }
    val requestCameraPermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            if (takeVideoIntent.resolveActivity(context.packageManager) != null) takeVideoLauncher2.launch(takeVideoIntent)
            else AppLog.e(TAG, "No activity for ACTION_VIDEO_CAPTURE video 2")
        }
        else AppLog.w(TAG, "Camera permission denied for video 2")
    }
    val requestStoragePermissionLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) selectVideoLauncher2.launch("video/*")
        else AppLog.w(TAG, "Storage permission denied for video 2")
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
                    AppLog.d(TAG, "Video size changed: ${videoSize.width}x${videoSize.height}, AspectRatio: $aspectRatio")
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (timeline.windowCount > 0) {
                        val window = Timeline.Window()
                        timeline.getWindow(0, window)
                        val duration = window.durationMs
                        if (duration != C.TIME_UNSET && duration > 0) {
                            setOriginalDuration(duration)
                            AppLog.d(TAG, "Timeline changed. Original duration: $duration ms for player of $uri")
                            updateSharedMaxDuration()
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                    AppLog.d(TAG, "ExoPlayer (uri: $uri) onIsPlayingChanged: $isPlayingChange. Current composable isPlaying: $isPlaying")
                }
            })
        }
        setPlayer(newPlayer)
        val mediaItem = MediaItem.fromUri(uri)
        newPlayer.setMediaItem(mediaItem)
        newPlayer.prepare()
        newPlayer.playWhenReady = false
        newPlayer.seekTo(startPosMs)
        AppLog.d(TAG, "Player initialized/updated for URI: $uri, seeking to $startPosMs ms. playWhenReady initially false.")
    }

    // レイアウトモード変更時に編集モードを終了
    LaunchedEffect(layoutMode) {
        isDrawingMode1 = false
        isDrawingMode2 = false
        lineDrawingView1.clearSelection()
        lineDrawingView2.clearSelection()
    }

    // 描画モード変更時にLineDrawingViewの状態を更新
    LaunchedEffect(isDrawingMode1, drawMode1) {
        if (isDrawingMode1) {
            AppLog.d("VideoScreen", "Drawing mode ON for video1, mode=$drawMode1")
            lineDrawingView1.setDrawingEnabled(true)
            lineDrawingView1.setDrawMode(drawMode1)
        } else {
            AppLog.d("VideoScreen", "Drawing mode OFF for video1")
            lineDrawingView1.setDrawingEnabled(false)
        }
    }
    
    LaunchedEffect(isDrawingMode2, drawMode2) {
        if (isDrawingMode2) {
            lineDrawingView2.setDrawingEnabled(true)
            lineDrawingView2.setDrawMode(drawMode2)
        } else {
            lineDrawingView2.setDrawingEnabled(false)
        }
    }
    
    // 描画モード中はズーム情報を更新しない
    // Zoom 変更時の更新は削除（描画モード中のズーム操作は無効化される）

    DisposableEffect(videoUri1, exoPlayer1) {
        if (videoUri1 != null && exoPlayer1 == null) {
            AppLog.d(TAG, "DisposableEffect(videoUri1): Initializing player1 with startPosition=$startPosition1Ms")
            initializeOrUpdatePlayer(null, videoUri1, startPosition1Ms, { exoPlayer1 = it }, { videoAspectRatio1 = it }, { originalDuration1Ms = it })
        }
        onDispose {
            if (videoUri1 == null) {
                exoPlayer1?.release()
                exoPlayer1 = null
                AppLog.d(TAG, "Disposed and released exoPlayer1 because videoUri1 became null.")
            }
        }
    }
    DisposableEffect(videoUri2, exoPlayer2) {
        if (videoUri2 != null && exoPlayer2 == null) {
            AppLog.d(TAG, "DisposableEffect(videoUri2): Initializing player2 with startPosition=$startPosition2Ms")
            initializeOrUpdatePlayer(null, videoUri2, startPosition2Ms, { exoPlayer2 = it }, { videoAspectRatio2 = it }, { originalDuration2Ms = it })
        }
        onDispose {
            if (videoUri2 == null) {
                exoPlayer2?.release()
                exoPlayer2 = null
                AppLog.d(TAG, "Disposed and released exoPlayer2 because videoUri2 became null.")
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AppLog.d(TAG, "VideoScreen onDispose: Releasing players.")
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
            AppLog.d(TAG, "LaunchedEffect: isPlaying is true. Advancing position. Player1.play() and Player2.play() called.")

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
                AppLog.d(TAG, "LaunchedEffect: Playback reached end. Setting isPlaying to false.")
            } else if (!isPlaying) {
                AppLog.d(TAG, "LaunchedEffect: isPlaying became false during loop. Pausing players.")
                exoPlayer1?.pause()
                exoPlayer2?.pause()
            }
        } else {
            AppLog.d(TAG, "LaunchedEffect: isPlaying is false. Pausing players.")
            exoPlayer1?.pause()
            exoPlayer2?.pause()
        }
    }

    val togglePlayPause = {
        if (exoPlayer1 != null || exoPlayer2 != null) {
            val newIsPlayingState = !isPlaying
            isPlaying = newIsPlayingState

            if (newIsPlayingState) {
                AppLog.d(TAG, "togglePlayPause: Set isPlaying to TRUE. Telling players to play.")
                if (sharedCurrentPositionMs >= sharedMaxDurationMs && sharedMaxDurationMs > 0) {
                    sharedCurrentPositionMs = 0L
                    exoPlayer1?.seekTo(startPosition1Ms)
                    exoPlayer2?.seekTo(startPosition2Ms)
                    AppLog.d(TAG, "Playback reset to start as it was at the end.")
                }
                exoPlayer1?.play()
                exoPlayer2?.play()
            } else {
                AppLog.d(TAG, "togglePlayPause: Set isPlaying to FALSE. Telling players to pause.")
                exoPlayer1?.pause()
                exoPlayer2?.pause()
            }
        }
    }

    val frameAdvance = { frameOffsetMs: Long ->
        if (exoPlayer1 != null || exoPlayer2 != null) {
            isPlaying = false
            exoPlayer1?.pause()
            exoPlayer2?.pause()
            
            val newPosition = (sharedCurrentPositionMs + frameOffsetMs).coerceIn(0, sharedMaxDurationMs)
            sharedCurrentPositionMs = newPosition
            
            val targetPos1 = (startPosition1Ms + newPosition).coerceIn(0, originalDuration1Ms)
            val targetPos2 = (startPosition2Ms + newPosition).coerceIn(0, originalDuration2Ms)
            
            exoPlayer1?.seekTo(targetPos1)
            exoPlayer2?.seekTo(targetPos2)
            AppLog.d(TAG, "Frame advance: moved to $newPosition ms")
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

            AppLog.d(TAG, "Seeked to: $sharedCurrentPositionMs ms (Player1: $targetPos1, Player2: $targetPos2)")

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
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    AccountActionsMenu(navController = navController)
                },
                colors = getCustomTopAppBarColors(),
                modifier = Modifier.heightIn(max = CustomTopAppBarHeight)
            )
        }
    ) { paddingValues ->
        if (isLoading && projectId != null) {
            // Show loading screen while project is being loaded
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("プロジェクトを読み込み中...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            // Show actual content when loaded
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            Box(modifier = Modifier.weight(1f)) {
                when (layoutMode) {
                    VideoLayoutMode.HORIZONTAL -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VideoPlayerBox(
                                exoPlayer = exoPlayer1, videoAspectRatio = videoAspectRatio1, videoName = "ビデオ1",
                                currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L)),
                                durationOfTrimmedView = (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L),
                                onClick = { if (!isDrawingMode1) { videoPlayerTargetForDialog = 1; showVideoSourceDialog = true } },
                                isDrawingMode = isDrawingMode1, onIsDrawingModeChange = { newMode ->
                                    isDrawingMode1 = newMode
                                    if (!newMode) lineDrawingView1.clearSelection()
                                },
                                drawMode = drawMode1, onDrawModeChange = { drawMode1 = it },
                                layoutMode = layoutMode,
                                onClearLines = { lineDrawingView1.clearCanvas() },
                                lineDrawingView = lineDrawingView1,
                                modifier = Modifier.weight(1f).padding(if (exoPlayer2 != null) PaddingValues(end = 2.dp) else PaddingValues())
                            )
                            if (exoPlayer1 != null && exoPlayer2 != null) Spacer(modifier = Modifier.width(4.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
                            VideoPlayerBox(
                                exoPlayer = exoPlayer2, videoAspectRatio = videoAspectRatio2, videoName = "ビデオ2",
                                currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L)),
                                durationOfTrimmedView = (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L),
                                onClick = { if (!isDrawingMode2) { videoPlayerTargetForDialog = 2; showVideoSourceDialog = true } },
                                isDrawingMode = isDrawingMode2, onIsDrawingModeChange = { newMode ->
                                    isDrawingMode2 = newMode
                                    if (!newMode) lineDrawingView2.clearSelection()
                                },
                                drawMode = drawMode2, onDrawModeChange = { drawMode2 = it },
                                layoutMode = layoutMode,
                                onClearLines = { lineDrawingView2.clearCanvas() },
                                lineDrawingView = lineDrawingView2,
                                modifier = Modifier.weight(1f).padding(if (exoPlayer1 != null) PaddingValues(start = 2.dp) else PaddingValues())
                            )
                        }
                    }
                    VideoLayoutMode.VERTICAL -> {
                        val verticalScrollState = rememberScrollState()
                        val isShapeSelected = lineDrawingView1.isShapeSelected() || lineDrawingView2.isShapeSelected()
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(verticalScrollState, enabled = !isShapeSelected)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                                VideoPlayerBox(
                                    exoPlayer = exoPlayer1, videoAspectRatio = videoAspectRatio1, videoName = "ビデオ1",
                                    currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L)),
                                    durationOfTrimmedView = (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L),
                                    onClick = { if (!isDrawingMode1) { videoPlayerTargetForDialog = 1; showVideoSourceDialog = true } },
                                    isDrawingMode = isDrawingMode1, onIsDrawingModeChange = { newMode ->
                                        isDrawingMode1 = newMode
                                        if (!newMode) lineDrawingView1.clearSelection()
                                    },
                                    drawMode = drawMode1, onDrawModeChange = { drawMode1 = it },
                                    layoutMode = layoutMode,
                                    onClearLines = { lineDrawingView1.clearCanvas() },
                                    lineDrawingView = lineDrawingView1,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (exoPlayer1 != null || exoPlayer2 != null) Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                                VideoPlayerBox(
                                    exoPlayer = exoPlayer2, videoAspectRatio = videoAspectRatio2, videoName = "ビデオ2",
                                    currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L)),
                                    durationOfTrimmedView = (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L),
                                    onClick = { if (!isDrawingMode2) { videoPlayerTargetForDialog = 2; showVideoSourceDialog = true } },
                                    isDrawingMode = isDrawingMode2, onIsDrawingModeChange = { newMode ->
                                        isDrawingMode2 = newMode
                                        if (!newMode) lineDrawingView2.clearSelection()
                                    },
                                    drawMode = drawMode2, onDrawModeChange = { drawMode2 = it },
                                    layoutMode = layoutMode,
                                    onClearLines = { lineDrawingView2.clearCanvas() },
                                    lineDrawingView = lineDrawingView2,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                    VideoLayoutMode.OVERLAY -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            VideoPlayerBox(
                                exoPlayer = exoPlayer1, videoAspectRatio = videoAspectRatio1, videoName = "ビデオ1",
                                currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L)),
                                durationOfTrimmedView = (originalDuration1Ms - startPosition1Ms).coerceAtLeast(0L),
                                onClick = { /* Overlay mode, click disabled */ },
                                isDrawingMode = isDrawingMode1, onIsDrawingModeChange = { newMode ->
                                    isDrawingMode1 = newMode
                                    if (!newMode) lineDrawingView1.clearSelection()
                                },
                                drawMode = drawMode1, onDrawModeChange = { drawMode1 = it },
                                layoutMode = layoutMode,
                                onClearLines = { lineDrawingView1.clearCanvas() },
                                lineDrawingView = lineDrawingView1,
                                modifier = Modifier.fillMaxSize().zIndex(1f).graphicsLayer(alpha = overlayAlpha1, compositingStrategy = CompositingStrategy.Offscreen)
                            )
                            VideoPlayerBox(
                                exoPlayer = exoPlayer2, videoAspectRatio = videoAspectRatio2, videoName = "ビデオ2",
                                currentPositionInTrimmedView = (sharedCurrentPositionMs).coerceIn(0, (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L)),
                                durationOfTrimmedView = (originalDuration2Ms - startPosition2Ms).coerceAtLeast(0L),
                                onClick = { /* Overlay mode, click disabled */ },
                                isDrawingMode = isDrawingMode2, onIsDrawingModeChange = { newMode ->
                                    isDrawingMode2 = newMode
                                    if (!newMode) lineDrawingView2.clearSelection()
                                },
                                drawMode = drawMode2, onDrawModeChange = { drawMode2 = it },
                                layoutMode = layoutMode,
                                onClearLines = { lineDrawingView2.clearCanvas() },
                                lineDrawingView = lineDrawingView2,
                                modifier = Modifier.fillMaxSize().zIndex(2f).graphicsLayer(alpha = overlayAlpha2, compositingStrategy = CompositingStrategy.Offscreen)
                            )
                        }
                    }
                }
            }

            // Controls section
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (sharedMaxDurationMs > 0) {
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

                if (layoutMode == VideoLayoutMode.OVERLAY) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Video1", modifier = Modifier.width(50.dp))
                            Slider(
                                value = overlayAlpha1,
                                onValueChange = { overlayAlpha1 = it },
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                valueRange = 0f..1f
                            )
                            Text(String.format("%.0f%%", overlayAlpha1 * 100), modifier = Modifier.width(40.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Video2", modifier = Modifier.width(50.dp))
                            Slider(
                                value = overlayAlpha2,
                                onValueChange = { overlayAlpha2 = it },
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                valueRange = 0f..1f
                            )
                            Text(String.format("%.0f%%", overlayAlpha2 * 100), modifier = Modifier.width(40.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { layoutMode = layoutMode.next() }) {
                        Icon(layoutMode.icon, contentDescription = layoutMode.description)
                    }

                    IconButton(onClick = {
                        // Save current drawings before navigating
                        val drawingsJson1 = DrawingSerializer.serializeShapes(lineDrawingView1.getShapes())
                        val drawingsJson2 = DrawingSerializer.serializeShapes(lineDrawingView2.getShapes())
                        // Combine into single JSON with video keys
                        val combinedDrawings = "{\"video1\":$drawingsJson1,\"video2\":$drawingsJson2}"
                        currentBackStackEntry?.savedStateHandle?.set("tempDrawings", combinedDrawings)
                        // Also save alignment points and common offset
                        currentBackStackEntry?.savedStateHandle?.set("alignmentPoint1Ms", alignmentPoint1Ms)
                        currentBackStackEntry?.savedStateHandle?.set("alignmentPoint2Ms", alignmentPoint2Ms)
                        currentBackStackEntry?.savedStateHandle?.set("offset1Ms", offsetMs) // Common offset
                        
                        val encodedUri1 = videoUri1?.let { URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString()) } ?: "null"
                        val encodedUri2 = videoUri2?.let { URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString()) } ?: "null"
                        navController.navigate(
                            "${AppDestinations.START_POSITION_SETTING_ROUTE}/$encodedUri1/$encodedUri2/$startPosition1Ms/$startPosition2Ms/$originalDuration1Ms/$originalDuration2Ms"
                        )
                    }) {
                        Icon(Icons.Filled.Settings, "開始位置設定")
                    }

                    IconButton(onClick = { frameAdvance(-33L) }, enabled = sharedMaxDurationMs > 0) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "前フレーム")
                    }

                    Button(onClick = togglePlayPause, enabled = sharedMaxDurationMs > 0) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "一時停止" else "再生")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(if (isPlaying) "一時停止" else "再生")
                    }

                    IconButton(onClick = { frameAdvance(33L) }, enabled = sharedMaxDurationMs > 0, modifier = Modifier.graphicsLayer(scaleX = -1f)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "次フレーム")
                    }

                    IconButton(onClick = { navController.navigate(AppDestinations.PROJECT_LIST_SCREEN_ROUTE) }) {
                        Icon(Icons.Filled.Assessment, "保存済みプロジェクト")
                    }

                    Button(
                        onClick = { showSaveDialog = true },
                        enabled = exoPlayer1 != null || exoPlayer2 != null
                    ) {
                        Text("保存")
                    }
                }
            }
            }  // End of else block for loading state
        }
    }

    // Save dialog
    if (showSaveDialog) {
        if (loadedProject != null) {
            // Show choice dialog: overwrite or save as new
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("保存方法を選択") },
                text = { Text("既存プロジェクトを上書きしますか、それとも新規保存しますか？") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val db = AppDatabase.getDatabase(context)
                                val repository = ProjectRepository(db.projectDao())
                                
                                // Serialize drawings from both views
                                val drawingsJson1 = DrawingSerializer.serializeShapes(
                                    lineDrawingView1.getShapes()
                                )
                                val drawingsJson2 = DrawingSerializer.serializeShapes(
                                    lineDrawingView2.getShapes()
                                )
                                
                                // Combine drawings JSON: embed arrays directly (no extra quoting)
                                val combinedDrawings = "{\"video1\":$drawingsJson1,\"video2\":$drawingsJson2}"
                                
                                // Overwrite existing project: preserve createdAt, update other fields
                                val updated = ProjectEntity(
                                    id = loadedProject!!.id,
                                    name = loadedProject!!.name,
                                    videoUri1 = videoUri1?.toString(),
                                    videoUri2 = videoUri2?.toString(),
                                    startPosition1Ms = startPosition1Ms,
                                    startPosition2Ms = startPosition2Ms,
                                    alignmentPoint1Ms = alignmentPoint1Ms,
                                    alignmentPoint2Ms = alignmentPoint2Ms,
                                    offsetMs = offsetMs,
                                    drawingsJson = combinedDrawings,
                                    createdAt = loadedProject!!.createdAt,
                                    updatedAt = System.currentTimeMillis()
                                )
                                AppLog.d("VideoScreen", "Saving project: name=${updated.name}, align1=${updated.alignmentPoint1Ms}, offset=${updated.offsetMs}, align2=${updated.alignmentPoint2Ms}")
                                repository.updateProject(updated)
                                // Update loadedProject with the saved data
                                loadedProject = updated
                                
                                // Reset dialog
                                showSaveDialog = false
                                projectNameForSave = ""
                            }
                        }
                    ) {
                        Text("上書き保存")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showSaveDialog = false
                            // Show name input dialog for new save
                            projectNameForSave = ""
                            showSaveDialog = true
                            loadedProject = null
                        }
                    ) {
                        Text("名前をつけて保存")
                    }
                }
            )
        } else {
            // Show name input dialog for new save
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("プロジェクトを保存") },
                text = {
                    TextField(
                        value = projectNameForSave,
                        onValueChange = { projectNameForSave = it },
                        label = { Text("プロジェクト名") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (projectNameForSave.isNotBlank()) {
                                coroutineScope.launch {
                                    val db = AppDatabase.getDatabase(context)
                                    val repository = ProjectRepository(db.projectDao())
                                    
                                    // Serialize drawings from both views
                                    val drawingsJson1 = DrawingSerializer.serializeShapes(
                                        lineDrawingView1.getShapes()
                                    )
                                    val drawingsJson2 = DrawingSerializer.serializeShapes(
                                        lineDrawingView2.getShapes()
                                    )
                                    
                                    // Combine drawings JSON: embed arrays directly (no extra quoting)
                                    val combinedDrawings = "{\"video1\":$drawingsJson1,\"video2\":$drawingsJson2}"
                                    
                                    val project = ProjectEntity(
                                        name = projectNameForSave,
                                        videoUri1 = videoUri1?.toString(),
                                        videoUri2 = videoUri2?.toString(),
                                        startPosition1Ms = startPosition1Ms,
                                        startPosition2Ms = startPosition2Ms,
                                        alignmentPoint1Ms = alignmentPoint1Ms,
                                        alignmentPoint2Ms = alignmentPoint2Ms,
                                        offsetMs = offsetMs,
                                        drawingsJson = combinedDrawings
                                    )
                                    AppLog.d("VideoScreen", "Creating new project: name=${project.name}, align1=${project.alignmentPoint1Ms}, offset=${project.offsetMs}, align2=${project.alignmentPoint2Ms}")
                                    repository.insertProject(project)
                                    
                                    // Reset dialog
                                    showSaveDialog = false
                                    projectNameForSave = ""
                                }
                            }
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    Button(onClick = { showSaveDialog = false }) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}

@Composable
fun DrawingControls(
    layoutMode: VideoLayoutMode,
    isDrawingMode: Boolean,
    onIsDrawingModeChange: (Boolean) -> Unit,
    currentMode: DrawMode,
    onDrawModeChange: (DrawMode) -> Unit,
    onClear: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    // Use a scrollable Row for horizontal mode to prevent layout shift
    if (layoutMode == VideoLayoutMode.HORIZONTAL) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onIsDrawingModeChange(!isDrawingMode) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (isDrawingMode) Icons.Filled.Close else Icons.Filled.BorderColor,
                    contentDescription = "Toggle Drawing",
                    modifier = Modifier.size(20.dp)
                )
            }
            if (isDrawingMode) {
                IconToggleButton(checked = currentMode == DrawMode.SELECT, onCheckedChange = { onDrawModeChange(DrawMode.SELECT) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.TouchApp, contentDescription = "Select", modifier = Modifier.size(20.dp))
                }
                IconToggleButton(checked = currentMode == DrawMode.FREE, onCheckedChange = { onDrawModeChange(DrawMode.FREE) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Brush, contentDescription = "Freehand", modifier = Modifier.size(20.dp))
                }
                IconToggleButton(checked = currentMode == DrawMode.LINE, onCheckedChange = { onDrawModeChange(DrawMode.LINE) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Line", modifier = Modifier.size(20.dp))
                }
                IconToggleButton(checked = currentMode == DrawMode.CIRCLE, onCheckedChange = { onDrawModeChange(DrawMode.CIRCLE) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Circle, contentDescription = "Circle", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDeleteSelected, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Selected", modifier = Modifier.size(20.dp))
                }
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape)
        ) {
            IconButton(onClick = { onIsDrawingModeChange(!isDrawingMode) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (isDrawingMode) Icons.Filled.Close else Icons.Filled.BorderColor,
                    contentDescription = "Toggle Drawing",
                    modifier = Modifier.size(20.dp)
                )
            }
            if (isDrawingMode) {
                IconToggleButton(checked = currentMode == DrawMode.SELECT, onCheckedChange = { onDrawModeChange(DrawMode.SELECT) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.TouchApp, contentDescription = "Select", modifier = Modifier.size(20.dp))
                }
                IconToggleButton(checked = currentMode == DrawMode.FREE, onCheckedChange = { onDrawModeChange(DrawMode.FREE) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Brush, contentDescription = "Freehand", modifier = Modifier.size(20.dp))
                }
                IconToggleButton(checked = currentMode == DrawMode.LINE, onCheckedChange = { onDrawModeChange(DrawMode.LINE) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Line", modifier = Modifier.size(20.dp))
                }
                IconToggleButton(checked = currentMode == DrawMode.CIRCLE, onCheckedChange = { onDrawModeChange(DrawMode.CIRCLE) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Circle, contentDescription = "Circle", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDeleteSelected, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Selected", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

val VideoLayoutMode.icon: ImageVector
    get() = when (this) {
        VideoLayoutMode.VERTICAL -> Icons.Filled.SwapHoriz
        VideoLayoutMode.HORIZONTAL -> Icons.Filled.Layers
        VideoLayoutMode.OVERLAY -> Icons.Filled.SwapVert
    }

val VideoLayoutMode.description: String
    get() = when (this) {
        VideoLayoutMode.VERTICAL -> "横並びに変更"
        VideoLayoutMode.HORIZONTAL -> "重ね表示に切り替え"
        VideoLayoutMode.OVERLAY -> "縦並びに変更"
    }

fun VideoLayoutMode.next(): VideoLayoutMode {
    return when (this) {
        VideoLayoutMode.HORIZONTAL -> VideoLayoutMode.OVERLAY
        VideoLayoutMode.OVERLAY -> VideoLayoutMode.VERTICAL
        VideoLayoutMode.VERTICAL -> VideoLayoutMode.HORIZONTAL
    }
}
