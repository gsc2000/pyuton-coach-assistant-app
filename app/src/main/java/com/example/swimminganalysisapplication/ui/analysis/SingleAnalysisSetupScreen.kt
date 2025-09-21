package com.example.swimminganalysisapplication.ui.analysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.navigation.AppDestinations
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.analysisResult.collectLatest { analysis ->
            navController.navigate("${AppDestinations.ANALYSIS_PROGRESS_ROUTE}/${analysis.analysisId}")
        }
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video selection
            Button(onClick = { videoPickerLauncher.launch("video/*") }) {
                Text(if (videoUri != null) "動画を変更" else "動画を選択")
            }
            videoUri?.let {
                Text("選択中の動画: ${it.path}")
            }

            // Date picker
            OutlinedTextField(
                value = date,
                onValueChange = { viewModel.onDateChange(it) },
                label = { Text("日付") },
                modifier = Modifier.fillMaxWidth()
            )

            // Player search and selection
            PlayerSearch(
                searchText = playerSearchText,
                onSearchTextChange = { viewModel.onPlayerSearchTextChange(it) },
                players = players,
                onPlayerSelected = { viewModel.onPlayerSelected(it) },
                onAddNewPlayer = { viewModel.addNewPlayer(it) },
                isSearching = isSearching
            )


            // Comment
            OutlinedTextField(
                value = comment,
                onValueChange = { viewModel.onCommentChange(it) },
                label = { Text("コメント") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )

            // Start analysis button
            Button(
                onClick = { viewModel.startAnalysis(context) },
                enabled = videoUri != null && selectedPlayer != null
            ) {
                Text("解析開始")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigate to analysis list button
            Button(
                onClick = { viewModel.onNavigateToAnalysisListClicked() }
            ) {
                Text("解析一覧へ")
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

    // ViewModelのsearchTextが変更された場合（例：選手選択後）に内部の状態を更新
    LaunchedEffect(searchText) {
        if (textFieldValue.text != searchText) {
            textFieldValue = textFieldValue.copy(text = searchText)
        }
    }

    // 入力が落ち着いたら検索を実行
    LaunchedEffect(textFieldValue) {
        // 変換中は検索しない
        if (textFieldValue.composition == null) {
            delay(300) // 300msのデバウンス
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
            singleLine = true, // 改行を無効化
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        DropdownMenu(
            expanded = expanded,
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
                    DropdownMenuItem(
                        text = { Text(player.playerName) },
                        onClick = {
                            onPlayerSelected(player)
                            // textfieldの値を更新するが、onSearchTextChangeは呼ばない
                            textFieldValue = textFieldValue.copy(
                                text = player.playerName,
                                selection = TextRange(player.playerName.length)
                            )
                            expanded = false
                        }
                    )
                }
                if (players.none { it.playerName.equals(textFieldValue.text, ignoreCase = true) } && textFieldValue.text.isNotBlank()) {
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
