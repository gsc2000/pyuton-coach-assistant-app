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

    // UIに公開する状態をCommentThreadのリストに変更
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
                // 現在の展開状態を保持する
                val oldExpansionState = _commentThreads.value.associateBy(
                    keySelector = { it.parent.chatId },
                    valueTransform = { it.isExpanded }
                )

                val chatsForMenu = repository.getMenuChats(menuId) ?: emptyList()
                Log.d(TAG, "repository.getMenuChats returned ${chatsForMenu.size} chats.")

                val parentChats = chatsForMenu.filter { it.chatThreadId == null }
                val repliesByThreadId = chatsForMenu.filter { it.chatThreadId != null }
                    .groupBy { it.chatThreadId!! }
                Log.d(
                    TAG,
                    "Found ${parentChats.size} parent chats and ${repliesByThreadId.size} groups of replies."
                )

                // グルーピングロジックを修正
                // 親コメントが見つかった場合のみスレッドを構築するシンプルなロジックに変更。
                val newThreads = parentChats.map { parent ->
                    val replies = repliesByThreadId[parent.chatId] ?: emptyList()
                    // 以前の展開状態を復元する
                    val wasExpanded = oldExpansionState[parent.chatId] ?: false
                    CommentThread(
                        parent = parent,
                        replies = replies.sortedBy { it.chatSentAt },
                        isExpanded = wasExpanded // 状態を適用
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

    /**
     * 指定された親コメントIDのスレッド展開状態を切り替える。
     * UI（親コメントのタップイベント）から呼び出す。
     */
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

                val newChatRequest = ChatCreate(
                    chatContent = content,
                    userId = currentUser.userId,
                    menuId = menuId.toIntOrNull(),
                    chatThreadId = parentChatId
                )

                val createdChat = repository.createChat(newChatRequest)

                if (createdChat != null) {
                    // 投稿成功後、リストを再読み込みして最新の状態を反映する
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