package com.example.swimminganalysisapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "スイムアナリシス",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { navController.navigate(AppDestinations.VIDEO_SCREEN_ROUTE) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("動画解析を開始")
        }

        Button(
            onClick = { navController.navigate(AppDestinations.SWIMMERS_SCREEN_ROUTE) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("選手一覧")
        }

        // New Button to navigate to CreatePracticeMenuScreen
        Button(
            onClick = { navController.navigate(AppDestinations.CREATE_PRACTICE_MENU_ROUTE) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("練習メニューを作成")
        }

        // You can add more buttons here for other features

    }
}
