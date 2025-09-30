package domain.manager

import domain.model.AgentMemory
import domain.model.MemoryRelation
import domain.model.MemoryType
import domain.model.RelationType
import domain.model.ChatMessage
import domain.repository.AgentRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * 记忆管理器
 * 负责智能体的长期记忆管理，包括记忆的创建、检索、重要性评估和遗忘机制
 */
class MemoryManager(
    private val agentRepository: AgentRepository
) {
    
    companion object {
        private const val DEFAULT_IMPORTANCE_THRESHOLD = 0.3
        private const val MAX_MEMORIES_PER_AGENT = 1000
        private const val MEMORY_DECAY_FACTOR = 0.95
        private const val SIMILARITY_THRESHOLD = 0.7
    }
    
    /**
     * 从聊天消息创建记忆
     * @param agentId 智能体ID
     * @param message 聊天消息
     * @param conversationId 对话ID
     * @return 创建的记忆对象，如果不重要则返回null
     */
    suspend fun createMemoryFromMessage(
        agentId: String,
        message: ChatMessage,
        conversationId: String
    ): AgentMemory? {
        val importance = calculateImportance(message.content)
        
        // 只保存重要的记忆
        if (importance < DEFAULT_IMPORTANCE_THRESHOLD) {
            return null
        }
        
        val now = Clock.System.now()
        val memory = AgentMemory(
            id = generateMemoryId(),
            agentId = agentId,
            conversationId = conversationId,
            messageId = message.id,
            content = extractKeyContent(message.content),
            memoryType = classifyMemoryType(message.content),
            importanceScore = importance,
            accessCount = 0,
            lastAccessedAt = now,
            createdAt = now,
            updatedAt = now,
            tags = extractTags(message.content),
            embeddingVector = null // TODO: 实现向量化
        )
        
        val success = agentRepository.createMemory(memory)
        if (success) {
            // 创建与现有记忆的关联关系
            createMemoryRelations(memory)
            
            // 检查是否需要清理旧记忆
            cleanupOldMemories(agentId)
            
            return memory
        }
        
        return null
    }
    
    /**
     * 检索相关记忆
     * @param agentId 智能体ID
     * @param query 查询内容
     * @param limit 返回数量限制
     * @return 相关记忆列表
     */
    suspend fun retrieveRelevantMemories(
        agentId: String,
        query: String,
        limit: Int = 10
    ): List<AgentMemory> {
        // 1. 基于关键词搜索
        val searchResults = agentRepository.searchMemories(agentId, query)
        
        // 2. 获取最重要的记忆
        val topMemories = agentRepository.getTopMemories(agentId, limit)
        
        // 3. 合并并去重
        val allMemories = (searchResults + topMemories).distinctBy { it.id }
        
        // 4. 按相关性和重要性排序
        val sortedMemories = allMemories.sortedWith(
            compareByDescending<AgentMemory> { calculateRelevance(it, query) }
                .thenByDescending { it.importanceScore }
                .thenByDescending { it.accessCount }
        )
        
        // 5. 更新访问统计
        val now = Clock.System.now()
        sortedMemories.take(limit).forEach { memory ->
            agentRepository.updateMemoryAccess(memory.id, now)
        }
        
        return sortedMemories.take(limit)
    }
    
    /**
     * 更新记忆重要性
     * @param memoryId 记忆ID
     * @param feedback 反馈分数 (-1.0 到 1.0)
     */
    suspend fun updateMemoryImportance(memoryId: String, feedback: Double) {
        val memory = agentRepository.getAgentMemories("").find { it.id == memoryId }
        memory?.let {
            val newImportance = (it.importanceScore + feedback * 0.1).coerceIn(0.0, 1.0)
            val updatedMemory = it.copy(
                importanceScore = newImportance,
                updatedAt = Clock.System.now()
            )
            agentRepository.updateMemory(updatedMemory)
        }
    }
    
    /**
     * 获取对话相关的记忆摘要
     * @param agentId 智能体ID
     * @param conversationId 对话ID
     * @return 记忆摘要文本
     */
    suspend fun getConversationMemorySummary(
        agentId: String,
        conversationId: String
    ): String {
        val memories = agentRepository.getMemoriesByConversation(conversationId)
        if (memories.isEmpty()) return ""
        
        val importantMemories = memories
            .filter { it.importanceScore > DEFAULT_IMPORTANCE_THRESHOLD }
            .sortedByDescending { it.importanceScore }
            .take(5)
        
        return buildString {
            appendLine("相关记忆摘要：")
            importantMemories.forEach { memory ->
                val formattedScore = (memory.importanceScore * 100).toInt() / 100.0
                appendLine("- ${memory.content} (重要性: $formattedScore)")
            }
        }
    }
    
    /**
     * 计算消息重要性
     * @param content 消息内容
     * @return 重要性分数 (0.0-1.0)
     */
    private fun calculateImportance(content: String): Double {
        var importance = 0.5 // 基础重要性
        
        // 长度因子
        val lengthFactor = (content.length / 100.0).coerceAtMost(0.3)
        importance += lengthFactor
        
        // 关键词因子
        val importantKeywords = listOf(
            "重要", "记住", "不要忘记", "关键", "必须", "一定要",
            "姓名", "生日", "喜欢", "不喜欢", "偏好", "习惯"
        )
        val keywordCount = importantKeywords.count { content.contains(it) }
        importance += keywordCount * 0.1
        
        // 问号和感叹号
        val punctuationCount = content.count { it in "?？!！" }
        importance += punctuationCount * 0.05
        
        return importance.coerceIn(0.0, 1.0)
    }
    
    /**
     * 分类记忆类型
     * @param content 内容
     * @return 记忆类型
     */
    private fun classifyMemoryType(content: String): MemoryType {
        return when {
            content.contains(Regex("喜欢|不喜欢|偏好|习惯")) -> MemoryType.PREFERENCE
            content.contains(Regex("会|能够|技能|能力")) -> MemoryType.SKILL
            content.contains(Regex("是|叫|名字|生日|年龄")) -> MemoryType.FACT
            else -> MemoryType.CONVERSATION
        }
    }
    
    /**
     * 提取关键内容
     * @param content 原始内容
     * @return 提取的关键内容
     */
    private fun extractKeyContent(content: String): String {
        // 简单的关键内容提取，实际应用中可以使用更复杂的NLP技术
        return if (content.length > 200) {
            content.take(200) + "..."
        } else {
            content
        }
    }
    
    /**
     * 提取标签
     * @param content 内容
     * @return 标签列表
     */
    private fun extractTags(content: String): List<String> {
        val tags = mutableListOf<String>()
        
        // 基于关键词提取标签
        val tagKeywords = mapOf(
            "个人信息" to listOf("姓名", "年龄", "生日", "职业"),
            "偏好" to listOf("喜欢", "不喜欢", "偏好", "习惯"),
            "技能" to listOf("会", "能够", "技能", "能力"),
            "情感" to listOf("开心", "难过", "生气", "兴奋"),
            "计划" to listOf("计划", "打算", "准备", "想要")
        )
        
        tagKeywords.forEach { (tag, keywords) ->
            if (keywords.any { content.contains(it) }) {
                tags.add(tag)
            }
        }
        
        return tags
    }
    
    /**
     * 创建记忆关联关系
     * @param newMemory 新记忆
     */
    private suspend fun createMemoryRelations(newMemory: AgentMemory) {
        val existingMemories = agentRepository.getAgentMemories(newMemory.agentId)
        
        existingMemories.forEach { existingMemory ->
            val similarity = calculateSimilarity(newMemory.content, existingMemory.content)
            
            if (similarity > SIMILARITY_THRESHOLD) {
                val relation = MemoryRelation(
                    id = generateRelationId(),
                    sourceMemoryId = newMemory.id,
                    targetMemoryId = existingMemory.id,
                    relationType = RelationType.SIMILAR,
                    strength = similarity,
                    createdAt = Clock.System.now()
                )
                agentRepository.createMemoryRelation(relation)
            }
        }
    }
    
    /**
     * 计算相似度
     * @param content1 内容1
     * @param content2 内容2
     * @return 相似度分数 (0.0-1.0)
     */
    private fun calculateSimilarity(content1: String, content2: String): Double {
        // 简单的基于共同词汇的相似度计算
        val words1 = content1.split(Regex("\\s+")).map { it.lowercase() }.toSet()
        val words2 = content2.split(Regex("\\s+")).map { it.lowercase() }.toSet()
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return if (union > 0) intersection.toDouble() / union else 0.0
    }
    
    /**
     * 计算相关性
     * @param memory 记忆
     * @param query 查询
     * @return 相关性分数
     */
    private fun calculateRelevance(memory: AgentMemory, query: String): Double {
        val contentSimilarity = calculateSimilarity(memory.content, query)
        val tagRelevance = memory.tags.count { tag ->
            query.lowercase().contains(tag.lowercase())
        } * 0.1
        
        return contentSimilarity + tagRelevance
    }
    
    /**
     * 清理旧记忆
     * @param agentId 智能体ID
     */
    private suspend fun cleanupOldMemories(agentId: String) {
        val memories = agentRepository.getAgentMemories(agentId)
        
        if (memories.size > MAX_MEMORIES_PER_AGENT) {
            // 按重要性和访问频率排序，删除最不重要的记忆
            val memoriesToDelete = memories
                .sortedWith(
                    compareBy<AgentMemory> { it.importanceScore }
                        .thenBy { it.accessCount }
                        .thenBy { it.lastAccessedAt }
                )
                .take(memories.size - MAX_MEMORIES_PER_AGENT)
            
            memoriesToDelete.forEach { memory ->
                agentRepository.deleteMemory(memory.id)
            }
        }
    }
    
    /**
     * 生成记忆ID
     */
    private fun generateMemoryId(): String {
        return "memory_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"
    }
    
    /**
     * 生成关联ID
     */
    private fun generateRelationId(): String {
        return "relation_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"
    }
}