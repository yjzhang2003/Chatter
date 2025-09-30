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
import kotlinx.coroutines.launch

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
    fun sendMessage(text: String) {
        val conversationId = currentConversationId ?: return
        
        // 防止重复发送
        if (_uiState.value.status is Status.Loading) {
            return
        }
        
        viewModelScope.launch {
            try {
                // 设置加载状态
                _uiState.value = _uiState.value.copy(status = Status.Loading)
                
                // 确保ConversationManager切换到当前对话
                conversationManager.switchToConversation(conversationId)
                
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
                
                // 处理用户消息的记忆创建
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
                
                // 添加AI消息占位符到UI
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + aiMessage
                )
                
                // 获取当前对话的智能体信息
                val currentConversation = _uiState.value.conversation
                println("Debug: 当前对话信息: ${currentConversation?.title}, agentId: ${currentConversation?.agentId}")
                
                val agent = currentConversation?.agentId?.let { agentId ->
                    println("Debug: 正在获取智能体信息，agentId: $agentId")
                    val agentInfo = agentRepository.getAgentById(agentId)
                    println("Debug: 获取到的智能体信息: ${agentInfo?.name}, systemPrompt长度: ${agentInfo?.systemPrompt?.length}")
                    agentInfo
                }
                
                // 构建包含智能体系统提示的上下文消息
                val contextMessages = mutableListOf<ChatMessage>()
                
                // 如果有智能体，添加系统提示作为第一条消息
                if (agent != null) {
                    println("Debug: 智能体不为空: ${agent.name}")
                    if (agent.systemPrompt.isNotBlank()) {
                        val systemMessage = ChatMessage.create(
                            conversationId = conversationId,
                            content = agent.systemPrompt,
                            sender = MessageSender.SYSTEM
                        )
                        contextMessages.add(systemMessage)
                        println("Debug: 添加智能体系统提示: ${agent.name} - ${agent.systemPrompt.take(50)}...")
                    } else {
                        println("Debug: 智能体系统提示为空")
                    }
                    
                    // 添加记忆增强的上下文
                    val memoryContext = memoryUseCase.generateMemoryEnhancedContext(
                        agentId = agent.id,
                        conversationId = conversationId,
                        currentMessage = text
                    )
                    
                    if (memoryContext.isNotBlank()) {
                        val memoryMessage = ChatMessage.create(
                            conversationId = conversationId,
                            content = memoryContext,
                            sender = MessageSender.SYSTEM
                        )
                        contextMessages.add(memoryMessage)
                        println("Debug: 添加记忆上下文: ${memoryContext.take(100)}...")
                    }
                } else {
                    println("Debug: 没有找到智能体信息")
                }
                
                // 获取历史对话上下文消息
                val historyMessages = conversationManager.getContextMessages(
                    currentPrompt = text,
                    useTokenOptimization = true
                )
                contextMessages.addAll(historyMessages)
                
                // 添加调试日志
                println("Debug: 总共获取到 ${contextMessages.size} 条上下文消息（包含智能体系统提示）")
                contextMessages.forEachIndexed { index, message ->
                    println("Debug: 上下文消息 $index: ${message.sender} - ${message.content.take(50)}...")
                }
                
                // 生成AI回复，传递包含智能体系统提示的上下文消息
                val aiResponse = when (val result = aiRepository.generate(text, emptyList(), contextMessages)) {
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
                    },
                    status = Status.Success("消息发送成功")
                )
                
                // 保存AI消息到数据库
                conversationManager.saveMessage(updatedAiMessage)
                
                // 处理AI消息的记忆创建
                if (agentId != null) {
                    memoryUseCase.processMessageForMemory(agentId, updatedAiMessage, conversationId)
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "发送消息失败: ${e.message}",
                    status = Status.Error("发送消息失败: ${e.message}")
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