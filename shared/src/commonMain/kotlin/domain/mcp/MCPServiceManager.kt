package domain.mcp

import domain.model.MCPService
import domain.model.AgentMCPConfig
import domain.model.MCPCallLog
import kotlinx.coroutines.flow.Flow

/**
 * MCP服务管理器接口
 * 负责管理MCP服务的生命周期和调度
 */
interface MCPServiceManager {
    
    /**
     * 初始化服务管理器
     */
    suspend fun initialize()
    
    /**
     * 注册MCP服务
     * @param service MCP服务配置
     * @return 注册是否成功
     */
    suspend fun registerService(service: MCPService): Boolean
    
    /**
     * 注销MCP服务
     * @param serviceId 服务ID
     * @return 注销是否成功
     */
    suspend fun unregisterService(serviceId: String): Boolean
    
    /**
     * 获取可用的MCP服务
     * @param agentId 智能体ID
     * @return 可用服务列表
     */
    suspend fun getAvailableServices(agentId: String): List<MCPService>
    
    /**
     * 调用MCP服务
     * @param agentId 智能体ID
     * @param serviceId 服务ID
     * @param methodName 方法名称
     * @param params 请求参数
     * @param conversationId 对话ID（可选）
     * @param messageId 消息ID（可选）
     * @return MCP调用结果
     */
    suspend fun callService(
        agentId: String,
        serviceId: String,
        methodName: String,
        params: Map<String, Any> = emptyMap(),
        conversationId: String? = null,
        messageId: String? = null
    ): MCPCallResult
    
    /**
     * 获取服务状态
     * @param serviceId 服务ID
     * @return 服务状态
     */
    suspend fun getServiceStatus(serviceId: String): MCPServiceStatus
    
    /**
     * 监听服务状态变化
     * @return 服务状态流
     */
    fun observeServiceStatus(): Flow<Map<String, MCPServiceStatus>>
    
    /**
     * 获取调用历史
     * @param agentId 智能体ID
     * @param limit 限制数量
     * @return 调用历史列表
     */
    suspend fun getCallHistory(agentId: String, limit: Int = 50): List<MCPCallLog>
    
    /**
     * 清理资源
     */
    suspend fun cleanup()
}

/**
 * MCP服务状态枚举
 */
enum class MCPServiceStatus {
    DISCONNECTED,    // 未连接
    CONNECTING,      // 连接中
    CONNECTED,       // 已连接
    ERROR,           // 错误状态
    DISABLED         // 已禁用
}

/**
 * MCP服务配置
 */
data class MCPServiceConfig(
    val service: MCPService,
    val agentConfig: AgentMCPConfig? = null,
    val client: MCPClient? = null,
    val status: MCPServiceStatus = MCPServiceStatus.DISCONNECTED
)