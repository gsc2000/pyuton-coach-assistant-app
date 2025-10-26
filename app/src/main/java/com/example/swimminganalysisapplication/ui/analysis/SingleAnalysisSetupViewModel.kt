package com.example.swimminganalysisapplication.ui.analysis

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.JobRequest
import com.example.swimminganalysisapplication.data.remote.model.Player
import com.example.swimminganalysisapplication.data.remote.model.PlayerCreate
import com.example.swimminganalysisapplication.data.remote.model.VideoCreate
import com.example.swimminganalysisapplication.data.storage.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SingleAnalysisSetupViewModel(
    private val repository: SwimmingRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {
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

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var searchJob: Job? = null

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
            delay(300) // Debounce
            loadPlayers(text)
        }
    }

    fun onPlayerSelected(player: Player) {
        selectedPlayer = player
        playerSearchText = player.playerName ?: ""
        players = emptyList()
    }

    fun onCommentChange(newComment: String) {
        comment = newComment
    }

    private fun loadPlayers(query: String? = null) {
        viewModelScope.launch {
            isSearching = true
            try {
                players = repository.getPlayers(query) ?: emptyList()
            } catch (e: Exception) {
                errorMessage = "選手の読み込みに失敗しました: ${e.message}"
            } finally {
                isSearching = false
            }
        }
    }

    fun addNewPlayer(playerName: String) {
        viewModelScope.launch {
            try {
                val userId = userPreferences.userId.first()
                if (userId == null) {
                    errorMessage = "ユーザー情報が取得できませんでした。再度ログインしてください。"
                    return@launch
                }
                val newPlayer = repository.createPlayer(PlayerCreate(playerName = playerName, userId = userId))
                if (newPlayer != null) {
                    selectedPlayer = newPlayer
                    playerSearchText = newPlayer.playerName ?: ""
                    players = emptyList()
                }
            } catch (e: Exception) {
                errorMessage = "選手の追加に失敗しました: ${e.message}"
            }
        }
    }

    fun onNavigateToAnalysisListClicked() {
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToAnalysisList)
        }
    }

    fun startAnalysis(context: Context) {
        if (videoUri == null) {
            errorMessage = "動画を選択してください。"
            return
        }
        if (selectedPlayer == null) {
            errorMessage = "選手を選択してください。"
            return
        }

        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading("解析準備中...")
            errorMessage = null

            try {
                val userId = userPreferences.userId.first() ?: throw Exception("ユーザーIDが取得できませんでした。")

                // Step 1: Upload Video
                _uiState.value = AnalysisUiState.Loading("動画をアップロード中...")
                val videoFile = videoUri?.let { uri ->
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.mp4")
                        file.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                        file
                    }
                } ?: throw Exception("動画ファイルの準備に失敗しました。")

                val videoRequestBody = videoFile.asRequestBody("video/mp4".toMediaTypeOrNull())
                val videoPart = MultipartBody.Part.createFormData("file", videoFile.name, videoRequestBody)

                val uploadResponse = repository.uploadVideo(videoPart)
                    ?: throw Exception("動画のアップロードに失敗しました。サーバーからの応答がありません。")
                Log.d("SingleAnalysisSetupVM", "Video uploaded. File ID: ${uploadResponse.id}")

//                // Step 2: Create Video Object (Temporarily Skipped)
//                val videoToCreate = VideoCreate(
//                    videoTitle = comment.ifBlank { "新しいビデオ" }, // commentが空ならデフォルトタイトル
//                    userId = userId,
//                    videoUuid = uploadResponse.id
//                )
//                val createdVideo = repository.createVideo(videoToCreate)
//                    ?: throw Exception("Videoオブジェクトの作成に失敗しました。")
//                Log.d("SingleAnalysisSetupVM", "Video object created. ID: ${createdVideo.videoId}")

                // Step 3: Start Inference Job
                _uiState.value = AnalysisUiState.Loading("解析ジョブを開始中...")
                val jobRequest = JobRequest(fileId = uploadResponse.id)
                val jobResponse = repository.startInferenceJob(jobRequest)
                    ?: throw Exception("解析ジョブの作成に失敗しました。")
                Log.d("SingleAnalysisSetupVM", "Inference job started. Job ID: ${jobResponse.jobId}")

                // Step 4: Poll Job Status
                _uiState.value = AnalysisUiState.Loading("解析中...")
                var jobStatus = repository.getJobStatus(jobResponse.jobId)
                while (jobStatus?.status != "completed" && jobStatus?.status != "failed") {
                    delay(5000) // 5秒待機
                    jobStatus = repository.getJobStatus(jobResponse.jobId)
                    Log.d("SingleAnalysisSetupVM", "Polling job status: ${jobStatus?.status}")
                }

                // Step 5 & 6: Handle Completion
                when (jobStatus?.status) {
                    "completed" -> {
                        _uiState.value = AnalysisUiState.Success("解析が完了しました！")
                        // ここで解析結果画面への遷移などの処理を呼び出す
                        // _navigationEvent.emit(NavigationEvent.NavigateToResultScreen(jobResponse.jobId))
                    }
                    "failed" -> {
                        throw Exception("解析に失敗しました。詳細: ${jobStatus.detail}")
                    }
                    else -> {
                        throw Exception("不明な解析ステータスです: ${jobStatus?.status}")
                    }
                }

            } catch (e: Exception) {
                errorMessage = e.message ?: "不明なエラーが発生しました。"
                _uiState.value = AnalysisUiState.Error(errorMessage!!)
                Log.e("SingleAnalysisSetupVM", "Analysis failed", e)
            } finally {
                // ローディング状態は各ステップで管理されるため、ここではIdleに戻さない
            }
        }
    }
}

sealed class NavigationEvent {
    object NavigateToAnalysisList : NavigationEvent()
    // data class NavigateToResultScreen(val jobId: String) : NavigationEvent()
}

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    data class Loading(val message: String) : AnalysisUiState()
    data class Success(val message: String) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}
