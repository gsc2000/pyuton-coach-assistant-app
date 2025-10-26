package com.example.swimminganalysisapplication.ui.analysis

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.RetrofitClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisDetailScreen(
    videoId: Int,
    navController: NavController,
    viewModel: AnalysisViewModel
) {
    val analysisDetail by viewModel.selectedAnalysisDetail.collectAsState()

    LaunchedEffect(videoId) {
        viewModel.fetchAnalysisJson(videoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "分析詳細 (ID: $videoId)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            analysisDetail?.let {
                Text("ビデオ名: ${it.videoName}", fontWeight = FontWeight.Bold)
                Text("分析日: ${it.analysisDate}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("合計タイム: ${it.totalTime}秒")
                Text("合計ストローク数: ${it.totalStrokeCount}回")
                Spacer(modifier = Modifier.height(16.dp))
                Text("ラップタイム:", fontWeight = FontWeight.Bold)
                it.lapTimes.forEach { lap ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("  Lap ${lap.lapNumber}:")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${lap.lapTime}秒, ${lap.strokeCount}回")
                    }
                }
            }
        }
    }
}
