package data.database

import app.cash.sqldelight.db.SqlDriver
import domain.model.Agent
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * 智能体数据访问对象
 * 提供智能体相关的数据库操作功能
 */
class AgentDao(private val driver: SqlDriver) {
    
    private val database = ChatDatabase(driver)
    
    /**
     * 获取所有智能体列表
     * @return 按预设优先级、使用次数和名称排序的智能体列表
     */
    suspend fun getAllAgents(): List<Agent> {
        return try {
            database.chatDatabaseQueries.getAllAgents().executeAsList().map { row ->
                Agent(
                    id = row.id,
                    name = row.name,
                    description = row.description,
                    systemPrompt = row.system_prompt,
                    avatar = row.avatar,
                    isPreset = row.is_preset == 1L,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    usageCount = row.usage_count.toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 根据ID获取智能体
     * @param id 智能体ID
     * @return 智能体对象，如果不存在则返回null
     */
    suspend fun getAgentById(id: String): Agent? {
        return try {
            val row = database.chatDatabaseQueries.getAgentById(id).executeAsOneOrNull()
            row?.let {
                Agent(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    systemPrompt = it.system_prompt,
                    avatar = it.avatar,
                    isPreset = it.is_preset == 1L,
                    createdAt = Instant.fromEpochMilliseconds(it.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(it.updated_at),
                    usageCount = it.usage_count.toInt()
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 插入新智能体
     * @param agent 要插入的智能体对象
     * @return 插入是否成功
     */
    suspend fun insertAgent(agent: Agent): Boolean {
        return try {
            database.chatDatabaseQueries.insertAgent(
                id = agent.id,
                name = agent.name,
                description = agent.description,
                system_prompt = agent.systemPrompt,
                avatar = agent.avatar,
                is_preset = if (agent.isPreset) 1L else 0L,
                created_at = agent.createdAt.toEpochMilliseconds(),
                updated_at = agent.updatedAt.toEpochMilliseconds(),
                usage_count = agent.usageCount.toLong()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新智能体信息
     * @param agent 要更新的智能体对象
     * @return 更新是否成功
     */
    suspend fun updateAgent(agent: Agent): Boolean {
        return try {
            database.chatDatabaseQueries.updateAgent(
                name = agent.name,
                description = agent.description,
                system_prompt = agent.systemPrompt,
                avatar = agent.avatar,
                updated_at = agent.updatedAt.toEpochMilliseconds(),
                id = agent.id
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除智能体（仅限自定义智能体）
     * @param id 智能体ID
     * @return 删除是否成功
     */
    suspend fun deleteAgent(id: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteAgent(id)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取预设智能体列表
     * @return 预设智能体列表
     */
    suspend fun getPresetAgents(): List<Agent> {
        return try {
            database.chatDatabaseQueries.getPresetAgents().executeAsList().map { row ->
                Agent(
                    id = row.id,
                    name = row.name,
                    description = row.description,
                    systemPrompt = row.system_prompt,
                    avatar = row.avatar,
                    isPreset = row.is_preset == 1L,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    usageCount = row.usage_count.toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取自定义智能体列表
     * @return 自定义智能体列表
     */
    suspend fun getCustomAgents(): List<Agent> {
        return try {
            database.chatDatabaseQueries.getCustomAgents().executeAsList().map { row ->
                Agent(
                    id = row.id,
                    name = row.name,
                    description = row.description,
                    systemPrompt = row.system_prompt,
                    avatar = row.avatar,
                    isPreset = row.is_preset == 1L,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    usageCount = row.usage_count.toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 增加智能体使用次数
     * @param id 智能体ID
     * @return 更新是否成功
     */
    suspend fun incrementAgentUsage(id: String): Boolean {
        return try {
            database.chatDatabaseQueries.incrementAgentUsage(
                updated_at = Clock.System.now().toEpochMilliseconds(),
                id = id
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 搜索智能体
     * @param query 搜索关键词
     * @return 匹配的智能体列表
     */
    suspend fun searchAgents(query: String): List<Agent> {
        return try {
            database.chatDatabaseQueries.searchAgents(query, query).executeAsList().map { row ->
                Agent(
                    id = row.id,
                    name = row.name,
                    description = row.description,
                    systemPrompt = row.system_prompt,
                    avatar = row.avatar,
                    isPreset = row.is_preset == 1L,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at),
                    usageCount = row.usage_count.toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}