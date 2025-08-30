package com.example.swimminganalysisapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
            text = "スイミング分析アプリ",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = { navController.navigate(AppDestinations.VIDEO_SCREEN_ROUTE) },
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            Text("動画解析")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { navController.navigate(AppDestinations.SWIMMERS_SCREEN_ROUTE) },
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            Text("選手一覧")
        }
    }
}

