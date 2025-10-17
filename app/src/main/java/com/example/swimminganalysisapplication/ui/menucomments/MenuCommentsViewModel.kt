package com.example.swimminganalysisapplication.ui.menucomments

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swimminganalysisapplication.data.SwimmingRepository
import com.example.swimminganalysisapplication.data.remote.model.Chat
import com.example.swimminganalysisapplication.data.remote.model.ChatCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DisplayThread(
    val parent: Chat,
    val replies: List<Chat>
)

class MenuCommentsViewModel(
    private val repository: SwimmingRepository,
    private val menuId: String
) : ViewModel() {

    private val _threads = MutableStateFlow<List<DisplayThread>>(emptyList())
    val threads: StateFlow<List<DisplayThread>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        private const val TAG = "MenuCommentsViewModel"
    }

    init {
        loadComments()
    }

    fun loadComments() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d(TAG, "loadComments started for menuId: $menuId")
            try {
                val chatsForMenu = repository.getMenuChats(menuId) ?: emptyList()
                Log.d(TAG, "repository.getMenuChats returned ${chatsForMenu.size} chats.")
                Log.d(TAG, "Raw chatsForMenu: $chatsForMenu")


                val parentChats = chatsForMenu.filter { it.chatThreadId == null }
                val repliesByThreadId = chatsForMenu.filter { it.chatThreadId != null }.groupBy { it.chatThreadId!! }

                Log.d(TAG, "Found ${parentChats.size} parent chats.")
                Log.d(TAG, "Found ${repliesByThreadId.size} groups of replies.")

                val displayThreads: List<DisplayThread>

                // A. 親コメントが存在する場合 (Happy Path)
                if (parentChats.isNotEmpty()) {
                    Log.d(TAG, "Processing Happy Path (A)")
                    displayThreads = parentChats.map { parent ->
                        val replies = repliesByThreadId[parent.chatId] ?: emptyList()
                        DisplayThread(parent = parent, replies = replies.sortedBy { it.chatSentAt })
                    }.sortedByDescending { it.parent.chatSentAt }
                }
                // B. 親コメントは無いが、何らかのコメント（返信のみ）は存在する場合 (Fallback)
                else if (chatsForMenu.isNotEmpty()) {
                    Log.d(TAG, "Processing Fallback Path (B)")
                    // 全てのコメントを「返信のない親コメント」として表示する
                    displayThreads = chatsForMenu.map { chat ->
                        DisplayThread(parent = chat, replies = emptyList())
                    }.sortedByDescending { it.parent.chatSentAt }
                }
                // C. コメントが一つも無い場合
                else {
                    Log.d(TAG, "Processing No Comments Path (C)")
                    displayThreads = emptyList()
                }

                Log.d(TAG, "Final displayThreads count: ${displayThreads.size}")
                _threads.value = displayThreads

            } catch (e: Exception) {
                Log.e(TAG, "Error loading comments", e)
                _error.value = "コメントの読み込みに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "loadComments finished.")
            }
        }
    }

    fun postComment(content: String, parentChatId: Int?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val currentUser = repository.getMe()
                if (currentUser == null) {
                    _error.value = "ユーザー情報が取得できません。再度ログインしてください。"
                    _isLoading.value = false
                    return@launch
                }

                val newChatRequest = ChatCreate(
                    chatContent = content,
                    userId = currentUser.userId,
                    menuId = menuId.toIntOrNull(),
                    chatThreadId = parentChatId
                )

                val createdChat = repository.createChat(newChatRequest)

                if (createdChat != null) {
                    // 投稿成功後、リストを再読み込み
                    loadComments()
                } else {
                    _error.value = "投稿に失敗しました。"
                }

            } catch (e: Exception) {
                _error.value = "投稿中にエラーが発生しました: ${e.message}"
            } finally {
                // isLoadingはloadComments()のfinallyでfalseにされる
            }
        }
    }
}
