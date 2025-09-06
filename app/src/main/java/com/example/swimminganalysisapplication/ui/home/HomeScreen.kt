package com.example.swimminganalysisapplication.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt // For "選手一覧"
import androidx.compose.material.icons.filled.Analytics // For "動画解析を開始"
import androidx.compose.material.icons.filled.PostAdd // For "練習メニューを作成"
// Remove SportsScore, add new icons
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pyuton Coach Assistant") }, // Updated App Title
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from Scaffold
                .padding(16.dp), // Add overall padding for content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Consistent spacing between items
        ) {

            Spacer(modifier = Modifier.height(16.dp)) // Add some space at the top

            // Replaced single Icon with a Row of sports pictograms
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp), // Space between icons
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics { this.contentDescription = "アプリロゴ - 各種スポーツアイコン" }
            ) {
                Icon(
                    imageVector = Icons.Filled.Pool,
                    contentDescription = "スイミングアイコン", // Individual description for accessibility
                    modifier = Modifier.size(40.dp), // Adjusted size for multiple icons
                    tint = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = "フィットネスアイコン",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.DirectionsRun,
                    contentDescription = "ランニングアイコン",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "ようこそ！",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "AIが導き、つながりが広げる\nコーチングの未来", // Updated catchphrase with newline
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Navigation Cards
            HomeNavigationCard(
                title = "動画解析を開始",
                icon = Icons.Filled.Analytics,
                onClick = { navController.navigate(AppDestinations.VIDEO_SCREEN_ROUTE) }
            )

            HomeNavigationCard(
                title = "選手一覧",
                icon = Icons.AutoMirrored.Filled.ListAlt,
                onClick = { navController.navigate(AppDestinations.SWIMMERS_SCREEN_ROUTE) }
            )

            HomeNavigationCard(
                title = "練習メニュー", // Title changed from "練習メニューを作成"
                icon = Icons.Filled.PostAdd, // Icon can be changed if needed e.g. Icons.Filled.List
                onClick = { navController.navigate(AppDestinations.PRACTICE_LIST_SCREEN_ROUTE) } // Navigate to Practice List
            )

            Spacer(modifier = Modifier.weight(1f)) // Pushes content to the top if not enough to fill screen
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeNavigationCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Ensures card height fits content
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp) // Increased padding for a more spacious feel
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title, // Content description for the card's icon
                modifier = Modifier.size(36.dp), // Slightly larger icon
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge, // Larger text for cards
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

