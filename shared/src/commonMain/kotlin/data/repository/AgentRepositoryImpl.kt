package data.repository

import data.database.AgentDao
import data.database.AgentMemoryDao
import data.database.MCPServiceDao
import domain.model.Agent
import domain.model.AgentMemory
import domain.model.MemoryRelation
import domain.model.MCPService
import domain.model.AgentMCPConfig
import domain.model.MCPCallLog
import domain.model.MCPServiceType
import domain.model.AuthType
import domain.repository.AgentRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * 智能体仓库实现类
 * 实现智能体管理的具体业务逻辑，包括长期记忆和MCP功能
 */
class AgentRepositoryImpl(
    private val agentDao: AgentDao,
    private val agentMemoryDao: AgentMemoryDao,
    private val mcpServiceDao: MCPServiceDao
) : AgentRepository {
    
    // ========== 基础智能体管理 ==========
    
    override suspend fun getAllAgents(): List<Agent> {
        return agentDao.getAllAgents()
    }
    
    override suspend fun getAgentById(id: String): Agent? {
        return agentDao.getAgentById(id)
    }
    
    override suspend fun createAgent(agent: Agent): Boolean {
        return agentDao.insertAgent(agent)
    }
    
    override suspend fun updateAgent(agent: Agent): Boolean {
        return agentDao.updateAgent(agent)
    }
    
    override suspend fun deleteAgent(id: String): Boolean {
        // 只允许删除自定义智能体
        val agent = agentDao.getAgentById(id)
        return if (agent?.isCustom() == true) {
            // 删除智能体时同时删除相关的记忆和MCP配置
            agentMemoryDao.deleteMemoriesByAgentId(id)
            mcpServiceDao.deleteAgentMCPConfigsByAgentId(id)
            agentDao.deleteAgent(id)
        } else {
            false
        }
    }
    
    override suspend fun getPresetAgents(): List<Agent> {
        return agentDao.getPresetAgents()
    }
    
    override suspend fun getCustomAgents(): List<Agent> {
        return agentDao.getCustomAgents()
    }
    
    override suspend fun searchAgents(query: String): List<Agent> {
        return agentDao.searchAgents(query)
    }
    
    override suspend fun initializePresetAgents(): Boolean {
        return try {
            val existingPresets = getPresetAgents()
            val presetAgents = getDefaultPresetAgents()
            
            // 只添加不存在的预设智能体
            presetAgents.forEach { preset ->
                if (existingPresets.none { it.id == preset.id }) {
                    agentDao.insertAgent(preset)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // ========== 长期记忆管理 ==========
    
    override suspend fun getAgentMemories(agentId: String): List<AgentMemory> {
        return agentMemoryDao.getMemoriesByAgentId(agentId)
    }
    
    override suspend fun getMemoriesByConversation(conversationId: String): List<AgentMemory> {
        return agentMemoryDao.getMemoriesByConversationId(conversationId)
    }
    
    override suspend fun createMemory(memory: AgentMemory): Boolean {
        return agentMemoryDao.insertMemory(memory)
    }
    
    override suspend fun updateMemory(memory: AgentMemory): Boolean {
        return agentMemoryDao.updateMemory(memory)
    }
    
    override suspend fun deleteMemory(memoryId: String): Boolean {
        return agentMemoryDao.deleteMemory(memoryId)
    }
    
    override suspend fun searchMemories(agentId: String, query: String): List<AgentMemory> {
        return agentMemoryDao.searchMemories(agentId, query)
    }
    
    override suspend fun getTopMemories(agentId: String, limit: Int): List<AgentMemory> {
        return agentMemoryDao.getTopMemories(agentId, limit)
    }
    
    override suspend fun updateMemoryAccess(memoryId: String, accessTime: Instant): Boolean {
        return agentMemoryDao.updateMemoryAccess(memoryId, accessTime)
    }
    
    override suspend fun createMemoryRelation(relation: MemoryRelation): Boolean {
        return agentMemoryDao.insertMemoryRelation(relation)
    }
    
    override suspend fun getMemoryRelations(memoryId: String): List<MemoryRelation> {
        return agentMemoryDao.getMemoryRelations(memoryId)
    }
    
    // ========== MCP服务管理 ==========
    
    override suspend fun getAllMCPServices(): List<MCPService> {
        return mcpServiceDao.getAllMCPServices()
    }
    
    override suspend fun getMCPServicesByType(serviceType: MCPServiceType): List<MCPService> {
        return mcpServiceDao.getMCPServicesByType(serviceType)
    }
    
    override suspend fun getEnabledMCPServices(): List<MCPService> {
        return mcpServiceDao.getEnabledMCPServices()
    }
    
    override suspend fun createMCPService(service: MCPService): Boolean {
        return mcpServiceDao.insertMCPService(service)
    }
    
    override suspend fun updateMCPService(service: MCPService): Boolean {
        return mcpServiceDao.updateMCPService(service)
    }
    
    override suspend fun deleteMCPService(serviceId: String): Boolean {
        return mcpServiceDao.deleteMCPService(serviceId)
    }
    
    override suspend fun getAgentMCPConfigs(agentId: String): List<AgentMCPConfig> {
        return mcpServiceDao.getAgentMCPConfigs(agentId)
    }
    
    override suspend fun getEnabledAgentMCPConfigs(agentId: String): List<AgentMCPConfig> {
        return mcpServiceDao.getEnabledAgentMCPConfigs(agentId)
    }
    
    override suspend fun createAgentMCPConfig(config: AgentMCPConfig): Boolean {
        return mcpServiceDao.insertAgentMCPConfig(config)
    }
    
    override suspend fun updateAgentMCPConfig(config: AgentMCPConfig): Boolean {
        return mcpServiceDao.updateAgentMCPConfig(config)
    }
    
    override suspend fun deleteAgentMCPConfig(configId: String): Boolean {
        return mcpServiceDao.deleteAgentMCPConfig(configId)
    }
    
    override suspend fun configureAgentMCPService(agentId: String, mcpServiceId: String, isEnabled: Boolean): Boolean {
        return try {
            // 检查是否已存在配置
            val existingConfigs = getAgentMCPConfigs(agentId)
            val existingConfig = existingConfigs.find { it.mcpServiceId == mcpServiceId }
            
            if (existingConfig != null) {
                // 更新现有配置
                val updatedConfig = existingConfig.copy(
                    isEnabled = isEnabled,
                    updatedAt = Clock.System.now()
                )
                updateAgentMCPConfig(updatedConfig)
            } else if (isEnabled) {
                // 创建新配置（仅当启用时）
                val newConfig = AgentMCPConfig(
                    id = "amc_${Clock.System.now().toEpochMilliseconds()}",
                    agentId = agentId,
                    mcpServiceId = mcpServiceId,
                    isEnabled = true,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now()
                )
                createAgentMCPConfig(newConfig)
            } else {
                // 如果不存在配置且要禁用，直接返回成功
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun logMCPCall(log: MCPCallLog): Boolean {
        return mcpServiceDao.insertMCPCallLog(log)
    }
    
    override suspend fun getMCPCallLogs(agentId: String, limit: Int): List<MCPCallLog> {
        return mcpServiceDao.getMCPCallLogs(agentId, limit)
    }
    
    override suspend fun cleanupOldMCPCallLogs(beforeTime: Instant): Boolean {
        return mcpServiceDao.deleteOldMCPCallLogs(beforeTime)
    }
    
    override suspend fun initializePresetMCPServices(): Boolean {
        return try {
            val existingServices = getAllMCPServices()
            val presetServices = getDefaultPresetMCPServices()
            
            // 只添加不存在的预设MCP服务
            presetServices.forEach { preset ->
                if (existingServices.none { it.id == preset.id }) {
                    mcpServiceDao.insertMCPService(preset)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取默认预设智能体列表
     */
    private fun getDefaultPresetAgents(): List<Agent> {
        return listOf(
            Agent.createPreset(
                id = "default_assistant",
                name = "通用助手",
                description = "友好、有帮助的AI助手，可以协助处理各种任务",
                systemPrompt = "你是一个友好、有帮助的AI助手。请以礼貌、专业的方式回答用户的问题，并尽力提供准确、有用的信息。",
                avatar = "🤖"
            ),
            Agent.createPreset(
                id = "translator",
                name = "翻译专家",
                description = "专业的多语言翻译助手，支持准确的语言转换",
                systemPrompt = "你是一个专业的翻译专家，精通多种语言。请准确、自然地翻译用户提供的文本，保持原文的语调和含义。如果用户没有指定目标语言，请询问需要翻译成哪种语言。",
                avatar = "🌐"
            ),
            Agent.createPreset(
                id = "programmer",
                name = "编程助手",
                description = "专业的编程和技术问题解答助手",
                systemPrompt = "你是一个经验丰富的程序员和技术专家。请帮助用户解决编程问题，提供清晰的代码示例，解释技术概念，并给出最佳实践建议。请确保代码的正确性和可读性。",
                avatar = "💻"
            ),
            Agent.createPreset(
                id = "writer",
                name = "创意写手",
                description = "专业的创意写作和文案助手",
                systemPrompt = "你是一个富有创意的写作专家。请帮助用户创作各种类型的文本，包括故事、文章、广告文案等。注重文字的美感、逻辑性和吸引力，根据用户需求调整写作风格。",
                avatar = "✍️"
            ),
            Agent.createPreset(
                id = "teacher",
                name = "学习导师",
                description = "耐心的教学助手，擅长解释复杂概念",
                systemPrompt = "你是一个耐心、专业的教师。请用简单易懂的方式解释复杂的概念，提供学习建议，帮助用户理解和掌握知识。根据用户的学习水平调整解释的深度和方式。",
                avatar = "👨‍🏫"
            ),
            Agent.createPreset(
                id = "analyst",
                name = "数据分析师",
                description = "专业的数据分析和商业洞察助手",
                systemPrompt = "你是一个专业的数据分析师。请帮助用户分析数据、解读趋势、提供商业洞察。用清晰的逻辑和数据支持你的分析结论，并提供可行的建议。",
                avatar = "📊"
            )
        )
    }
    
    /**
     * 获取默认预设MCP服务列表
     */
    private fun getDefaultPresetMCPServices(): List<MCPService> {
        val now = Clock.System.now()
        return listOf(
            MCPService(
                id = "didi_transport",
                name = "滴滴出行",
                displayName = "滴滴出行",
                description = "提供打车、预约车、查看行程等服务",
                serviceType = MCPServiceType.RIDE_HAILING,
                endpointUrl = "https://api.didi.com/mcp",
                apiVersion = "2.0",
                authType = AuthType.API_KEY,
                authConfig = mapOf("api_key_header" to "X-Didi-Key"),
                capabilities = listOf("book_ride", "cancel_ride", "get_ride_status", "estimate_price"),
                isEnabled = true,
                isSystem = true,
                createdAt = now,
                updatedAt = now
            ),
            MCPService(
                id = "github_dev",
                name = "GitHub开发",
                displayName = "GitHub开发",
                description = "提供代码仓库管理、Issue跟踪、PR管理等服务",
                serviceType = MCPServiceType.GITHUB,
                endpointUrl = "https://api.github.com/mcp",
                apiVersion = "3.0",
                authType = AuthType.BEARER,
                authConfig = mapOf("token_header" to "Authorization"),
                capabilities = listOf("create_repo", "manage_issues", "create_pr", "get_commits"),
                isEnabled = true,
                isSystem = true,
                createdAt = now,
                updatedAt = now
            ),
            MCPService(
                id = "weather_service",
                name = "天气服务",
                displayName = "天气服务",
                description = "提供实时天气、天气预报、气象预警等服务",
                serviceType = MCPServiceType.WEATHER,
                endpointUrl = "https://api.weather.com/mcp",
                apiVersion = "1.0",
                authType = AuthType.API_KEY,
                authConfig = mapOf("api_key_header" to "X-Weather-Key"),
                capabilities = listOf("current_weather", "forecast", "weather_alerts", "historical_data"),
                isEnabled = true,
                isSystem = true,
                createdAt = now,
                updatedAt = now
            ),
            MCPService(
                id = "calendar_manage",
                name = "日历管理",
                displayName = "日历管理",
                description = "提供日程安排、提醒设置、会议管理等服务",
                serviceType = MCPServiceType.CALENDAR,
                endpointUrl = "https://api.calendar.com/mcp",
                apiVersion = "1.0",
                authType = AuthType.OAUTH,
                authConfig = mapOf(
                    "client_id" to "",
                    "client_secret" to "",
                    "scope" to "calendar.read,calendar.write"
                ),
                capabilities = listOf("create_event", "update_event", "delete_event", "get_schedule"),
                isEnabled = true,
                isSystem = true,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}