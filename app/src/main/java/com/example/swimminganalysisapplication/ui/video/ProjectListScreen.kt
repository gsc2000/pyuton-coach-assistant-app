package com.example.swimminganalysisapplication.ui.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import com.example.swimminganalysisapplication.ui.theme.CustomTopAppBarHeight
import com.example.swimminganalysisapplication.ui.theme.getCustomTopAppBarColors
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.data.storage.AppDatabase
import com.example.swimminganalysisapplication.data.storage.ProjectRepository
import com.example.swimminganalysisapplication.navigation.AppDestinations
import com.example.swimminganalysisapplication.ui.common.AccountActionsMenu
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Get database and repository
    val db = remember { AppDatabase.getDatabase(context) }
    val repository = remember { ProjectRepository(db.projectDao()) }

    // Observe projects from database
    val projects by repository.getAllProjects().collectAsState(initial = emptyList())

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("保存済みプロジェクト") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る")
                    }
                },
                actions = {
                    AccountActionsMenu(navController = navController)
                },
                colors = getCustomTopAppBarColors(),
                modifier = Modifier.heightIn(max = CustomTopAppBarHeight)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navigate to new VideoScreen without projectId
                    navController.navigate(AppDestinations.VIDEO_SCREEN_ROUTE) {
                        launchSingleTop = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, "新規プロジェクト")
            }
        }
    ) { innerPadding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("保存されたプロジェクトがありません", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(projects) { project ->
                    ListItem(
                        headlineContent = { Text(project.name) },
                        supportingContent = {
                            Text(
                                "作成: ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm").format(java.util.Date(project.createdAt))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { projectToDelete = project.id; showDeleteDialog = true }) {
                                Icon(Icons.Filled.Delete, "削除")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Auto-load project when clicked
                                val route = AppDestinations.PROJECT_LOAD_ROUTE.replace("{projectId}", project.id.toString())
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("プロジェクトを削除") },
            text = { Text("本当に削除しますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteProjectById(projectToDelete!!)
                            showDeleteDialog = false
                            projectToDelete = null
                        }
                    }
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
