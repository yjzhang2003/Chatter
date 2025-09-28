package data.database

import app.cash.sqldelight.db.SqlDriver
import domain.model.ChatMessage
import domain.model.MessageSender
import domain.model.MessageMetadata
import domain.model.AIModel
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 聊天消息数据访问对象
 * 提供聊天消息相关的数据库操作功能
 */
class ChatMessageDao(private val driver: SqlDriver) {
    
    private val database = ChatDatabase(driver)
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 根据对话ID获取所有消息
     * @param conversationId 对话ID
     * @return 按创建时间升序排列的消息列表
     */
    suspend fun getMessagesByConversationId(conversationId: String): List<ChatMessage> {
        return try {
            database.chatDatabaseQueries.getMessagesByConversationId(conversationId).executeAsList().map { row ->
                ChatMessage(
                    id = row.id,
                    conversationId = row.conversation_id,
                    content = row.content ?: "",
                    sender = MessageSender.valueOf(row.sender),
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    images = parseImages(row.images),
                    isLoading = row.is_loading == 1L,
                    aiModel = row.ai_model?.let { AIModel.fromString(it) },
                    metadata = createMetadata(row)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 根据ID获取消息
     * @param messageId 消息ID
     * @return 消息对象，如果不存在则返回null
     */
    suspend fun getMessageById(messageId: String): ChatMessage? {
        return try {
            val row = database.chatDatabaseQueries.getMessageById(messageId).executeAsOneOrNull()
            row?.let {
                ChatMessage(
                    id = it.id,
                    conversationId = it.conversation_id,
                    content = it.content,
                    sender = MessageSender.valueOf(it.sender),
                    createdAt = Instant.fromEpochMilliseconds(it.created_at),
                    images = parseImages(it.images),
                    isLoading = it.is_loading == 1L,
                    aiModel = it.ai_model?.let { model -> AIModel.fromString(model) },
                    metadata = createMetadata(it)
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 插入新消息
     * @param message 要插入的消息对象
     * @return 是否插入成功
     */
    suspend fun insertMessage(message: ChatMessage): Boolean {
        return try {
            database.chatDatabaseQueries.insertMessage(
                id = message.id,
                conversation_id = message.conversationId,
                content = message.content,
                sender = message.sender.name,
                created_at = message.createdAt.toEpochMilliseconds(),
                images = serializeImages(message.images),
                is_loading = if (message.isLoading) 1L else 0L,
                ai_model = message.aiModel?.name,
                token_count = message.metadata?.tokenCount?.toLong(),
                processing_time = message.metadata?.processingTime,
                temperature = message.metadata?.temperature?.toDouble(),
                max_tokens = message.metadata?.maxTokens?.toLong(),
                error_message = message.metadata?.errorMessage
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新消息
     * @param message 要更新的消息对象
     * @return 是否更新成功
     */
    suspend fun updateMessage(message: ChatMessage): Boolean {
        return try {
            database.chatDatabaseQueries.updateMessage(
                content = message.content,
                is_loading = if (message.isLoading) 1L else 0L,
                token_count = message.metadata?.tokenCount?.toLong(),
                processing_time = message.metadata?.processingTime,
                temperature = message.metadata?.temperature?.toDouble(),
                max_tokens = message.metadata?.maxTokens?.toLong(),
                error_message = message.metadata?.errorMessage,
                id = message.id
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除消息
     * @param messageId 要删除的消息ID
     * @return 是否删除成功
     */
    suspend fun deleteMessage(messageId: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteMessage(messageId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除对话的所有消息
     * @param conversationId 对话ID
     * @return 是否删除成功
     */
    suspend fun deleteMessagesByConversationId(conversationId: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteMessagesByConversationId(conversationId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取对话的消息数量
     * @param conversationId 对话ID
     * @return 消息数量
     */
    suspend fun getMessageCountByConversationId(conversationId: String): Int {
        return try {
            database.chatDatabaseQueries.getMessageCountByConversationId(conversationId).executeAsOne().toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * 获取对话的最后一条消息
     * @param conversationId 对话ID
     * @return 最后一条消息，如果不存在则返回null
     */
    suspend fun getLastMessageByConversationId(conversationId: String): ChatMessage? {
        return try {
            val row = database.chatDatabaseQueries.getLastMessageByConversationId(conversationId).executeAsOneOrNull()
            row?.let {
                ChatMessage(
                    id = it.id,
                    conversationId = it.conversation_id,
                    content = it.content,
                    sender = MessageSender.valueOf(it.sender),
                    createdAt = Instant.fromEpochMilliseconds(it.created_at),
                    images = parseImages(it.images),
                    isLoading = it.is_loading == 1L,
                    aiModel = it.ai_model?.let { model -> AIModel.fromString(model) },
                    metadata = createMetadata(it)
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取上下文消息（最近的N条消息）
     * @param conversationId 对话ID
     * @param limit 消息数量限制
     * @return 按创建时间倒序排列的消息列表
     */
    suspend fun getContextMessages(conversationId: String, limit: Int): List<ChatMessage> {
        return try {
            database.chatDatabaseQueries.getContextMessages(conversationId, limit.toLong()).executeAsList().map { row ->
                ChatMessage(
                    id = row.id,
                    conversationId = row.conversation_id,
                    content = row.content ?: "",
                    sender = MessageSender.valueOf(row.sender),
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    images = parseImages(row.images),
                    isLoading = row.is_loading == 1L,
                    aiModel = row.ai_model?.let { AIModel.fromString(it) },
                    metadata = createMetadata(row)
                )
            }.reversed() // 转换为升序以保持对话顺序
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 搜索消息
     * @param query 搜索关键词
     * @param conversationId 可选的对话ID，限制搜索范围
     * @return 匹配的消息列表
     */
    suspend fun searchMessages(query: String, conversationId: String?): List<ChatMessage> {
        return try {
            database.chatDatabaseQueries.searchMessages(query, conversationId ?: "").executeAsList().map { row ->
                ChatMessage(
                    id = row.id,
                    conversationId = row.conversation_id,
                    content = row.content ?: "",
                    sender = MessageSender.valueOf(row.sender),
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    images = parseImages(row.images),
                    isLoading = row.is_loading == 1L,
                    aiModel = row.ai_model?.let { AIModel.fromString(it) },
                    metadata = createMetadata(row)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 清空所有消息
     * @return 是否清空成功
     */
    suspend fun clearAllMessages(): Boolean {
        return try {
            database.chatDatabaseQueries.clearAllMessages()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 解析图片列表JSON字符串
     */
    private fun parseImages(imagesJson: String?): List<String> {
        return try {
            if (imagesJson.isNullOrBlank()) {
                emptyList()
            } else {
                json.decodeFromString<List<String>>(imagesJson)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 序列化图片列表为JSON字符串
     */
    private fun serializeImages(images: List<String>): String {
        return try {
            if (images.isEmpty()) {
                ""
            } else {
                json.encodeToString(images)
            }
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 从数据库行创建消息元数据
     */
    /**
     * 创建消息元数据对象
     * @param row 数据库行数据
     * @return 消息元数据对象，如果没有元数据则返回null
     */
    private fun createMetadata(row: data.database.Chat_message): MessageMetadata? {
        val hasMetadata = row.token_count != null || 
                         row.processing_time != null || 
                         row.temperature != null || 
                         row.max_tokens != null || 
                         row.error_message != null
        
        return if (hasMetadata) {
            MessageMetadata(
                tokenCount = row.token_count?.toInt(),
                processingTime = row.processing_time,
                temperature = row.temperature?.toFloat(),
                maxTokens = row.max_tokens?.toInt(),
                errorMessage = row.error_message
            )
        } else {
            null
        }
    }
}