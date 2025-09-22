package com.example.swimminganalysisapplication.ui.menucomments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Chat
import com.example.swimminganalysisapplication.data.remote.model.ChatThread
import com.example.swimminganalysisapplication.data.storage.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// UIでスレッド形式のコメントを表示するためのデータクラス
data class DisplayThread(
    val parent: Chat,
    val replies: List<Chat>
)

class MenuCommentsViewModel(
    private val repository: SwimmingRepository,
    private val userPreferences: UserPreferences,
    private val menuId: String // ★ Int から String に変更
) : ViewModel() {

    private val _threads = MutableStateFlow<List<DisplayThread>>(emptyList())
    val threads: StateFlow<List<DisplayThread>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadComments()
    }

    fun loadComments() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // repository の関数が String 型の menuId を受け付けるか確認が必要
                val chatThreads = repository.getMenuChatThreads(menuId) ?: emptyList() // menuId を String で渡す
                val allChats = repository.getMenuChats(menuId) ?: emptyList()       // menuId を String で渡す

                if (chatThreads.isNotEmpty() && allChats.isNotEmpty()) {
                    val chatsByThreadId = allChats.groupBy { it.chatThreadId }

                    val displayThreads = chatThreads.mapNotNull { thread ->
                        val parentChat = allChats.find { it.chatId == thread.chatId }
                        val replies = chatsByThreadId[thread.chatThreadId]
                            ?.filter { it.chatId != thread.chatId }
                            ?: emptyList()

                        parentChat?.let {
                            DisplayThread(parent = it, replies = replies.sortedBy { r -> r.chatSentAt })
                        }
                    }.sortedByDescending { it.parent.chatSentAt }

                    _threads.value = displayThreads
                } else {
                    _threads.value = emptyList()
                }

            } catch (e: Exception) {
                _error.value = "コメントの読み込みに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // postNewThread と postReply 内の threadId (Int) の扱いは、
    // Chat データクラスや API の仕様に依存するため、ここでは変更していません。
    // menuId が String になったことで、これらのメソッドに直接影響はないはずです。

    fun postNewThread(content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val currentUserId = userPreferences.userId.firstOrNull()
                if (currentUserId == null) {
                    _error.value = "ユーザー情報が取得できませんでした。"
                    _isLoading.value = false
                    return@launch
                }

                val newChatRequest = Chat(
                    chatId = 0,
                    chatContent = content,
                    chatSentAt = "",
                    chatThreadId = null,
                    userId = currentUserId
                )
                val newChat = repository.createChat(newChatRequest)

                if (newChat != null) {
                    val newChatThreadRequest = ChatThread(
                        chatThreadId = 0,
                        chatThreadTitle = null,
                        chatThreadCreateAt = "",
                        chatId = newChat.chatId
                        // menuId を ChatThread に含めるかはAPI仕様による
                    )
                    repository.createChatThread(newChatThreadRequest)
                    loadComments()
                } else {
                    _error.value = "スレッドの作成に失敗しました。"
                }
            } catch (e: Exception) {
                _error.value = "投稿に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun postReply(threadId: Int, content: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val currentUserId = userPreferences.userId.firstOrNull()
                if (currentUserId == null) {
                    _error.value = "ユーザー情報が取得できませんでした。"
                    _isLoading.value = false
                    return@launch
                }
                val newChatRequest = Chat(
                    chatId = 0,
                    chatContent = content,
                    chatSentAt = "",
                    chatThreadId = threadId,
                    userId = currentUserId
                )
                repository.createChat(newChatRequest)
                loadComments()
            } catch (e: Exception) {
                _error.value = "返信に失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
