package com.example.swimminganalysisapplication

import android.Manifest
import android.app.Activity
// import android.content.Context // VideoScreenで使用、LoginScreenでは未使用
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// import androidx.compose.runtime.getValue // 明示的なimportを追加 (既にある)
// import androidx.compose.runtime.setValue // 明示的なimportを追加 (既にある)
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.swimminganalysisapplication.data.remote.RetrofitClient // 追加
import com.example.swimminganalysisapplication.data.remote.model.HTTPValidationError // 追加
import com.example.swimminganalysisapplication.ui.theme.SwimmingAnalysisApplicationTheme
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.gson.Gson // 追加
import kotlinx.coroutines.launch // 追加

private const val TAG = "MainActivity" // VideoScreen用に残す
private const val LOGIN_TAG = "LoginScreen" // LoginScreen用に新しいTAGを追加

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwimmingAnalysisApplicationTheme {
                // ログイン状態を管理するState
                var isLoggedIn by remember { mutableStateOf(false) } // 初期状態は未ログイン

                // ログイン成功時に呼び出されるコールバック
                val onLoginSuccess: (String) -> Unit = { token ->
                    Log.i(LOGIN_TAG, "Login successful, token received.")
                    // ここでトークンを保存する処理 (例: SharedPreferences, DataStore)
                    // (今回はシンプルにするため、保存処理は省略し、ログイン状態のみ変更)
                    isLoggedIn = true
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoggedIn) {
                        VideoScreen()
                    } else {
                        LoginScreen(onLoginSuccess = onLoginSuccess)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) { // ログイン成功コールバックを受け取る
    var username by remember { mutableStateOf("user@example.com") } // テスト用ユーザー名
    var password by remember { mutableStateOf("aaa") } // テスト用パスワード
    var loginResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
            // visualTransformation = PasswordVisualTransformation() // 必要なら追加
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                isLoading = true
                loginResult = null
                coroutineScope.launch {
                    try {
                        Log.d(LOGIN_TAG, "Attempting login with U: $username") // パスワードはログに出力しないのが一般的
                        val apiService = RetrofitClient.instance
                        val response = apiService.login(
                            username = username,
                            password = password
                        )

                        if (response.isSuccessful) {
                            val token = response.body()
                            if (token != null && token.isNotBlank()) { // トークンがnullでなく、空文字列でもないことを確認
                                loginResult = "Success!" // Token内容はログで確認するのでUIにはシンプルに
                                Log.i(LOGIN_TAG, "Token: $token")
                                onLoginSuccess(token) // 成功コールバックを呼び出し
                            } else {
                                loginResult = "Login Error: Token is null or empty. Code: ${response.code()}"
                                Log.e(LOGIN_TAG, "Token is null or empty from response body. Code: ${response.code()}")
                            }
                        } else {
                            val errorBodyString = response.errorBody()?.string()
                            var errorMsg = "Login Error: ${response.code()}"
                            if (!errorBodyString.isNullOrEmpty()) {
                                errorMsg += "\nBody: (See Logcat)" // UIには詳細を出さない
                                Log.e(LOGIN_TAG, "Code: ${response.code()}, Body: $errorBodyString")
                                if (response.code() == 422) {
                                    try {
                                        val validationError = Gson().fromJson(errorBodyString, HTTPValidationError::class.java)
                                        val specificErrors = validationError?.detail?.joinToString { vd -> vd.msg ?: "Unknown error" } ?: "No details"
                                        errorMsg += "\nValidation: $specificErrors" // UIに少し詳細を出す場合
                                        Log.e(LOGIN_TAG, "Parsed 422 Error: $specificErrors")
                                    } catch (e: Exception) {
                                        Log.e(LOGIN_TAG, "Failed to parse 422 error body: $e")
                                    }
                                }
                            } else {
                                Log.e(LOGIN_TAG, "Code: ${response.code()}, Error body is null or empty")
                            }
                            loginResult = errorMsg
                        }
                    } catch (e: Exception) {
                        loginResult = "Login Exception: ${e.localizedMessage ?: "Unknown error"}"
                        Log.e(LOGIN_TAG, "Exception during login", e)
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Login")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        loginResult?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// VideoScreen() の続きと末尾
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
            exoPlayer = null // ここで exoPlayer を null に設定
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
