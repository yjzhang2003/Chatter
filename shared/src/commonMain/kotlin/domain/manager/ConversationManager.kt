package domain.manager

import data.repository.ConversationRepository
import domain.model.Conversation
import domain.model.ChatMessage
import domain.model.MessageSender
import domain.model.AIModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 对话管理器
 * 处理对话生命周期和上下文管理
 */
class ConversationManager(
    private val conversationRepository: ConversationRepository
) {
    
    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val mutex = Mutex()
    
    /**
     * 当前对话流
     */
    val currentConversation: Flow<Conversation?> = _currentConversation.asStateFlow()
    
    /**
     * 当前对话消息流
     */
    val currentMessages: Flow<List<ChatMessage>> = _currentMessages.asStateFlow()
    
    /**
     * 所有对话流
     */
    val allConversations: Flow<List<Conversation>> = conversationRepository.getAllConversationsFlow()

    /**
     * 保存消息到数据库
     */
    suspend fun saveMessage(message: ChatMessage): Boolean {
        return conversationRepository.addMessage(message)
    }

    /**
     * 创建新对话
     * @param title 对话标题，如果为空则自动生成
     * @param aiModel 使用的AI模型
     * @return 创建的对话ID，失败返回null
     */
    suspend fun createNewConversation(title: String? = null, aiModel: AIModel? = null): String? {
        return mutex.withLock {
            try {
                val conversation = conversationRepository.createConversation(title, aiModel)
                conversation?.let {
                    _currentConversation.value = it
                    _currentMessages.value = emptyList()
                    it.id
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 切换到指定对话
     * @param conversationId 对话ID
     * @return 是否切换成功
     */
    suspend fun switchToConversation(conversationId: String): Boolean {
        return mutex.withLock {
            try {
                val conversation = conversationRepository.getConversationById(conversationId)
                if (conversation != null) {
                    _currentConversation.value = conversation
                    val messages = conversationRepository.getConversationMessages(conversationId)
                    _currentMessages.value = messages
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 删除对话
     * @param conversationId 对话ID
     * @return 是否删除成功
     */
    suspend fun deleteConversation(conversationId: String): Boolean {
        return mutex.withLock {
            try {
                val success = conversationRepository.deleteConversation(conversationId)
                
                // 如果删除的是当前对话，清空当前状态
                if (success && _currentConversation.value?.id == conversationId) {
                    _currentConversation.value = null
                    _currentMessages.value = emptyList()
                }
                
                success
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 更新对话标题
     * @param conversationId 对话ID
     * @param newTitle 新标题
     * @return 是否更新成功
     */
    suspend fun updateConversationTitle(conversationId: String, newTitle: String): Boolean {
        return mutex.withLock {
            try {
                val conversation = conversationRepository.getConversationById(conversationId)
                if (conversation != null) {
                    val updatedConversation = conversation.copy(title = newTitle)
                    val success = conversationRepository.updateConversation(updatedConversation)
                    
                    // 如果更新的是当前对话，更新当前状态
                    if (success && _currentConversation.value?.id == conversationId) {
                        _currentConversation.value = updatedConversation
                    }
                    
                    success
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 获取对话上下文消息（用于AI生成）
     * @param limit 最大消息数量
     * @return 上下文消息列表
     */
    suspend fun getContextMessages(limit: Int = 10): List<ChatMessage> {
        val currentConv = _currentConversation.value ?: return emptyList()
        return conversationRepository.getContextMessages(currentConv.id, limit)
    }

    /**
     * 搜索对话
     * @param query 搜索关键词
     * @return 匹配的对话列表
     */
    suspend fun searchConversations(query: String): List<Conversation> {
        return conversationRepository.searchConversations(query)
    }
    
    /**
     * 搜索消息
     * @param query 搜索关键词
     * @param conversationId 可选的对话ID，限制搜索范围
     * @return 匹配的消息列表
     */
    suspend fun searchMessages(query: String, conversationId: String? = null): List<ChatMessage> {
        return conversationRepository.searchMessages(query, conversationId)
    }

    /**
     * 清空当前对话
     */
    suspend fun clearCurrentConversation() {
        mutex.withLock {
            _currentConversation.value = null
            _currentMessages.value = emptyList()
        }
    }

    /**
     * 获取对话统计信息
     */
    suspend fun getConversationStats(): ConversationStats {
        return try {
            val totalConversations = conversationRepository.getConversationCount()
            val allConversations = conversationRepository.getAllConversations()
            val totalMessages = allConversations.sumOf { it.messageCount }
            
            ConversationStats(
                totalConversations = totalConversations,
                totalMessages = totalMessages,
                currentConversationId = _currentConversation.value?.id
            )
        } catch (e: Exception) {
            ConversationStats()
        }
    }

    /**
     * 根据消息内容生成对话标题
     */
    private fun generateConversationTitle(content: String): String {
        return when {
            content.length <= 20 -> content
            content.length <= 50 -> content.take(20) + "..."
            else -> {
                // 尝试找到第一个句号或问号
                val firstSentence = content.split(Regex("[。？！.?!]")).firstOrNull()
                if (firstSentence != null && firstSentence.length <= 30) {
                    firstSentence
                } else {
                    content.take(20) + "..."
                }
            }
        }
    }
}

/**
 * 对话统计信息
 */
data class ConversationStats(
    val totalConversations: Int = 0,
    val totalMessages: Int = 0,
    val currentConversationId: String? = null
)