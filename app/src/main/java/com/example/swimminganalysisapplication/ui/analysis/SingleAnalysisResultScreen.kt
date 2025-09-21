package com.example.swimminganalysisapplication.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAnalysisResultScreen(
    viewModel: SingleAnalysisResultViewModel,
    navController: NavController
) {
    val analysis by viewModel.analysis
    val errorMessage by viewModel.errorMessage

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is SingleAnalysisResultViewModel.NavigationEvent.NavigateToAnalysisList -> {
                    navController.navigate(AppDestinations.ANALYSIS_LIST_SCREEN_ROUTE) {
                        // Clear back stack up to home screen
                        popUpTo(AppDestinations.HOME_SCREEN_ROUTE)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("単体解析結果") },
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
            verticalArrangement = Arrangement.Center
        ) {
            if (errorMessage != null) {
                Text(text = errorMessage!!)
            } else if (analysis == null) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("解析結果を読み込んでいます...")
            } else {
                // Display analysis results here
                Text("解析ID: ${analysis!!.analysisId}")
                Text("ステータス: ${analysis!!.status ?: "N/A"}")
                // Add more details as needed
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.onNavigateToAnalysisListClicked() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("解析一覧へ")
                }
            }
        }
    }
}
