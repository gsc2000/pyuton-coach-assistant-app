package com.example.swimminganalysisapplication.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.swimminganalysisapplication.ui.theme.CustomTopAppBarHeight
import com.example.swimminganalysisapplication.ui.theme.getCustomTopAppBarColors
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.RetrofitClient
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    onNavigateBack: () -> Unit,
) {
    // --- このプロジェクトの設計に従い、手動で依存性を注入します ---
    // 1. Contextの取得
    val context = LocalContext.current
    // 2. ApiServiceの取得
    val apiService = RetrofitClient.getInstance(context)
    // 3. Repositoryの生成
    val repository = SwimmingRepository(apiService)
    // 4. 専用Factoryの生成
    val factory = CreateAccountViewModelFactory(repository)
    // 5. ViewModelの生成
    val viewModel: CreateAccountViewModel = viewModel(factory = factory)
    // --- DI完了 ---


    val username by viewModel.username.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val birthday by viewModel.birthday.collectAsState()
    val createAccountState by viewModel.createAccountState.collectAsState()

    // アカウント作成成功時の処理
    LaunchedEffect(createAccountState) {
        if (createAccountState is CreateAccountState.Success) {
            delay(2000) // 2秒待機
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("アカウント作成") },
                modifier = Modifier.heightIn(max = CustomTopAppBarHeight)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ユーザー名入力
                OutlinedTextField(
                    value = username,
                    onValueChange = viewModel::onUsernameChange,
                    label = { Text("ユーザー名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // メールアドレス入力
                OutlinedTextField(
                    value = email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("メールアドレス") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // パスワード入力
                OutlinedTextField(
                    value = password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("パスワード") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 誕生日入力フィールド
                OutlinedTextField(
                    value = birthday,
                    onValueChange = viewModel::onBirthdayChange,
                    label = { Text("誕生日 (任意, YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // アカウント作成ボタン
                Button(
                    onClick = { viewModel.createAccount() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = createAccountState !is CreateAccountState.Loading
                ) {
                    Text("アカウントを作成")
                }

                // --- 状態に応じたUI表示 ---
                when (val state = createAccountState) {
                    is CreateAccountState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                    }
                    is CreateAccountState.Success -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    is CreateAccountState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    is CreateAccountState.Idle -> {
                        // 何も表示しない
                    }
                }
            }
        }
    }
}