package com.example.swimminganalysisapplication.ui.common

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.example.swimminganalysisapplication.navigation.AppDestinations

private const val TAG_ACCOUNT_ACTIONS = "AccountActionsMenu"

@Composable
fun AccountActionsMenu(navController: NavController) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "アカウント情報"
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("設定") },
                onClick = {
                    showMenu = false
                    Log.d(TAG_ACCOUNT_ACTIONS, "Settings option clicked.")
                    // TODO: 設定画面への実際のナビゲーションを実装
                    // navController.navigate("settings_screen_route")
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "設定アイコン"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("ログアウト") },
                onClick = {
                    showMenu = false
                    Log.d(TAG_ACCOUNT_ACTIONS, "Logout option clicked.")
                    navController.navigate(AppDestinations.LOGIN_SCREEN_ROUTE) {
                        // HomeScreenまで遡ってスタックをクリアし、HomeScreen自体もクリア
                        popUpTo(AppDestinations.HOME_SCREEN_ROUTE) {
                            inclusive = true
                        }
                        // LoginScreenが既にスタックのトップにある場合に再作成しないようにする
                        launchSingleTop = true
                    }
                },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "ログアウトアイコン"
                    )
                }
            )
        }
    }
}
