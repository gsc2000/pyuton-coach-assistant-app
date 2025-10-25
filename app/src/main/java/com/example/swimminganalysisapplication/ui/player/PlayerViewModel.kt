package com.example.swimminganalysisapplication.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.data.remote.model.PlayerCreate
import com.example.swimminganalysisapplication.data.storage.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val repository: SwimmingRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // players: UIが購読できるようにStateFlowで公開
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    // selectedPlayer: UIが購読できるようにStateFlowで公開
    private val _selectedPlayer = MutableStateFlow<Player?>(null)
    val selectedPlayer: StateFlow<Player?> = _selectedPlayer.asStateFlow()

    // errorMessage: UIが購読できるようにStateFlowで公開
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadPlayers()
    }

    fun loadPlayers() {
        viewModelScope.launch {
            try {
                _players.value = repository.getPlayers() ?: emptyList()
            } catch (e: Exception) {
                _errorMessage.value = "選手の読み込みに失敗しました: ${e.message}"
            }
        }
    }

    fun loadPlayer(playerId: Int) {
        viewModelScope.launch {
            try {
                _selectedPlayer.value = repository.getPlayer(playerId)
            } catch (e: Exception) {
                _errorMessage.value = "選手情報の読み込みに失敗しました: ${e.message}"
            }
        }
    }

    fun resetSelectedPlayer() {
        _selectedPlayer.value = null
    }

    fun savePlayer(
        playerName: String,
        birthday: String?,
        contractStartDate: String?,
        contractEndDate: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = userPreferences.userId.first()
                if (userId == null) {
                    _errorMessage.value = "ユーザー情報が取得できませんでした。再度ログインしてください。"
                    return@launch
                }

                val currentPlayer = _selectedPlayer.value
                if (currentPlayer != null) {
                    // 更新
                    val updatedPlayer = currentPlayer.copy(
                        playerName = playerName,
                        playerBirthday = birthday,
                        playerContractStartDate = contractStartDate,
                        playerContractEndDate = contractEndDate
                    )
                    repository.updatePlayer(currentPlayer.playerId, updatedPlayer)
                } else {
                    // 新規作成
                    val newPlayer = PlayerCreate(
                        playerName = playerName,
                        playerBirthday = birthday,
                        playerContractStartDate = contractStartDate,
                        playerContractEndDate = contractEndDate,
                        userId = userId
                    )
                    repository.createPlayer(newPlayer)
                }
                loadPlayers() // 一覧を更新
                onSuccess()   // 成功したら画面を閉じる
            } catch (e: Exception) {
                _errorMessage.value = "選手の保存に失敗しました: ${e.message}"
            }
        }
    }

    fun deletePlayer(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _selectedPlayer.value?.let { player ->
                try {
                    repository.deletePlayer(player.playerId)
                    onSuccess()
                    loadPlayers() // 一覧を更新
                } catch (e: Exception) {
                    _errorMessage.value = "選手の削除に失敗しました: ${e.message}"
                }
            }
        }
    }
}
