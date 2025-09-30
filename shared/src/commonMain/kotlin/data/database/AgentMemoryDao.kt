package data.database

import app.cash.sqldelight.db.SqlDriver
import domain.model.AgentMemory
import domain.model.MemoryType
import domain.model.MemoryRelation
import domain.model.RelationType
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 智能体记忆数据访问对象
 * 提供智能体记忆相关的数据库操作功能
 */
class AgentMemoryDao(private val driver: SqlDriver) {
    
    private val database = ChatDatabase(driver)
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 根据智能体ID获取记忆列表
     * @param agentId 智能体ID
     * @return 按重要性和访问时间排序的记忆列表
     */
    suspend fun getMemoriesByAgentId(agentId: String): List<AgentMemory> {
        return try {
            database.chatDatabaseQueries.getMemoriesByAgentId(agentId).executeAsList().map { row ->
                AgentMemory(
                    id = row.id,
                    agentId = row.agent_id,
                    conversationId = row.conversation_id,
                    messageId = row.message_id,
                    content = row.content,
                    memoryType = MemoryType.fromString(row.memory_type),
                    importanceScore = row.importance_score,
                    accessCount = row.access_count.toInt(),
                    lastAccessedAt = Instant.fromEpochMilliseconds(row.last_accessed_at),
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    tags = if (row.tags.isNotEmpty()) json.decodeFromString(row.tags) else emptyList(),
                    embeddingVector = row.embedding_vector
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 根据对话ID获取记忆列表
     * @param conversationId 对话ID
     * @return 按创建时间排序的记忆列表
     */
    suspend fun getMemoriesByConversationId(conversationId: String): List<AgentMemory> {
        return try {
            database.chatDatabaseQueries.getMemoriesByConversationId(conversationId).executeAsList().map { row ->
                AgentMemory(
                    id = row.id,
                    agentId = row.agent_id,
                    conversationId = row.conversation_id,
                    messageId = row.message_id,
                    content = row.content,
                    memoryType = MemoryType.fromString(row.memory_type),
                    importanceScore = row.importance_score,
                    accessCount = row.access_count.toInt(),
                    lastAccessedAt = Instant.fromEpochMilliseconds(row.last_accessed_at),
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    tags = if (row.tags.isNotEmpty()) json.decodeFromString(row.tags) else emptyList(),
                    embeddingVector = row.embedding_vector
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 根据ID获取记忆
     * @param id 记忆ID
     * @return 记忆对象，如果不存在则返回null
     */
    suspend fun getMemoryById(id: String): AgentMemory? {
        return try {
            val row = database.chatDatabaseQueries.getMemoryById(id).executeAsOneOrNull()
            row?.let {
                AgentMemory(
                    id = it.id,
                    agentId = it.agent_id,
                    conversationId = it.conversation_id,
                    messageId = it.message_id,
                    content = it.content,
                    memoryType = MemoryType.fromString(it.memory_type),
                    importanceScore = it.importance_score,
                    accessCount = it.access_count.toInt(),
                    lastAccessedAt = Instant.fromEpochMilliseconds(it.last_accessed_at),
                    createdAt = Instant.fromEpochMilliseconds(it.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(it.updated_at),
                    tags = if (it.tags.isNotEmpty()) json.decodeFromString(it.tags) else emptyList(),
                    embeddingVector = it.embedding_vector
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 插入新记忆
     * @param memory 记忆对象
     * @return 是否插入成功
     */
    suspend fun insertMemory(memory: AgentMemory): Boolean {
        return try {
            database.chatDatabaseQueries.insertMemory(
                id = memory.id,
                agent_id = memory.agentId,
                conversation_id = memory.conversationId,
                message_id = memory.messageId,
                content = memory.content,
                memory_type = memory.memoryType.value,
                importance_score = memory.importanceScore,
                access_count = memory.accessCount.toLong(),
                last_accessed_at = memory.lastAccessedAt.toEpochMilliseconds(),
                created_at = memory.createdAt.toEpochMilliseconds(),
                updated_at = memory.updatedAt.toEpochMilliseconds(),
                tags = json.encodeToString(memory.tags),
                embedding_vector = memory.embeddingVector
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新记忆
     * @param memory 记忆对象
     * @return 是否更新成功
     */
    suspend fun updateMemory(memory: AgentMemory): Boolean {
        return try {
            database.chatDatabaseQueries.updateMemory(
                content = memory.content,
                memory_type = memory.memoryType.value,
                importance_score = memory.importanceScore,
                access_count = memory.accessCount.toLong(),
                last_accessed_at = memory.lastAccessedAt.toEpochMilliseconds(),
                updated_at = memory.updatedAt.toEpochMilliseconds(),
                tags = json.encodeToString(memory.tags),
                embedding_vector = memory.embeddingVector,
                id = memory.id
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除记忆
     * @param id 记忆ID
     * @return 是否删除成功
     */
    suspend fun deleteMemory(id: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteMemory(id)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除智能体的所有记忆
     * @param agentId 智能体ID
     * @return 是否删除成功
     */
    suspend fun deleteMemoriesByAgentId(agentId: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteMemoriesByAgentId(agentId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 搜索记忆
     * @param agentId 智能体ID
     * @param query 搜索关键词
     * @return 匹配的记忆列表
     */
    suspend fun searchMemories(agentId: String, query: String): List<AgentMemory> {
        return try {
            database.chatDatabaseQueries.searchMemories(agentId, query, query).executeAsList().map { row ->
                AgentMemory(
                    id = row.id,
                    agentId = row.agent_id,
                    conversationId = row.conversation_id,
                    messageId = row.message_id,
                    content = row.content,
                    memoryType = MemoryType.fromString(row.memory_type),
                    importanceScore = row.importance_score,
                    accessCount = row.access_count.toInt(),
                    lastAccessedAt = Instant.fromEpochMilliseconds(row.last_accessed_at),
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    tags = if (row.tags.isNotEmpty()) json.decodeFromString(row.tags) else emptyList(),
                    embeddingVector = row.embedding_vector
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取最重要的记忆
     * @param agentId 智能体ID
     * @param limit 限制数量
     * @return 按重要性排序的记忆列表
     */
    suspend fun getTopMemories(agentId: String, limit: Int): List<AgentMemory> {
        return try {
            database.chatDatabaseQueries.getTopMemories(agentId, limit.toLong()).executeAsList().map { row ->
                AgentMemory(
                    id = row.id,
                    agentId = row.agent_id,
                    conversationId = row.conversation_id,
                    messageId = row.message_id,
                    content = row.content,
                    memoryType = MemoryType.fromString(row.memory_type),
                    importanceScore = row.importance_score,
                    accessCount = row.access_count.toInt(),
                    lastAccessedAt = Instant.fromEpochMilliseconds(row.last_accessed_at),
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    tags = if (row.tags.isNotEmpty()) json.decodeFromString(row.tags) else emptyList(),
                    embeddingVector = row.embedding_vector
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 更新记忆访问信息
     * @param id 记忆ID
     * @param accessTime 访问时间
     * @return 是否更新成功
     */
    suspend fun updateMemoryAccess(id: String, accessTime: Instant): Boolean {
        return try {
            database.chatDatabaseQueries.updateMemoryAccess(
                last_accessed_at = accessTime.toEpochMilliseconds(),
                id = id
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取记忆关联关系
     * @param memoryId 记忆ID
     * @return 关联关系列表
     */
    suspend fun getMemoryRelations(memoryId: String): List<MemoryRelation> {
        return try {
            database.chatDatabaseQueries.getMemoryRelations(memoryId, memoryId).executeAsList().map { row ->
                MemoryRelation(
                    id = row.id,
                    sourceMemoryId = row.source_memory_id,
                    targetMemoryId = row.target_memory_id,
                    relationType = RelationType.fromString(row.relation_type),
                    strength = row.strength,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 插入记忆关联关系
     * @param relation 关联关系对象
     * @return 是否插入成功
     */
    suspend fun insertMemoryRelation(relation: MemoryRelation): Boolean {
        return try {
            database.chatDatabaseQueries.insertMemoryRelation(
                id = relation.id,
                source_memory_id = relation.sourceMemoryId,
                target_memory_id = relation.targetMemoryId,
                relation_type = relation.relationType.value,
                strength = relation.strength,
                created_at = relation.createdAt.toEpochMilliseconds()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除记忆关联关系
     * @param id 关联关系ID
     * @return 是否删除成功
     */
    suspend fun deleteMemoryRelation(id: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteMemoryRelation(id)
            true
        } catch (e: Exception) {
            false
        }
    }
}