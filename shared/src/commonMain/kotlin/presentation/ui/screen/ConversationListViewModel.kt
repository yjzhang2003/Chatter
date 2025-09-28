package presentation.ui.screen

import data.repository.ConversationRepository
import domain.model.Conversation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 对话列表ViewModel
 * 负责管理对话列表的状态和业务逻辑
 */
class ConversationListViewModel(
    private val conversationRepository: ConversationRepository
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            conversationRepository.getAllConversationsFlow().collectLatest { conversations: List<Conversation> ->
                _uiState.value = _uiState.value.copy(
                    conversations = conversations,
                    filteredConversations = filterConversations(conversations, _uiState.value.searchQuery),
                    isLoading = false
                )
            }
        }
    }



    /**
     * 搜索对话
     */
    fun searchConversations(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredConversations = filterConversations(_uiState.value.conversations, query)
        )
    }

    /**
     * 过滤对话列表
     */
    private fun filterConversations(conversations: List<Conversation>, query: String): List<Conversation> {
        if (query.isBlank()) return conversations
        
        return conversations.filter { conversation ->
            conversation.getDisplayTitle().contains(query, ignoreCase = true) ||
            conversation.lastMessage.contains(query, ignoreCase = true)
        }
    }

    /**
     * 创建新对话
     */
    fun createNewConversation(title: String? = null) {
        viewModelScope.launch {
            conversationRepository.createConversation(title)
        }
    }

    /**
     * 删除对话
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 对话列表UI状态
 */
data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList(),
    val filteredConversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // 返回当前显示的对话列表（搜索结果或全部对话）
    val displayConversations: List<Conversation>
        get() = if (searchQuery.isBlank()) conversations else filteredConversations
}