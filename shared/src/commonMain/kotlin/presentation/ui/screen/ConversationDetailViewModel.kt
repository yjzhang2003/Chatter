package presentation.ui.screen

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import data.repository.ConversationRepository
import domain.repository.AgentRepository
import data.repository.AIRepositoryImpl
import domain.manager.ConversationManager
import domain.manager.MemoryManager
import domain.usecase.MemoryUseCase
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import domain.model.Conversation
import domain.model.ChatMessage
import domain.model.MessageSender
import domain.model.Status
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import io.ktor.util.encodeBase64

/**
 * 对话详情页面的ViewModel
 * 管理特定对话的消息显示和发送功能，支持智能体集成
 */
class ConversationDetailViewModel(
    private val conversationRepository: ConversationRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {
    
    private val _uiState = mutableStateOf(ConversationDetailUiState())
    val uiState: State<ConversationDetailUiState> = _uiState
    
    private val conversationManager = ConversationManager(conversationRepository)
    private val aiRepository = AIRepositoryImpl()
    
    // 记忆系统组件
    private val memoryManager = MemoryManager(agentRepository)
    private val memoryUseCase = MemoryUseCase(memoryManager, agentRepository, conversationRepository)
    
    // 新增：当前模型是否支持多模态的状态，供UI订阅
    private val _supportsMultimodal = mutableStateOf(false)
    val supportsMultimodalState: State<Boolean> = _supportsMultimodal
    private var currentConversationId: String? = null
    
    /**
     * 加载对话和消息
     */
    fun loadConversation(conversationId: String) {
        // 如果是相同对话且已经加载完成，则不重复加载
        if (currentConversationId == conversationId && 
            _uiState.value.conversation != null && 
            !_uiState.value.isLoading) {
            return
        }
        
        // 清理之前的状态
        clearState()
        
        currentConversationId = conversationId
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            try {
                // 确保ConversationManager切换到当前对话
                conversationManager.switchToConversation(conversationId)
                
                // 获取对话信息
                val conversation = conversationRepository.getConversationById(conversationId)
                println("Debug: loadConversation - conversationId: $conversationId")
                println("Debug: loadConversation - 获取到的对话信息: ${conversation?.title}, agentId: ${conversation?.agentId}")
                
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
    fun sendMessage(text: String, images: List<ByteArray> = emptyList()) {
        val conversationId = currentConversationId ?: return
        // 防止重复发送
        if (_uiState.value.status is Status.Loading) {
            return
        }
        viewModelScope.launch {
            try {
                // 设置加载状态
                _uiState.value = _uiState.value.copy(status = Status.Loading)
                // 切换到当前对话
                conversationManager.switchToConversation(conversationId)
                // 将图片转为Base64字符串以持久化到消息中
                val base64Images = images.map { it.encodeBase64() }
                // 创建用户消息（包含图片）
                val userMessage = ChatMessage.createUserMessage(
                    conversationId = conversationId,
                    content = text,
                    images = base64Images
                )
                // 添加用户消息到UI并保存
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + userMessage
                )
                conversationManager.saveMessage(userMessage)
                // 记忆处理
                val conversation = _uiState.value.conversation
                val agentId = conversation?.agentId
                if (agentId != null) {
                    memoryUseCase.processMessageForMemory(agentId, userMessage, conversationId)
                }
                // 创建AI消息占位符
                val aiMessage = ChatMessage.createAiMessage(
                    conversationId = conversationId,
                    content = "",
                    isLoading = true
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiMessage
                )
                // 构建上下文（系统提示+记忆+历史）
                val contextMessages = mutableListOf<ChatMessage>()
                val currentConversation = _uiState.value.conversation
                val agent = currentConversation?.agentId?.let { agentRepository.getAgentById(it) }
                if (agent != null && agent.systemPrompt.isNotBlank()) {
                    contextMessages.add(
                        ChatMessage.create(
                            conversationId = conversationId,
                            content = agent.systemPrompt,
                            sender = MessageSender.SYSTEM
                        )
                    )
                }
                val memoryContext = agent?.let {
                    memoryUseCase.generateMemoryEnhancedContext(
                        agentId = it.id,
                        conversationId = conversationId,
                        currentMessage = text
                    )
                } ?: ""
                if (memoryContext.isNotBlank()) {
                    contextMessages.add(
                        ChatMessage.create(
                            conversationId = conversationId,
                            content = memoryContext,
                            sender = MessageSender.SYSTEM
                        )
                    )
                }
                val historyMessages = conversationManager.getContextMessages(
                    currentPrompt = text,
                    useTokenOptimization = true
                )
                contextMessages.addAll(historyMessages)
                // 生成AI回复，传递图片与上下文
                val aiResponse = when (val result = aiRepository.generate(text, images, contextMessages)) {
                    is Status.Success -> result.data
                    is Status.Error -> result.message
                    else -> "生成回复失败"
                }
                // 更新AI消息
                val updatedAiMessage = aiMessage.copy(
                    content = aiResponse,
                    isLoading = false
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages.map { if (it.id == aiMessage.id) updatedAiMessage else it },
                    status = Status.Success("消息发送成功")
                )
                conversationManager.saveMessage(updatedAiMessage)
                if (agentId != null) {
                    memoryUseCase.processMessageForMemory(agentId, updatedAiMessage, conversationId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "发送消息失败: ${e.message}",
                    status = Status.Error(e.message ?: "发送失败")
                )
            }
        }
    }
    
    /**
     * 清理状态
     * 重置ViewModel状态，防止状态混乱和内存泄漏
     */
    private fun clearState() {
        _uiState.value = ConversationDetailUiState()
        currentConversationId = null
    }
    
    /**
     * ViewModel销毁时清理状态
     */
    override fun onCleared() {
        super.onCleared()
        clearState()
    }
    
    /**
     * 清理错误状态
     * 用于在导航时清理错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * 重置状态
     * 用于在导航时重置加载状态
     */
    fun resetStatus() {
        _uiState.value = _uiState.value.copy(status = Status.Idle)
    }
    
    /**
     * 刷新当前对话信息
     * 用于在对话信息更新后重新加载
     */
    fun refreshConversation() {
        val conversationId = currentConversationId ?: return
        println("Debug: refreshConversation - 刷新对话信息，conversationId: $conversationId")
        
        viewModelScope.launch {
            try {
                // 重新获取对话信息
                val conversation = conversationRepository.getConversationById(conversationId)
                println("Debug: refreshConversation - 刷新后的对话信息: ${conversation?.title}, agentId: ${conversation?.agentId}")
                
                if (conversation != null) {
                    _uiState.value = _uiState.value.copy(conversation = conversation)
                }
            } catch (e: Exception) {
                println("Debug: refreshConversation - 刷新失败: ${e.message}")
            }
        }
    }
    
    /**
     * 获取对话记忆摘要
     * @return 记忆摘要文本
     */
    fun getMemorySummary(): String {
        val conversationId = currentConversationId ?: return ""
        val agentId = _uiState.value.conversation?.agentId ?: return ""
        
        return try {
            // 这里需要使用runBlocking或者改为suspend函数
            // 为了简化，先返回空字符串，实际使用时需要在协程中调用
            ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 搜索记忆
     * @param query 搜索查询
     */
    fun searchMemories(query: String) {
        val agentId = _uiState.value.conversation?.agentId ?: return
        
        viewModelScope.launch {
            try {
                val memories = memoryUseCase.searchMemories(agentId, query)
                // 可以将搜索结果添加到UI状态中，这里暂时只打印日志
                println("Debug: 搜索到 ${memories.size} 条相关记忆")
                memories.forEach { memory ->
                    println("Debug: 记忆内容: ${memory.content.take(50)}...")
                }
            } catch (e: Exception) {
                println("Debug: 搜索记忆失败: ${e.message}")
            }
        }
    }
    
    /**
     * 更新记忆反馈
     * @param memoryId 记忆ID
     * @param isHelpful 是否有帮助
     */
    fun updateMemoryFeedback(memoryId: String, isHelpful: Boolean) {
        viewModelScope.launch {
            try {
                memoryUseCase.updateMemoryFeedback(memoryId, isHelpful)
                println("Debug: 记忆反馈更新成功")
            } catch (e: Exception) {
                println("Debug: 更新记忆反馈失败: ${e.message}")
            }
        }
    }

    /**
     * 检查当前模型是否支持多模态输入
     * 单一职责：提供当前支持状态给UI读取
     */
    fun supportsMultimodal(): Boolean {
        return supportsMultimodalState.value
    }
    
    /**
     * 刷新当前模型的多模态支持状态
     * 单一职责：查询当前模型并更新本地状态，供UI响应变化
     */
    fun refreshModelSupport() {
        viewModelScope.launch {
            val currentModel = aiRepository.getCurrentModel()
            _supportsMultimodal.value = aiRepository.supportsMultimodal(currentModel)
        }
    }
    
    /**
     * 切换智能体
     * 更新对话的智能体ID，并刷新对话信息
     * @param newAgentId 新智能体ID
     */
    fun switchAgent(newAgentId: String) {
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            try {
                // 更新对话的智能体ID
                conversationRepository.updateConversationAgent(conversationId, newAgentId)
                
                // 刷新对话信息以反映智能体变更
                refreshConversation()
                
                println("Debug: 智能体切换成功，新智能体ID: $newAgentId")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "切换智能体失败: ${e.message}"
                )
                println("Debug: 切换智能体失败: ${e.message}")
            }
        }
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