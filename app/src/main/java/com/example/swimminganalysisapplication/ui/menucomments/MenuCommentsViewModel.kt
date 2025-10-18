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

/**
 * UIでスレッドを表示するためのデータクラス。
 * @param parent 親コメント
 * @param replies 返信コメントのリスト
 * @param isExpanded スレッドが展開されているかどうかの状態
 */
data class CommentThread(
    val parent: Chat,
    val replies: List<Chat>,
    val isExpanded: Boolean = false
)

class MenuCommentsViewModel(
    private val repository: SwimmingRepository,
    private val menuId: String
) : ViewModel() {

    private val _commentThreads = MutableStateFlow<List<CommentThread>>(emptyList())
    val commentThreads: StateFlow<List<CommentThread>> = _commentThreads.asStateFlow()

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
                val oldExpansionState = _commentThreads.value.associateBy(
                    keySelector = { it.parent.chatId },
                    valueTransform = { it.isExpanded }
                )

                val chatsForMenu = repository.getMenuChats(menuId) ?: emptyList()
                Log.d(TAG, "repository.getMenuChats returned ${chatsForMenu.size} chats.")

                // ★★★ ここから修正 ★★★
                val parentChats = chatsForMenu.filter { it.parentChatId == null }
                val repliesByThreadId = chatsForMenu.filter { it.parentChatId != null }
                    .groupBy { it.parentChatId!! }
                // ★★★ ここまで修正 ★★★

                Log.d(
                    TAG,
                    "Found ${parentChats.size} parent chats and ${repliesByThreadId.size} groups of replies."
                )

                val newThreads = parentChats.map { parent ->
                    val replies = repliesByThreadId[parent.chatId] ?: emptyList()
                    val wasExpanded = oldExpansionState[parent.chatId] ?: false
                    CommentThread(
                        parent = parent,
                        replies = replies.sortedBy { it.chatSentAt },
                        isExpanded = wasExpanded
                    )
                }.sortedByDescending { it.parent.chatSentAt }

                Log.d(TAG, "Final commentThreads count: ${newThreads.size}")
                _commentThreads.value = newThreads

            } catch (e: Exception) {
                Log.e(TAG, "Error loading comments", e)
                _error.value = "コメントの読み込みに失敗しました: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "loadComments finished.")
            }
        }
    }

    fun toggleThreadExpansion(parentChatId: Int) {
        Log.d(TAG, "Toggling expansion for thread with parent ID: $parentChatId")
        val currentThreads = _commentThreads.value
        val newThreads = currentThreads.map { thread ->
            if (thread.parent.chatId == parentChatId) {
                thread.copy(isExpanded = !thread.isExpanded)
            } else {
                thread
            }
        }
        _commentThreads.value = newThreads
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

                // ★★★ ここから修正 ★★★
                val newChatRequest = ChatCreate(
                    chatContent = content,
                    userId = currentUser.userId,
                    menuId = menuId.toIntOrNull(),
                    parentChatId = parentChatId
                )
                // ★★★ ここまで修正 ★★★

                val createdChat = repository.createChat(newChatRequest)

                if (createdChat != null) {
                    loadComments()
                } else {
                    _error.value = "投稿に失敗しました。"
                }

            } catch (e: Exception) {
                _error.value = "投稿中にエラーが発生しました: ${e.message}"
            } finally {
                // isLoading is handled in loadComments()
            }
        }
    }
}