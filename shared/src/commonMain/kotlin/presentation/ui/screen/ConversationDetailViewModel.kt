package presentation.ui.screen

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import data.repository.ConversationRepository
import data.repository.AIRepositoryImpl
import domain.manager.ConversationManager
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import domain.model.Conversation
import domain.model.ChatMessage
import domain.model.Status
import kotlinx.coroutines.launch

/**
 * 对话详情页面的ViewModel
 * 管理特定对话的消息显示和发送功能
 */
class ConversationDetailViewModel(
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    
    private val _uiState = mutableStateOf(ConversationDetailUiState())
    val uiState: State<ConversationDetailUiState> = _uiState
    
    private val conversationManager = ConversationManager(conversationRepository)
    private val aiRepository = AIRepositoryImpl()
    
    private var currentConversationId: String? = null
    
    /**
     * 加载对话和消息
     */
    fun loadConversation(conversationId: String) {
        if (currentConversationId == conversationId) return
        
        currentConversationId = conversationId
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                // 获取对话信息
                val conversation = conversationRepository.getConversationById(conversationId)
                if (conversation == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "对话不存在"
                    )
                    return@launch
                }
                
                // 获取消息列表
                val messages = conversationRepository.getConversationMessages(conversationId)
                
                _uiState.value = _uiState.value.copy(
                    conversation = conversation,
                    messages = messages,
                    isLoading = false,
                    error = null
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载对话失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 发送消息
     * 处理用户消息发送和AI回复生成
     */
    fun sendMessage(text: String) {
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            try {
                // 创建用户消息
                val userMessage = ChatMessage.createUserMessage(
                    conversationId = conversationId,
                    content = text
                )
                
                // 添加用户消息到UI
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + userMessage
                )
                
                // 保存用户消息到数据库
                conversationManager.saveMessage(userMessage)
                
                // 创建AI消息占位符
                val aiMessage = ChatMessage.createAiMessage(
                    conversationId = conversationId,
                    content = "",
                    isLoading = true
                )
                
                // 添加AI消息占位符到UI
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiMessage
                )
                
                // 生成AI回复
                val aiResponse = when (val result = aiRepository.generate(text)) {
                    is Status.Success -> result.data
                    is Status.Error -> result.message
                    else -> "生成回复失败"
                }
                
                // 更新AI消息
                val updatedAiMessage = aiMessage.copy(
                    content = aiResponse,
                    isLoading = false
                )
                
                // 更新UI中的AI消息
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.map { message ->
                        if (message.id == aiMessage.id) updatedAiMessage else message
                    }
                )
                
                // 保存AI消息到数据库
                conversationManager.saveMessage(updatedAiMessage)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "发送消息失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 重置状态
     */
    fun resetStatus() {
        _uiState.value = _uiState.value.copy(status = Status.Idle)
    }
}

/**
 * 对话详情界面的UI状态
 */
data class ConversationDetailUiState(
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val status: Status = Status.Idle,
    val error: String? = null
)