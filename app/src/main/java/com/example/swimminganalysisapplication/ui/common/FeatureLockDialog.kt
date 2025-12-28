package com.example.swimminganalysisapplication.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * 機能ロック確認ダイアログ
 * ゲストユーザーが機能を使用しようとするときに表示
 */
@Composable
fun FeatureLockDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("会員登録が必要です") },
            text = { Text("この機能を使用するには会員登録が必要です。\nアカウント作成ページへ進みますか？") },
            confirmButton = {
                Button(
                    onClick = {
                        onNavigateToSignUp()
                        onDismiss()
                    }
                ) {
                    Text("アカウント作成")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("キャンセル")
                }
            }
        )
    }
}
