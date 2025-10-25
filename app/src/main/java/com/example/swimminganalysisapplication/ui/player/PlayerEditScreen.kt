package com.example.swimminganalysisapplication.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerEditScreen(
    navController: NavController,
    viewModel: PlayerViewModel,
    playerId: String?
) {
    val isEditing = playerId != null
    var playerName by remember { mutableStateOf("") }
    var playerBirthday by remember { mutableStateOf<LocalDate?>(null) }
    var playerContractStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var playerContractEndDate by remember { mutableStateOf<LocalDate?>(null) }

    var showBirthdayPicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val playerState by viewModel.selectedPlayer.collectAsState()

    LaunchedEffect(playerId) {
        if (isEditing) {
            viewModel.loadPlayer(playerId!!.toInt())
        } else {
            viewModel.resetSelectedPlayer()
        }
    }

    LaunchedEffect(playerState) {
        if (isEditing) {
            playerState?.let {
                // ★★★ エラー箇所を修正 ★★★
                playerName = it.playerName ?: ""
                it.playerBirthday?.let { dateStr ->
                    playerBirthday = try { LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME) } catch (e: Exception) { null }
                }
                it.playerContractStartDate?.let { dateStr ->
                    playerContractStartDate = try { LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME) } catch (e: Exception) { null }
                }
                it.playerContractEndDate?.let { dateStr ->
                    playerContractEndDate = try { LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME) } catch (e: Exception) { null }
                }
            }
        } else {
            playerName = ""
            playerBirthday = null
            playerContractStartDate = null
            playerContractEndDate = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "選手情報の編集" else "選手の追加") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("選手名") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Birthday Picker
            DatePickerField(
                label = "誕生日",
                selectedDate = playerBirthday,
                onClick = { showBirthdayPicker = true }
            )
            if (showBirthdayPicker) {
                DatePickerDialog(
                    initialDate = playerBirthday,
                    onDateSelected = { playerBirthday = it },
                    onDismiss = { showBirthdayPicker = false }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contract Start Date Picker
            DatePickerField(
                label = "契約開始日",
                selectedDate = playerContractStartDate,
                onClick = { showStartDatePicker = true }
            )
            if (showStartDatePicker) {
                DatePickerDialog(
                    initialDate = playerContractStartDate,
                    onDateSelected = { playerContractStartDate = it },
                    onDismiss = { showStartDatePicker = false }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contract End Date Picker
            DatePickerField(
                label = "契約終了日",
                selectedDate = playerContractEndDate,
                onClick = { showEndDatePicker = true }
            )
            if (showEndDatePicker) {
                DatePickerDialog(
                    initialDate = playerContractEndDate,
                    onDateSelected = { playerContractEndDate = it },
                    onDismiss = { showEndDatePicker = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val formatter = DateTimeFormatter.ISO_DATE_TIME
                    viewModel.savePlayer(
                        playerName = playerName,
                        birthday = playerBirthday?.atStartOfDay(ZoneId.systemDefault())?.format(formatter),
                        contractStartDate = playerContractStartDate?.atStartOfDay(ZoneId.systemDefault())?.format(formatter),
                        contractEndDate = playerContractEndDate?.atStartOfDay(ZoneId.systemDefault())?.format(formatter)
                    ) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除")
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("選手の削除") },
                text = { Text("本当にこの選手を削除しますか？この操作は元に戻せません。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deletePlayer {
                                navController.popBackStack()
                            }
                            showDeleteDialog = false
                        }
                    ) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false }
                    ) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}

@Composable
private fun DatePickerField(label: String, selectedDate: LocalDate?, onClick: () -> Unit) {
    OutlinedTextField(
        value = selectedDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "",
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        readOnly = true,
        enabled = false, // Make it non-editable but clickable
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: Instant.now().toEpochMilli()
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}