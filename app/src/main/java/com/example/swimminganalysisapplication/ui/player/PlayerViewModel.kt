package com.example.swimminganalysisapplication.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.data.remote.model.PlayerCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: SwimmingRepository) : ViewModel() {

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedPlayer = MutableStateFlow<Player?>(null)
    val selectedPlayer: StateFlow<Player?> = _selectedPlayer.asStateFlow()

    init {
        loadPlayers()
    }

    fun loadPlayers() {
        viewModelScope.launch {
            try {
                _players.value = repository.getPlayers() ?: emptyList()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "選手の読み込みに失敗しました: ${e.message}"
            }
        }
    }

    fun loadPlayer(playerId: Int) {
        viewModelScope.launch {
            try {
                _selectedPlayer.value = repository.getPlayer(playerId)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "選手情報の読み込みに失敗しました: ${e.message}"
            }
        }
    }

    fun savePlayer(
        playerName: String,
        birthday: String?,
        contractStartDate: String?,
        contractEndDate: String?,
        onSaveFinished: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val playerToSave = _selectedPlayer.value?.copy(
                    playerName = playerName,
                    playerBirthday = birthday,
                    playerContractStartDate = contractStartDate,
                    playerContractEndDate = contractEndDate
                )
                    ?: Player(
                        playerId = 0,
                        userId = 0, // Assuming default or to-be-filled user ID
                        playerName = playerName,
                        playerBirthday = birthday,
                        playerContractStartDate = contractStartDate,
                        playerContractEndDate = contractEndDate,
                        playerCreateAt = "",
                        playerUpdateAt = ""
                    )

                if (playerToSave.playerId == 0) {
                    repository.createPlayer(PlayerCreate(playerName = playerToSave.playerName))
                } else {
                    repository.updatePlayer(playerToSave.playerId, playerToSave)
                }
                loadPlayers() // Refresh the list
                onSaveFinished()
            } catch (e: Exception) {
                _errorMessage.value = "選手の保存に失敗しました: ${e.message}"
            }
        }
    }

    fun resetSelectedPlayer() {
        _selectedPlayer.value = null
    }

    fun deletePlayer(onDeleteFinished: () -> Unit) {
        viewModelScope.launch {
            _selectedPlayer.value?.let { player ->
                try {
                    repository.deletePlayer(player.playerId)
                    loadPlayers() // Refresh the list
                    onDeleteFinished()
                } catch (e: Exception) {
                    _errorMessage.value = "選手の削除に失敗しました: ${e.message}"
                }
            }
        }
    }
}
