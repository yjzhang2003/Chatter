package domain.repository

import domain.model.Agent
import domain.model.AgentMemory
import domain.model.MemoryRelation
import domain.model.MCPService
import domain.model.AgentMCPConfig
import domain.model.MCPCallLog
import domain.model.MCPServiceType
import kotlinx.datetime.Instant

/**
 * 智能体仓库接口
 * 定义智能体管理的业务逻辑接口，包括长期记忆和MCP功能
 */
interface AgentRepository {
    
    // ========== 基础智能体管理 ==========
    
    /**
     * 获取所有智能体
     * @return 智能体列表
     */
    suspend fun getAllAgents(): List<Agent>
    
    /**
     * 根据ID获取智能体
     * @param id 智能体ID
     * @return 智能体对象，如果不存在则返回null
     */
    suspend fun getAgentById(id: String): Agent?
    
    /**
     * 创建新智能体
     * @param agent 智能体对象
     * @return 创建是否成功
     */
    suspend fun createAgent(agent: Agent): Boolean
    
    /**
     * 更新智能体
     * @param agent 智能体对象
     * @return 更新是否成功
     */
    suspend fun updateAgent(agent: Agent): Boolean
    
    /**
     * 删除智能体（仅限自定义智能体）
     * @param id 智能体ID
     * @return 删除是否成功
     */
    suspend fun deleteAgent(id: String): Boolean
    
    /**
     * 获取预设智能体列表
     * @return 预设智能体列表
     */
    suspend fun getPresetAgents(): List<Agent>
    
    /**
     * 获取自定义智能体列表
     * @return 自定义智能体列表
     */
    suspend fun getCustomAgents(): List<Agent>
    
    /**
     * 搜索智能体
     * @param query 搜索关键词
     * @return 匹配的智能体列表
     */
    suspend fun searchAgents(query: String): List<Agent>
    
    /**
     * 初始化预设智能体
     * @return 初始化是否成功
     */
    suspend fun initializePresetAgents(): Boolean
    
    // ========== 长期记忆管理 ==========
    
    /**
     * 获取智能体的记忆列表
     * @param agentId 智能体ID
     * @return 记忆列表
     */
    suspend fun getAgentMemories(agentId: String): List<AgentMemory>
    
    /**
     * 根据对话ID获取记忆
     * @param conversationId 对话ID
     * @return 记忆列表
     */
    suspend fun getMemoriesByConversation(conversationId: String): List<AgentMemory>
    
    /**
     * 创建新记忆
     * @param memory 记忆对象
     * @return 创建是否成功
     */
    suspend fun createMemory(memory: AgentMemory): Boolean
    
    /**
     * 更新记忆
     * @param memory 记忆对象
     * @return 更新是否成功
     */
    suspend fun updateMemory(memory: AgentMemory): Boolean
    
    /**
     * 删除记忆
     * @param memoryId 记忆ID
     * @return 删除是否成功
     */
    suspend fun deleteMemory(memoryId: String): Boolean
    
    /**
     * 搜索记忆
     * @param agentId 智能体ID
     * @param query 搜索关键词
     * @return 匹配的记忆列表
     */
    suspend fun searchMemories(agentId: String, query: String): List<AgentMemory>
    
    /**
     * 获取最重要的记忆
     * @param agentId 智能体ID
     * @param limit 限制数量
     * @return 按重要性排序的记忆列表
     */
    suspend fun getTopMemories(agentId: String, limit: Int): List<AgentMemory>
    
    /**
     * 更新记忆访问信息
     * @param memoryId 记忆ID
     * @param accessTime 访问时间
     * @return 更新是否成功
     */
    suspend fun updateMemoryAccess(memoryId: String, accessTime: Instant): Boolean
    
    /**
     * 创建记忆关联关系
     * @param relation 关联关系对象
     * @return 创建是否成功
     */
    suspend fun createMemoryRelation(relation: MemoryRelation): Boolean
    
    /**
     * 获取记忆关联关系
     * @param memoryId 记忆ID
     * @return 关联关系列表
     */
    suspend fun getMemoryRelations(memoryId: String): List<MemoryRelation>
    
    // ========== MCP服务管理 ==========
    
    /**
     * 获取所有MCP服务
     * @return MCP服务列表
     */
    suspend fun getAllMCPServices(): List<MCPService>
    
    /**
     * 根据类型获取MCP服务
     * @param serviceType 服务类型
     * @return MCP服务列表
     */
    suspend fun getMCPServicesByType(serviceType: MCPServiceType): List<MCPService>
    
    /**
     * 获取启用的MCP服务
     * @return 启用的MCP服务列表
     */
    suspend fun getEnabledMCPServices(): List<MCPService>
    
    /**
     * 创建MCP服务
     * @param service MCP服务对象
     * @return 创建是否成功
     */
    suspend fun createMCPService(service: MCPService): Boolean
    
    /**
     * 更新MCP服务
     * @param service MCP服务对象
     * @return 更新是否成功
     */
    suspend fun updateMCPService(service: MCPService): Boolean
    
    /**
     * 删除MCP服务
     * @param serviceId 服务ID
     * @return 删除是否成功
     */
    suspend fun deleteMCPService(serviceId: String): Boolean
    
    /**
     * 获取智能体的MCP配置
     * @param agentId 智能体ID
     * @return MCP配置列表
     */
    suspend fun getAgentMCPConfigs(agentId: String): List<AgentMCPConfig>
    
    /**
     * 获取智能体启用的MCP配置
     * @param agentId 智能体ID
     * @return 启用的MCP配置列表
     */
    suspend fun getEnabledAgentMCPConfigs(agentId: String): List<AgentMCPConfig>
    
    /**
     * 创建智能体MCP配置
     * @param config MCP配置对象
     * @return 创建是否成功
     */
    suspend fun createAgentMCPConfig(config: AgentMCPConfig): Boolean
    
    /**
     * 更新智能体MCP配置
     * @param config MCP配置对象
     * @return 更新是否成功
     */
    suspend fun updateAgentMCPConfig(config: AgentMCPConfig): Boolean
    
    /**
     * 删除智能体MCP配置
     * @param configId 配置ID
     * @return 删除是否成功
     */
    suspend fun deleteAgentMCPConfig(configId: String): Boolean
    
    /**
     * 记录MCP调用日志
     * @param log MCP调用日志对象
     * @return 记录是否成功
     */
    suspend fun logMCPCall(log: MCPCallLog): Boolean
    
    /**
     * 获取MCP调用日志
     * @param agentId 智能体ID
     * @param limit 限制数量
     * @return MCP调用日志列表
     */
    suspend fun getMCPCallLogs(agentId: String, limit: Int): List<MCPCallLog>
    
    /**
     * 清理过期的MCP调用日志
     * @param beforeTime 删除此时间之前的日志
     * @return 清理是否成功
     */
    suspend fun cleanupOldMCPCallLogs(beforeTime: Instant): Boolean
    
    /**
     * 初始化预设MCP服务
     * @return 初始化是否成功
     */
    suspend fun initializePresetMCPServices(): Boolean
}