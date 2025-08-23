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
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController // ★ NavControllerをimport

private const val TAG = "VideoScreen"

@Composable
fun VideoScreen(navController: NavController) { // ★ navControllerを引数に追加
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
            }
        } else {
            Log.w(TAG, "Video capture failed, was cancelled, or camera app returned non-OK. resultCode: ${result.resultCode}")
        }
    }

    val selectVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d(TAG, "selectVideoLauncher callback: received URI = $uri")
        videoUri = uri
        if (uri != null) {
            Log.i(TAG, "selectVideoLauncher: videoUri successfully set to: $videoUri")
        } else {
            Log.w(TAG, "selectVideoLauncher: received null URI.")
        }
    }

    LaunchedEffect(videoUri) {
        Log.d(TAG, "LaunchedEffect triggered. videoUri: $videoUri")
        exoPlayer?.release()
        exoPlayer = null

        if (videoUri != null) {
            Log.d(TAG, "Creating new ExoPlayer instance.")
            val newPlayer = ExoPlayer.Builder(context).build().apply {
                Log.d(TAG, "Setting media item to ExoPlayer: $videoUri")
                setMediaItem(MediaItem.fromUri(videoUri!!))
                prepare()
                Log.d(TAG, "ExoPlayer prepared.")
            }
            exoPlayer = newPlayer
            Log.i(TAG, "ExoPlayer initialized and assigned.")
        } else {
            Log.i(TAG, "videoUri is null. ExoPlayer remains released.")
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

        // ★ 「選手一覧へ」ボタンを追加
        Button(onClick = {
            Log.d(TAG, "Navigate to Swimmers Screen button clicked.")
            navController.navigate("swimmers_screen") // ルート名を直接指定
        }) {
            Text("選手一覧へ")
        }
    }
}
