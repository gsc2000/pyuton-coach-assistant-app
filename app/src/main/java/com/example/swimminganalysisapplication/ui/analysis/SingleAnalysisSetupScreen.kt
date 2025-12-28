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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import com.example.swimminganalysisapplication.ui.theme.CustomTopAppBarHeight
import com.example.swimminganalysisapplication.ui.theme.getCustomTopAppBarColors
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by remember { derivedStateOf { viewModel.errorMessage } }
    val context = LocalContext.current

    var showVideoSourceDialog by remember { mutableStateOf(false) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            viewModel.onVideoSelected(tempVideoUri)
        } else {
            Toast.makeText(context, "動画の撮影がキャンセルされました。", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val videoFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(
                Objects.requireNonNull(context),
                "${context.packageName}.provider",
                videoFile
            )
            tempVideoUri = uri
            videoCaptureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "カメラの権限が拒否されました。", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AnalysisUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                navController.popBackStack() // Or navigate to result screen
            }
            is AnalysisUiState.Error -> {
                // Error messages are now handled by the errorMessage state
            }
            else -> Unit
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToAnalysisList -> {
                    navController.navigate(AppDestinations.ANALYSIS_LIST_SCREEN_ROUTE)
                }
            }
        }
    }

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
                },
                actions = {
                    AccountActionsMenu(navController = navController)
                },
                colors = getCustomTopAppBarColors(),
                modifier = Modifier.heightIn(max = CustomTopAppBarHeight)
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
                    onPlayerSearchFocused = { viewModel.onPlayerSearchFocused() },
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
                    enabled = videoUri != null && selectedPlayer != null && uiState !is AnalysisUiState.Loading
                ) {
                    if (uiState is AnalysisUiState.Loading) {
                        Text((uiState as AnalysisUiState.Loading).message)
                    } else {
                        Text("解析開始")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.onNavigateToAnalysisListClicked() }
                ) {
                    Text("解析一覧へ")
                }
            }

            if (uiState is AnalysisUiState.Loading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text((uiState as AnalysisUiState.Loading).message)
                        }
                    }
                }
            }
        }
    }

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
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) -> {
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
    onPlayerSearchFocused: () -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused) {
                        onPlayerSearchFocused()
                        expanded = true
                    }
                },
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