package com.example.swimminganalysisapplication.ui.swimmers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add // ★ Addアイコンをimport
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.remote.model.Swimmer
import com.example.swimminganalysisapplication.navigation.AppDestinations // ★ AppDestinationsをimport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwimmersScreen(
    navController: NavController,
    factory: SwimmersViewModelFactory,
    viewModel: SwimmersViewModel = viewModel(factory = factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("選手一覧") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }
            )
        },
        floatingActionButton = { // ★ FABを追加
            FloatingActionButton(onClick = {
                navController.navigate(AppDestinations.ADD_SWIMMER_ROUTE)
            }) {
                Icon(Icons.Filled.Add, "選手登録")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (!uiState.isLoading && uiState.errorMessage == null) {
                if (uiState.swimmers.isEmpty()) {
                    Text(
                        text = "選手データがありません。",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    SwimmerList(swimmers = uiState.swimmers)
                }
            }
        }
    }
}

@Composable
fun SwimmerList(swimmers: List<Swimmer>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(swimmers, key = { swimmer -> swimmer.id }) { swimmer ->
            SwimmerItem(swimmer = swimmer)
        }
    }
}

@Composable
fun SwimmerItem(swimmer: Swimmer) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = swimmer.name, style = MaterialTheme.typography.titleMedium)
            swimmer.age?.let { Text("年齢: $it") }
            swimmer.team?.let { Text("チーム: $it") }
        }
    }
}
