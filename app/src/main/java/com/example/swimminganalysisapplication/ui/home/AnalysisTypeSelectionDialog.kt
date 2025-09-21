package com.example.swimminganalysisapplication.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AnalysisTypeSelectionDialog(
    onDismissRequest: () -> Unit,
    onSingleAnalysisClick: () -> Unit,
    onComparisonAnalysisClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("解析種別の選択") },
        text = {
            Column {
                Text("どちらの解析を行いますか？")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSingleAnalysisClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("単体解析 (新規)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onComparisonAnalysisClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("比較解析 (既存)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("キャンセル")
            }
        }
    )
}
