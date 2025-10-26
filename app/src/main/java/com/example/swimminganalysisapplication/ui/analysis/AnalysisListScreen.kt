package com.example.swimminganalysisapplication.ui.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.RetrofitClient
import com.example.swimminganalysisapplication.data.remote.model.AnalysisDetail
import com.example.swimminganalysisapplication.data.remote.model.Video
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisListScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val repository = SwimmingRepository(RetrofitClient.getInstance(context))
    val viewModel: AnalysisViewModel = viewModel(
        factory = AnalysisViewModelFactory(repository)
    )
    val videos by viewModel.videos.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedAnalysisDetail by viewModel.selectedAnalysisDetail.collectAsState()
    val showDetailDialog by viewModel.showDetailDialog.collectAsState()

    if (showDetailDialog && selectedAnalysisDetail != null) {
        AnalysisDetailDialog(
            analysisDetail = selectedAnalysisDetail!!,
            onDismissRequest = { viewModel.dismissDetailDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "ビデオ一覧") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    AccountActionsMenu(navController = navController)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (videos.isEmpty()) {
                Text(
                    text = "ビデオがありません。",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(videos) { video ->
                        VideoItem(
                            video = video,
                            onItemClick = {
                                viewModel.fetchAnalysisJson(video.videoId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoItem(video: Video, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onItemClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "ビデオID: ${video.videoId}", style = MaterialTheme.typography.titleMedium)
            Text(text = "タイトル: ${video.videoTitle ?: "(タイトルなし)"}")
            Text(text = "ユーザーID: ${video.userId}")
            Text(text = "アップロード日時: ${video.videoUploadedAt}")
        }
    }
}

@Composable
fun AnalysisDetailDialog(analysisDetail: AnalysisDetail, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("分析詳細 (ID: ${analysisDetail.analysisId})") },
        text = {
            Column {
                Text("ビデオ名: ${analysisDetail.videoName}", fontWeight = FontWeight.Bold)
                Text("分析日: ${analysisDetail.analysisDate}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("合計タイム: ${analysisDetail.totalTime}秒")
                Text("合計ストローク数: ${analysisDetail.totalStrokeCount}回")
                Spacer(modifier = Modifier.height(16.dp))
                Text("ラップタイム:", fontWeight = FontWeight.Bold)
                analysisDetail.lapTimes.forEach { lap ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("  Lap ${lap.lapNumber}:")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${lap.lapTime}秒, ${lap.strokeCount}回")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("閉じる")
            }
        }
    )
}
