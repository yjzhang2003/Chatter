package data.repository

import domain.model.Conversation
import domain.model.ChatMessage
import domain.model.AIModel
import kotlinx.coroutines.flow.Flow

/**
 * 对话仓库接口
 * 定义对话管理的核心业务逻辑
 */
interface ConversationRepository {

    /**
     * 获取所有对话的流
     */
    fun getAllConversationsFlow(): Flow<List<Conversation>>

    /**
     * 获取所有对话
     */
    suspend fun getAllConversations(): List<Conversation>

    /**
     * 根据ID获取对话
     */
    suspend fun getConversationById(id: String): Conversation?

    /**
     * 创建新对话
     * @param title 对话标题，如果为空则自动生成
     * @param aiModel 使用的AI模型
     * @return 创建的对话对象
     */
    suspend fun createConversation(title: String? = null, aiModel: AIModel? = null): Conversation?

    /**
     * 更新对话信息
     */
    suspend fun updateConversation(conversation: Conversation): Boolean

    /**
     * 删除对话及其所有消息
     */
    suspend fun deleteConversation(conversationId: String): Boolean

    /**
     * 获取对话的所有消息
     */
    suspend fun getConversationMessages(conversationId: String): List<ChatMessage>

    /**
     * 获取对话的上下文消息（用于AI生成）
     * @param conversationId 对话ID
     * @param limit 最大消息数量
     */
    suspend fun getContextMessages(conversationId: String, limit: Int = 10): List<ChatMessage>

    /**
     * 添加消息到对话
     */
    suspend fun addMessage(message: ChatMessage): Boolean

    /**
     * 更新消息
     */
    suspend fun updateMessage(message: ChatMessage): Boolean

    /**
     * 删除消息
     */
    suspend fun deleteMessage(messageId: String): Boolean

    /**
     * 搜索对话
     * @param query 搜索关键词
     */
    suspend fun searchConversations(query: String): List<Conversation>

    /**
     * 搜索消息
     * @param query 搜索关键词
     * @param conversationId 可选的对话ID，限制搜索范围
     */
    suspend fun searchMessages(query: String, conversationId: String? = null): List<ChatMessage>

    /**
     * 获取对话数量
     */
    suspend fun getConversationCount(): Int

    /**
     * 清空所有对话和消息
     */
    suspend fun clearAllData(): Boolean

    /**
     * 更新对话统计信息（消息数量、最后消息等）
     */
    suspend fun updateConversationStats(conversationId: String): Boolean
}