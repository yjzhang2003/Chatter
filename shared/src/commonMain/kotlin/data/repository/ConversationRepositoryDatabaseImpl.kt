package data.repository

import data.database.ConversationDao
import data.database.ChatMessageDao
import domain.model.Conversation
import domain.model.ChatMessage
import domain.model.AIModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * 基于数据库的对话仓库实现类
 * 提供对话管理的数据库持久化实现
 */
class ConversationRepositoryDatabaseImpl(
    private val conversationDao: ConversationDao,
    private val chatMessageDao: ChatMessageDao
) : ConversationRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val mutex = Mutex()

    init {
        // 初始化时从数据库加载对话
        loadConversationsFromDatabase()
    }

    /**
     * 从数据库加载对话列表
     */
    private fun loadConversationsFromDatabase() {
        kotlinx.coroutines.GlobalScope.launch {
            try {
                val conversations = conversationDao.getAllConversations()
                _conversations.value = conversations
            } catch (e: Exception) {
                // 加载失败时保持空列表
                _conversations.value = emptyList()
            }
        }
    }

    override fun getAllConversationsFlow(): Flow<List<Conversation>> {
        return _conversations.asStateFlow()
    }

    override suspend fun getAllConversations(): List<Conversation> {
        return mutex.withLock {
            try {
                val conversations = conversationDao.getAllConversations()
                _conversations.value = conversations
                conversations
            } catch (e: Exception) {
                _conversations.value
            }
        }
    }

    override suspend fun getConversationById(id: String): Conversation? {
        return try {
            conversationDao.getConversationById(id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createConversation(title: String?, aiModel: AIModel?): Conversation? {
        return mutex.withLock {
            try {
                val conversation = Conversation(
                    id = generateId(),
                    title = title ?: "新对话",
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                    messageCount = 0,
                    lastMessage = "",
                    aiModel = aiModel ?: AIModel.getDefault()
                )
                
                val success = conversationDao.insertConversation(conversation)
                if (success) {
                    // 更新内存中的对话列表
                    val currentConversations = _conversations.value.toMutableList()
                    currentConversations.add(0, conversation)
                    _conversations.value = currentConversations
                    conversation
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun updateConversation(conversation: Conversation): Boolean {
        return mutex.withLock {
            try {
                val updatedConversation = conversation.copy(updatedAt = Clock.System.now())
                val success = conversationDao.updateConversation(updatedConversation)
                if (success) {
                    // 更新内存中的对话列表
                    val currentConversations = _conversations.value.toMutableList()
                    val index = currentConversations.indexOfFirst { it.id == conversation.id }
                    if (index != -1) {
                        currentConversations[index] = updatedConversation
                        _conversations.value = currentConversations
                    }
                }
                success
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun deleteConversation(conversationId: String): Boolean {
        return mutex.withLock {
            try {
                val success = conversationDao.deleteConversation(conversationId)
                if (success) {
                    // 删除相关消息
                    chatMessageDao.deleteMessagesByConversationId(conversationId)
                    
                    // 更新内存中的对话列表
                    val currentConversations = _conversations.value.toMutableList()
                    currentConversations.removeAll { it.id == conversationId }
                    _conversations.value = currentConversations
                }
                success
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getConversationMessages(conversationId: String): List<ChatMessage> {
        return try {
            chatMessageDao.getMessagesByConversationId(conversationId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getContextMessages(conversationId: String, limit: Int): List<ChatMessage> {
        return try {
            val messages = chatMessageDao.getMessagesByConversationId(conversationId)
            messages.takeLast(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun addMessage(message: ChatMessage): Boolean {
        return try {
            val success = chatMessageDao.insertMessage(message)
            if (success) {
                // 更新对话统计信息
                updateConversationStats(message.conversationId)
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateMessage(message: ChatMessage): Boolean {
        return try {
            chatMessageDao.updateMessage(message)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteMessage(messageId: String): Boolean {
        return try {
            chatMessageDao.deleteMessage(messageId)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun searchConversations(query: String): List<Conversation> {
        return try {
            conversationDao.searchConversations(query)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun searchMessages(query: String, conversationId: String?): List<ChatMessage> {
        return try {
            chatMessageDao.searchMessages(query, conversationId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getConversationCount(): Int {
        return try {
            conversationDao.getConversationCount()
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun clearAllData(): Boolean {
        return mutex.withLock {
            try {
                val success = conversationDao.clearAllConversations()
                if (success) {
                    _conversations.value = emptyList()
                }
                success
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun updateConversationStats(conversationId: String): Boolean {
        return try {
            val conversation = getConversationById(conversationId) ?: return false
            val messages = getConversationMessages(conversationId)
            
            val updatedConversation = conversation.copy(
                messageCount = messages.size,
                lastMessage = messages.lastOrNull()?.content ?: "",
                updatedAt = Clock.System.now()
            )
            
            updateConversation(updatedConversation)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 生成唯一的对话ID
     */
    private fun generateId(): String {
        return "conv_${Clock.System.now().toEpochMilliseconds()}_${(0..999999).random()}"
    }
    
    override suspend fun updateConversationAgent(conversationId: String, agentId: String): Boolean {
        return mutex.withLock {
            try {
                val conversation = getConversationById(conversationId) ?: return@withLock false
                val updatedConversation = conversation.copy(agentId = agentId)
                updateConversation(updatedConversation)
            } catch (e: Exception) {
                false
            }
        }
    }
}