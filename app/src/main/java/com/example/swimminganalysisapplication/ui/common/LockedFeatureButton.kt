package com.example.swimminganalysisapplication.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * ロックされた機能ボタン
 * ゲストユーザーが機能を使用しようとするとUnlockボタンが表示される
 */
@Composable
fun LockedFeatureButton(
    label: String,
    isLocked: Boolean,
    onUnlockClick: () -> Unit,
    onClick: () -> Unit
) {
    Box {
        Button(
            onClick = onClick,
            enabled = !isLocked,
            modifier = Modifier.size(120.dp, 60.dp)
        ) {
            Text(label)
        }

        if (isLocked) {
            IconButton(
                onClick = onUnlockClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Unlock",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
