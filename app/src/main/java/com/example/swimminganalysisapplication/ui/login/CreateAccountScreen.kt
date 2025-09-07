package com.example.swimminganalysisapplication.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("アカウント作成") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("アカウント作成画面 (実装中)", style = MaterialTheme.typography.headlineSmall)
            // TODO: Add account creation form fields and logic
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* TODO: Implement account creation */ navController.popBackStack() }) {
                Text("作成して戻る (仮)")
            }
        }
    }
}
