package com.example.swimminganalysisapplication.ui.addswimmer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu // ★ AccountActionsMenu をインポート

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSwimmerScreen(
    navController: NavController,
    factory: AddSwimmerViewModelFactory, // Factoryを渡す
    viewModel: AddSwimmerViewModel = viewModel(factory = factory)
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ViewModelのUI状態を監視
    val uiState = viewModel.uiState
    LaunchedEffect(uiState) {
        when (uiState) {
            is AddSwimmerUiState.Success -> {
                snackbarHostState.showSnackbar("選手「${uiState.swimmerName}」を登録しました。")
                viewModel.consumedUiState() // メッセージ表示後に状態をリセット
                navController.popBackStack() // 登録成功したら前の画面に戻る
            }
            is AddSwimmerUiState.Error -> {
                snackbarHostState.showSnackbar("エラー: ${uiState.message}")
                viewModel.consumedUiState() // メッセージ表示後に状態をリセット
            }
            else -> { /* Idle or Loading */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("選手登録") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = { // ★ actions スロットに AccountActionsMenu を追加
                    AccountActionsMenu(navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors( // ★ 色設定を追加
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.swimmerName,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("選手名 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = viewModel.swimmerAge,
                onValueChange = { viewModel.updateAge(it) },
                label = { Text("年齢") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                )
            )

            OutlinedTextField(
                value = viewModel.swimmerTeam,
                onValueChange = { viewModel.updateTeam(it) },
                label = { Text("チーム") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { viewModel.addSwimmer() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState != AddSwimmerUiState.Loading // ローディング中は無効
            ) {
                if (uiState == AddSwimmerUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("登録する")
                }
            }
        }
    }
}

