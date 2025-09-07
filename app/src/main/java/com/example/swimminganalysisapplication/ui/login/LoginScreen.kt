package com.example.swimminganalysisapplication.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations
import kotlinx.coroutines.launch // kotlinx.coroutines.launch をインポート

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() } // SnackbarHostState を宣言
    val scope = rememberCoroutineScope() // CoroutineScope を宣言

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ログイン") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // SnackbarHost を Scaffold に追加
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("ようこそ！", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("メールアドレス") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("パスワード") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // TODO: Implement actual login logic
                    // For now, navigate to HomeScreen after "login"
                    // navController.navigate(AppDestinations.HOME_SCREEN_ROUTE) {
                    //    popUpTo(AppDestinations.LOGIN_SCREEN_ROUTE) { inclusive = true }
                    //    launchSingleTop = true
                    // }
                    scope.launch { // CoroutineScope を使用して Snackbar を表示
                        snackbarHostState.showSnackbar(
                            message = "ログイン機能は実装中です。「登録せずに利用を開始する」を選択してください",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ログイン")
            }
            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    navController.navigate(AppDestinations.CREATE_ACCOUNT_SCREEN_ROUTE)
                }
            ) {
                Text("アカウントをお持ちでないですか？ アカウント作成")
            }
            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = {
                    navController.navigate(AppDestinations.HOME_SCREEN_ROUTE) {
                        popUpTo(AppDestinations.LOGIN_SCREEN_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                Text("登録せずに利用を開始する")
            }
        }
    }
}

