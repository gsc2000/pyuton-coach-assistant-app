package com.example.swimminganalysisapplication.ui.analysis

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.navigation.AppDestinations
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.io.File
import java.util.Objects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAnalysisSetupScreen(
    viewModel: SingleAnalysisSetupViewModel,
    navController: NavController
) {
    val videoUri by remember { derivedStateOf { viewModel.videoUri } }
    val date by remember { derivedStateOf { viewModel.date } }
    val selectedPlayer by remember { derivedStateOf { viewModel.selectedPlayer } }
    val comment by remember { derivedStateOf { viewModel.comment } }
    val playerSearchText by remember { derivedStateOf { viewModel.playerSearchText } }
    val players by remember { derivedStateOf { viewModel.players } }
    val isSearching by remember { derivedStateOf { viewModel.isSearching } }
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    val errorMessage by remember { derivedStateOf { viewModel.errorMessage } }
    val context = LocalContext.current

    var showVideoSourceDialog by remember { mutableStateOf(false) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    // --- カメラ撮影用ランチャー ---
    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            viewModel.onVideoSelected(tempVideoUri)
        } else {
            Toast.makeText(context, "動画の撮影がキャンセルされました。", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 権限要求用ランチャー ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 権限が許可されたので、カメラを起動する
            val videoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(
                Objects.requireNonNull(context),
                "${context.packageName}.provider",
                videoFile
            )
            tempVideoUri = uri
            videoCaptureLauncher.launch(uri)
        } else {
            // 権限が拒否された場合
            Toast.makeText(context, "カメラの権限が拒否されました。", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.jobStartedEvent.collectLatest { jobResponse ->
            Toast.makeText(context, "解析ジョブを開始しました: ${jobResponse.jobId}", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToAnalysisList -> {
                    navController.navigate(AppDestinations.ANALYSIS_LIST_SCREEN_ROUTE)
                }
            }
        }
    }

    // ギャラリー用ランチャー
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onVideoSelected(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("単体解析セットアップ") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { showVideoSourceDialog = true }) {
                    Text(if (videoUri != null) "動画を変更" else "動画を選択")
                }

                videoUri?.let {
                    Text("選択中の動画: ${it.path}")
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { viewModel.onDateChange(it) },
                    label = { Text("日付") },
                    modifier = Modifier.fillMaxWidth()
                )

                PlayerSearch(
                    searchText = playerSearchText,
                    onSearchTextChange = { viewModel.onPlayerSearchTextChange(it) },
                    players = players,
                    onPlayerSelected = { viewModel.onPlayerSelected(it) },
                    onAddNewPlayer = { viewModel.addNewPlayer(it) },
                    isSearching = isSearching
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { viewModel.onCommentChange(it) },
                    label = { Text("コメント") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { viewModel.startAnalysis(context) },
                    enabled = videoUri != null && selectedPlayer != null && !isLoading
                ) {
                    Text("解析開始")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.onNavigateToAnalysisListClicked() }
                ) {
                    Text("解析一覧へ")
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    // --- 修正されたダイアログ ---
    if (showVideoSourceDialog) {
        Dialog(onDismissRequest = { showVideoSourceDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "動画のソースを選択",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    Button(
                        onClick = {
                            showVideoSourceDialog = false
                            videoPickerLauncher.launch("video/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ギャラリーから選択")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showVideoSourceDialog = false
                            // --- 権限チェックと要求 ---
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) -> {
                                    // 権限がすでに許可されている場合、カメラを起動
                                    val videoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
                                    val uri = FileProvider.getUriForFile(
                                        Objects.requireNonNull(context),
                                        "${context.packageName}.provider",
                                        videoFile
                                    )
                                    tempVideoUri = uri
                                    videoCaptureLauncher.launch(uri)
                                }
                                else -> {
                                    // 権限がないので、許可を要求する
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("カメラで撮影")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = { showVideoSourceDialog = false }) {
                        Text("キャンセル")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSearch(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    players: List<Player>,
    onPlayerSelected: (Player) -> Unit,
    onAddNewPlayer: (String) -> Unit,
    isSearching: Boolean
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchText)) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(searchText) {
        if (textFieldValue.text != searchText) {
            textFieldValue = textFieldValue.copy(text = searchText)
        }
    }

    LaunchedEffect(textFieldValue) {
        if (textFieldValue.composition == null) {
            delay(300)
            if (textFieldValue.text != searchText) {
                onSearchTextChange(textFieldValue.text)
            }
        }
    }

    Box {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                expanded = true
            },
            label = { Text("選手を検索") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        DropdownMenu(
            expanded = expanded && (players.isNotEmpty() || (isSearching && textFieldValue.text.isNotEmpty()) || (textFieldValue.text.isNotBlank() && players.none { (it.playerName ?: "").equals(textFieldValue.text, ignoreCase = true) })),
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(),
            properties = PopupProperties(focusable = false)
        ) {
            if (isSearching && textFieldValue.text.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("検索中...") },
                    onClick = { },
                    enabled = false
                )
            } else {
                players.forEach { player ->
                    val playerName = player.playerName ?: "名前なし"
                    DropdownMenuItem(
                        text = { Text(playerName) },
                        onClick = {
                            onPlayerSelected(player)
                            textFieldValue = textFieldValue.copy(
                                text = playerName,
                                selection = TextRange(playerName.length)
                            )
                            expanded = false
                        }
                    )
                }
                if (players.none { (it.playerName ?: "").equals(textFieldValue.text, ignoreCase = true) } && textFieldValue.text.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("「${textFieldValue.text}」を新規追加") },
                        onClick = {
                            onAddNewPlayer(textFieldValue.text)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}