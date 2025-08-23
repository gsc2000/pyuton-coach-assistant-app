package com.example.swimminganalysisapplication

// AndroidX & System imports
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button // VideoScreenで使用
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text // VideoScreenで使用
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

// Project specific imports
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.RetrofitClient
import com.example.swimminganalysisapplication.ui.swimmers.SwimmersScreen // こちらを呼び出す
import com.example.swimminganalysisapplication.ui.swimmers.SwimmersViewModelFactory
import com.example.swimminganalysisapplication.ui.theme.SwimmingAnalysisApplicationTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val apiService by lazy { RetrofitClient.instance }
    private val swimmingRepository by lazy { SwimmingRepository(apiService) }
    private val swimmersViewModelFactory by lazy { SwimmersViewModelFactory(swimmingRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwimmingAnalysisApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // VideoScreen() の代わりに SwimmersScreen を呼び出す
                    SwimmersScreen(factory = swimmersViewModelFactory)
                }
            }
        }
    }
}

// VideoScreen Composable - アプリのメインコンテンツではなくなったため、
// このファイルに残しておくか、別のファイルに移動するか、削除するかは後ほど検討できます。
// 今回の修正では MainActivity の setContent からは呼び出されません。
@Composable
fun VideoScreen() {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Camera permission granted.")
        } else {
            Log.w(TAG, "Camera permission denied.")
        }
    }

    val requestStoragePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Storage permission granted.")
        } else {
            Log.w(TAG, "Storage permission denied.")
        }
    }

    val takeVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        Log.d(TAG, "takeVideoLauncher (StartActivityForResult) callback: resultCode = ${result.resultCode}, data = ${result.data}")
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                Log.i(TAG, "Video captured successfully, URI from intent data: $uri")
                videoUri = uri
            } else {
                Log.e(TAG, "Video capture RESULT_OK, but no URI in intent data!")
                videoUri = null
            }
        } else {
            Log.w(TAG, "Video capture failed, was cancelled, or camera app returned non-OK. resultCode: ${result.resultCode}")
            videoUri = null
        }
    }

    val selectVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d(TAG, "selectVideoLauncher callback: received URI = $uri")
        if (uri != null) {
            videoUri = uri
            Log.i(TAG, "selectVideoLauncher: videoUri successfully set to: $videoUri")
        } else {
            Log.w(TAG, "selectVideoLauncher: received null URI.")
        }
    }

    LaunchedEffect(videoUri) {
        Log.d(TAG, "LaunchedEffect triggered. videoUri: $videoUri")
        if (videoUri != null) {
            exoPlayer?.release()
            Log.d(TAG, "Creating new ExoPlayer instance.")
            val newPlayer = ExoPlayer.Builder(context).build().apply {
                Log.d(TAG, "Setting media item to ExoPlayer: $videoUri")
                setMediaItem(MediaItem.fromUri(videoUri!!))
                prepare()
                Log.d(TAG, "ExoPlayer prepared.")
            }
            exoPlayer = newPlayer
            Log.i(TAG, "ExoPlayer initialized and assigned.")

            Log.d(TAG, "Attempting to start VideoProcessingService with URI: $videoUri")
            VideoProcessingService.startService(context, videoUri!!) 

        } else {
            Log.d(TAG, "videoUri is null. Releasing ExoPlayer.")
            exoPlayer?.release()
            exoPlayer = null
            Log.i(TAG, "ExoPlayer released and nulled.")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "VideoScreen DisposableEffect: Releasing ExoPlayer.")
            exoPlayer?.release()
            exoPlayer = null 
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (exoPlayer != null && videoUri != null) {
                Log.d(TAG, "Displaying PlayerView. ExoPlayer: $exoPlayer, VideoUri: $videoUri")
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Log.d(TAG, "Displaying 'No video selected'. ExoPlayer: $exoPlayer, VideoUri: $videoUri")
                Text("No video selected, or player not ready.")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                Log.d(TAG, "Take Video button clicked.")
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Camera permission granted. Launching camera intent.")
                    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        takeVideoLauncher.launch(intent)
                    } else {
                        Log.e(TAG, "No activity found to handle video capture intent.")
                    }
                } else {
                    Log.d(TAG, "Requesting camera permission.")
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text("Take Video")
            }

            Button(onClick = {
                Log.d(TAG, "Select Video button clicked.")
                val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_VIDEO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                if (ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Storage permission granted. Launching selectVideoLauncher.")
                    selectVideoLauncher.launch("video/*")
                } else {
                    Log.d(TAG, "Requesting storage permission: $permissionToRequest")
                    requestStoragePermissionLauncher.launch(permissionToRequest)
                }
            }) {
                Text("Select Video")
            }
        }
    }
}
