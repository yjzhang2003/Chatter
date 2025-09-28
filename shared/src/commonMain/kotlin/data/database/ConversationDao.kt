package data.database

import app.cash.sqldelight.db.SqlDriver
import domain.model.Conversation as DomainConversation
import domain.model.AIModel
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

/**
 * 对话数据访问对象
 * 提供对话相关的数据库操作功能
 */
class ConversationDao(private val driver: SqlDriver) {
    
    private val database = ChatDatabase(driver)
    
    /**
     * 获取所有对话列表
     * @return 按更新时间倒序排列的对话列表
     */
    suspend fun getAllConversations(): List<DomainConversation> {
        return try {
            database.chatDatabaseQueries.getAllConversations().executeAsList().map { row ->
                DomainConversation(
                    id = row.id,
                    title = row.title,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    messageCount = row.message_count.toInt(),
                    lastMessage = row.last_message ?: "",
                    aiModel = AIModel.fromString(row.ai_model),
                    agentId = row.agent_id
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 根据ID获取对话
     * @param conversationId 对话ID
     * @return 对话对象，如果不存在则返回null
     */
    suspend fun getConversationById(conversationId: String): DomainConversation? {
        return try {
            val row = database.chatDatabaseQueries.getConversationById(conversationId).executeAsOneOrNull()
            row?.let {
                DomainConversation(
                    id = it.id,
                    title = it.title,
                    createdAt = Instant.fromEpochMilliseconds(it.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(it.updated_at),
                    messageCount = it.message_count.toInt(),
                    lastMessage = it.last_message ?: "",
                    aiModel = AIModel.fromString(it.ai_model),
                    agentId = it.agent_id
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 插入新对话
     * @param conversation 要插入的对话对象
     * @return 是否插入成功
     */
    suspend fun insertConversation(conversation: DomainConversation): Boolean {
        return try {
            database.chatDatabaseQueries.insertConversation(
                id = conversation.id,
                title = conversation.title,
                created_at = conversation.createdAt.toEpochMilliseconds(),
                updated_at = conversation.updatedAt.toEpochMilliseconds(),
                message_count = conversation.messageCount.toLong(),
                last_message = conversation.lastMessage,
                ai_model = conversation.aiModel.name,
                agent_id = conversation.agentId
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新对话信息
     * @param conversation 要更新的对话对象
     * @return 是否更新成功
     */
    suspend fun updateConversation(conversation: DomainConversation): Boolean {
        return try {
            database.chatDatabaseQueries.updateConversation(
                title = conversation.title,
                updated_at = conversation.updatedAt.toEpochMilliseconds(),
                message_count = conversation.messageCount.toLong(),
                last_message = conversation.lastMessage,
                ai_model = conversation.aiModel.name,
                agent_id = conversation.agentId,
                id = conversation.id
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除对话
     * @param conversationId 要删除的对话ID
     * @return 是否删除成功
     */
    suspend fun deleteConversation(conversationId: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteConversation(conversationId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取对话总数
     * @return 对话总数
     */
    suspend fun getConversationCount(): Int {
        return try {
            database.chatDatabaseQueries.getConversationCount().executeAsOne().toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 更新对话统计信息
     * @param conversationId 对话ID
     * @param messageCount 消息数量
     * @param lastMessage 最后一条消息内容
     * @return 是否更新成功
     */
    suspend fun updateConversationStats(
        conversationId: String,
        messageCount: Int,
        lastMessage: String
    ): Boolean {
        return try {
            database.chatDatabaseQueries.updateConversationStats(
                message_count = messageCount.toLong(),
                last_message = lastMessage,
                updated_at = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                id = conversationId
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 搜索对话
     * @param query 搜索关键词
     * @return 匹配的对话列表
     */
    suspend fun searchConversations(query: String): List<DomainConversation> {
        return try {
            database.chatDatabaseQueries.searchConversations(query, query).executeAsList().map { row ->
                DomainConversation(
                    id = row.id,
                    title = row.title,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    messageCount = row.message_count.toInt(),
                    lastMessage = row.last_message ?: "",
                    aiModel = AIModel.fromString(row.ai_model),
                    agentId = row.agent_id
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 清空所有对话
     * @return 是否清空成功
     */
    suspend fun clearAllConversations(): Boolean {
        return try {
            database.chatDatabaseQueries.clearAllConversations()
            true
        } catch (e: Exception) {
            false
        }
    }
}