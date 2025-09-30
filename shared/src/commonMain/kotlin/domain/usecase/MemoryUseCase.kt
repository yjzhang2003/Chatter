package domain.usecase

import domain.manager.MemoryManager
import domain.model.AgentMemory
import domain.model.ChatMessage
import domain.repository.AgentRepository
import data.repository.ConversationRepository

/**
 * 记忆用例
 * 提供记忆系统的高级业务逻辑接口
 */
class MemoryUseCase(
    private val memoryManager: MemoryManager,
    private val agentRepository: AgentRepository,
    private val conversationRepository: ConversationRepository
) {
    
    /**
     * 处理新消息并创建记忆
     * @param agentId 智能体ID
     * @param message 聊天消息
     * @param conversationId 对话ID
     * @return 是否成功创建记忆
     */
    suspend fun processMessageForMemory(
        agentId: String,
        message: ChatMessage,
        conversationId: String
    ): Boolean {
        return try {
            val memory = memoryManager.createMemoryFromMessage(agentId, message, conversationId)
            memory != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取对话上下文记忆
     * @param agentId 智能体ID
     * @param conversationId 对话ID
     * @param query 当前查询内容
     * @return 相关记忆列表
     */
    suspend fun getContextualMemories(
        agentId: String,
        conversationId: String,
        query: String
    ): List<AgentMemory> {
        // 1. 获取当前对话的相关记忆
        val conversationMemories = agentRepository.getMemoriesByConversation(conversationId)
        
        // 2. 获取与查询相关的全局记忆
        val relevantMemories = memoryManager.retrieveRelevantMemories(agentId, query, 5)
        
        // 3. 合并并去重
        val allMemories = (conversationMemories + relevantMemories).distinctBy { it.id }
        
        // 4. 按重要性和相关性排序
        return allMemories.sortedWith(
            compareByDescending<AgentMemory> { it.importanceScore }
                .thenByDescending { it.accessCount }
                .thenByDescending { it.lastAccessedAt }
        ).take(10)
    }
    
    /**
     * 生成记忆增强的回复上下文
     * @param agentId 智能体ID
     * @param conversationId 对话ID
     * @param currentMessage 当前消息
     * @return 增强的上下文文本
     */
    suspend fun generateMemoryEnhancedContext(
        agentId: String,
        conversationId: String,
        currentMessage: String
    ): String {
        val memories = getContextualMemories(agentId, conversationId, currentMessage)
        
        if (memories.isEmpty()) {
            return ""
        }
        
        return buildString {
            appendLine("基于以往记忆的上下文信息：")
            memories.forEach { memory ->
                appendLine("- ${memory.content}")
                if (memory.tags.isNotEmpty()) {
                    appendLine("  标签: ${memory.tags.joinToString(", ")}")
                }
            }
            appendLine()
        }
    }
    
    /**
     * 获取智能体记忆统计信息
     * @param agentId 智能体ID
     * @return 记忆统计信息
     */
    suspend fun getMemoryStatistics(agentId: String): MemoryStatistics {
        val memories = agentRepository.getAgentMemories(agentId)
        
        return MemoryStatistics(
            totalMemories = memories.size,
            importantMemories = memories.count { it.importanceScore > 0.7 },
            recentMemories = memories.count { 
                (kotlinx.datetime.Clock.System.now() - it.createdAt).inWholeDays < 7 
            },
            averageImportance = if (memories.isNotEmpty()) {
                memories.map { it.importanceScore }.average()
            } else 0.0,
            mostAccessedMemory = memories.maxByOrNull { it.accessCount },
            memoryTypeDistribution = memories.groupBy { it.memoryType }
                .mapValues { it.value.size }
        )
    }
    
    /**
     * 搜索记忆
     * @param agentId 智能体ID
     * @param query 搜索查询
     * @param limit 结果限制
     * @return 搜索结果
     */
    suspend fun searchMemories(
        agentId: String,
        query: String,
        limit: Int = 20
    ): List<AgentMemory> {
        return memoryManager.retrieveRelevantMemories(agentId, query, limit)
    }
    
    /**
     * 更新记忆反馈
     * @param memoryId 记忆ID
     * @param isHelpful 是否有帮助
     */
    suspend fun updateMemoryFeedback(memoryId: String, isHelpful: Boolean) {
        val feedback = if (isHelpful) 0.1 else -0.1
        memoryManager.updateMemoryImportance(memoryId, feedback)
    }
    
    /**
     * 获取对话记忆摘要
     * @param agentId 智能体ID
     * @param conversationId 对话ID
     * @return 记忆摘要
     */
    suspend fun getConversationMemorySummary(
        agentId: String,
        conversationId: String
    ): String {
        return memoryManager.getConversationMemorySummary(agentId, conversationId)
    }
}

/**
 * 记忆统计信息数据类
 */
data class MemoryStatistics(
    val totalMemories: Int,
    val importantMemories: Int,
    val recentMemories: Int,
    val averageImportance: Double,
    val mostAccessedMemory: AgentMemory?,
    val memoryTypeDistribution: Map<domain.model.MemoryType, Int>
)