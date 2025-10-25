package com.example.swimminganalysisapplication.ui.analysis

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    val isLoading by remember { derivedStateOf { viewModel.isLoading } }
    val errorMessage by remember { derivedStateOf { viewModel.errorMessage } }
    val context = LocalContext.current

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
                Button(onClick = { videoPickerLauncher.launch("video/*") }) {
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

                // ★★★ null許容の playerName を正しく扱うように修正 ★★★
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

    // searchText (from ViewModel) -> textFieldValue
    LaunchedEffect(searchText) {
        if (textFieldValue.text != searchText) {
            textFieldValue = textFieldValue.copy(text = searchText)
        }
    }

    // textFieldValue -> onSearchTextChange (to ViewModel) with debounce
    LaunchedEffect(textFieldValue) {
        // To avoid sending intermediate states of IME composition
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
                    // ★★★ ここでnullをチェックし、代替テキストを表示 ★★★
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
                // ★★★ ここでもnullをチェック ★★★
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