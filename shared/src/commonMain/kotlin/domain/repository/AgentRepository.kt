package domain.repository

import domain.model.Agent

/**
 * 智能体仓库接口
 * 定义智能体管理的业务逻辑接口
 */
interface AgentRepository {
    
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
}