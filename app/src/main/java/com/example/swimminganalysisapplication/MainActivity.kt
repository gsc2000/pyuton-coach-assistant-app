package com.example.swimminganalysisapplication

import android.Manifest
import android.app.Activity
import android.content.Context
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue // 明示的なimportを追加
import androidx.compose.runtime.setValue // 明示的なimportを追加
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.swimminganalysisapplication.ui.theme.SwimmingAnalysisApplicationTheme
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
// import java.io.File // createVideoFileUri を使わないためコメントアウト
// import java.text.SimpleDateFormat // createVideoFileUri を使わないためコメントアウト
// import java.util.* // createVideoFileUri を使わないためコメントアウト

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwimmingAnalysisApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VideoScreen()
                }
            }
        }
    }
}

// fun Context.createVideoFileUri(): Uri { ... } // この関数は使用しないためコメントアウトまたは削除

@Composable
fun VideoScreen() {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) } // mutableStateOF を mutableStateOf に修正済
    // var tempVideoFileUri by remember { mutableStateOf<Uri?>(null) } // 使用しないためコメントアウト

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
        ActivityResultContracts.StartActivityForResult() // StartActivityForResult に変更済
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

            // ★★★ Service の起動を追加 ★★★
            Log.d(TAG, "Attempting to start VideoProcessingService with URI: $videoUri")
            VideoProcessingService.startService(context, videoUri!!) // videoUriがnullでないことを確認済み

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
                    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE) // Intent のみ作成
                    // EXTRA_OUTPUT (tempVideoFileUri) は指定しない
                    if (intent.resolveActivity(context.packageManager) != null) {
                        takeVideoLauncher.launch(intent) // Intent を渡して起動
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
