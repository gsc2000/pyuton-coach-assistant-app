package com.example.swimminganalysisapplication.ui.analysis

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Analysis
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.data.remote.model.PlayerCreate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SingleAnalysisSetupViewModel(private val repository: SwimmingRepository) : ViewModel() {
    var videoUri by mutableStateOf<Uri?>(null)
        private set

    var date by mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        private set

    var selectedPlayer by mutableStateOf<Player?>(null)
        private set

    var comment by mutableStateOf("")
        private set

    var playerSearchText by mutableStateOf("")
        private set

    var players by mutableStateOf<List<Player>>(emptyList())
        private set

    var isSearching by mutableStateOf(false)
        private set

    private var searchJob: Job? = null

    private val _analysisResult = MutableSharedFlow<Analysis>()
    val analysisResult = _analysisResult.asSharedFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadPlayers()
    }

    fun onVideoSelected(uri: Uri?) {
        videoUri = uri
    }

    fun onDateChange(newDate: String) {
        date = newDate
    }

    fun onPlayerSearchTextChange(text: String) {
        playerSearchText = text
        isSearching = true
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(1000) // Debounce
            loadPlayers(text)
        }
    }

    fun onPlayerSelected(player: Player) {
        selectedPlayer = player
        playerSearchText = player.playerName
        players = emptyList()
    }

    fun onCommentChange(newComment: String) {
        comment = newComment
    }

    private fun loadPlayers(query: String? = null) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
            // Mock data for testing without backend
            val mockPlayers = listOf(
                Player(playerId = 1, playerName = "田中 太郎", userId = 1, playerBirthday = "2000-01-01", playerContractStartDate = now, playerContractEndDate = null, playerCreateAt = now, playerUpdateAt = now),
                Player(playerId = 2, playerName = "鈴木 一郎", userId = 1, playerBirthday = "2001-02-02", playerContractStartDate = now, playerContractEndDate = null, playerCreateAt = now, playerUpdateAt = now),
                Player(playerId = 3, playerName = "佐藤 花子", userId = 1, playerBirthday = "2002-03-03", playerContractStartDate = now, playerContractEndDate = null, playerCreateAt = now, playerUpdateAt = now)
            )
            players = if (query.isNullOrBlank()) {
                mockPlayers
            } else {
                mockPlayers.filter { it.playerName.contains(query, ignoreCase = true) }
            }
            isSearching = false
            /*
            try {
                players = repository.getPlayers(query) ?: emptyList()
            } catch (e: Exception) {
                // Handle error
            } finally {
                isSearching = false
            }
            */
        }
    }

    fun addNewPlayer(playerName: String) {
        viewModelScope.launch {
            try {
                val newPlayer = repository.createPlayer(PlayerCreate(playerName = playerName))
                if (newPlayer != null) {
                    selectedPlayer = newPlayer
                    playerSearchText = newPlayer.playerName
                    players = emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onNavigateToAnalysisListClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToAnalysisList)
        }
    }

    fun startAnalysis(context: Context) {
        viewModelScope.launch {
            val videoFile = videoUri?.let { uri ->
                context.contentResolver.openInputStream(uri)?.let { inputStream ->
                    val file = File(context.cacheDir, "upload.mp4")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    file
                }
            }

            if (videoFile == null || selectedPlayer == null) {
                // Handle error: video or player not selected
                return@launch
            }

            val dateBody = date.toRequestBody("text/plain".toMediaTypeOrNull())
            val playerIdBody = selectedPlayer!!.playerId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val commentBody = comment.toRequestBody("text/plain".toMediaTypeOrNull())
            val videoRequestBody = videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
            val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, videoRequestBody)

            try {
                val result = repository.uploadSingleAnalysis(dateBody, playerIdBody, commentBody, videoPart)
                result?.let {
                    _analysisResult.emit(it)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

sealed class NavigationEvent {
    object NavigateToAnalysisList : NavigationEvent()
}
