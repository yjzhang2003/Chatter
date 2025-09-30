package data.repository

import domain.model.Conversation
import domain.model.ChatMessage
import domain.model.AIModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * 对话仓库实现类
 * 提供对话管理的具体实现
 */
class ConversationRepositoryImpl : ConversationRepository {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val _messages = mutableMapOf<String, MutableList<ChatMessage>>()
    
    // 添加互斥锁来保证线程安全
    private val mutex = Mutex()

    override fun getAllConversationsFlow(): Flow<List<Conversation>> {
        return _conversations.asStateFlow()
    }

    override suspend fun getAllConversations(): List<Conversation> {
        return _conversations.value
    }

    override suspend fun getConversationById(id: String): Conversation? {
        return _conversations.value.find { it.id == id }
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
                
                val currentConversations = _conversations.value.toMutableList()
                currentConversations.add(0, conversation)
                _conversations.value = currentConversations
                
                // 初始化消息列表
                _messages[conversation.id] = mutableListOf()
                
                conversation
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun updateConversation(conversation: Conversation): Boolean {
        return try {
            val currentConversations = _conversations.value.toMutableList()
            val index = currentConversations.indexOfFirst { it.id == conversation.id }
            if (index != -1) {
                currentConversations[index] = conversation.copy(updatedAt = Clock.System.now())
                _conversations.value = currentConversations
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteConversation(conversationId: String): Boolean {
        return mutex.withLock {
            try {
                val currentConversations = _conversations.value.toMutableList()
                val removed = currentConversations.removeAll { it.id == conversationId }
                if (removed) {
                    _conversations.value = currentConversations
                    _messages.remove(conversationId)
                }
                removed
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getConversationMessages(conversationId: String): List<ChatMessage> {
        return _messages[conversationId]?.toList() ?: emptyList()
    }

    override suspend fun getContextMessages(conversationId: String, limit: Int): List<ChatMessage> {
        val messages = _messages[conversationId] ?: return emptyList()
        return messages.takeLast(limit)
    }

    override suspend fun addMessage(message: ChatMessage): Boolean {
        return mutex.withLock {
            try {
                val conversationMessages = _messages.getOrPut(message.conversationId) { mutableListOf() }
                conversationMessages.add(message)
                
                // 更新对话统计信息
                updateConversationStats(message.conversationId)
                
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun updateMessage(message: ChatMessage): Boolean {
        return try {
            val conversationMessages = _messages[message.conversationId] ?: return false
            val index = conversationMessages.indexOfFirst { it.id == message.id }
            if (index != -1) {
                conversationMessages[index] = message
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteMessage(messageId: String): Boolean {
        return try {
            var found = false
            _messages.values.forEach { messageList ->
                val removed = messageList.removeAll { it.id == messageId }
                if (removed) found = true
            }
            found
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun searchConversations(query: String): List<Conversation> {
        return _conversations.value.filter { conversation ->
            conversation.title.contains(query, ignoreCase = true) ||
            conversation.lastMessage.contains(query, ignoreCase = true)
        }
    }

    override suspend fun searchMessages(query: String, conversationId: String?): List<ChatMessage> {
        val messagesToSearch = if (conversationId != null) {
            _messages[conversationId]?.toList() ?: emptyList()
        } else {
            _messages.values.flatten()
        }
        
        return messagesToSearch.filter { message ->
            message.content.contains(query, ignoreCase = true)
        }
    }

    override suspend fun getConversationCount(): Int {
        return _conversations.value.size
    }

    override suspend fun clearAllData(): Boolean {
        return try {
            _conversations.value = emptyList()
            _messages.clear()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateConversationStats(conversationId: String): Boolean {
        return try {
            val conversation = getConversationById(conversationId) ?: return false
            val messages = _messages[conversationId] ?: emptyList()
            
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
        return try {
            val conversation = getConversationById(conversationId) ?: return false
            val updatedConversation = conversation.copy(agentId = agentId)
            updateConversation(updatedConversation)
        } catch (e: Exception) {
            false
        }
    }
}