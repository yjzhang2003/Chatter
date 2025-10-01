package domain.mcp

import domain.model.Agent
import domain.model.ChatMessage
import domain.model.MCPService
import kotlinx.coroutines.flow.Flow

/**
 * MCP集成服务接口
 * 负责将MCP功能集成到聊天系统中
 */
interface MCPIntegrationService {
    
    /**
     * 初始化MCP集成服务
     */
    suspend fun initialize()
    
    /**
     * 处理智能体消息，检测并执行MCP调用
     * @param agent 智能体
     * @param message 消息内容
     * @param conversationId 对话ID
     * @return 处理后的消息内容
     */
    suspend fun processMessage(
        agent: Agent,
        message: String,
        conversationId: String
    ): MCPProcessResult
    
    /**
     * 获取智能体可用的MCP工具列表
     * @param agentId 智能体ID
     * @return MCP工具列表
     */
    suspend fun getAvailableTools(agentId: String): List<MCPTool>
    
    /**
     * 执行MCP工具调用
     * @param agentId 智能体ID
     * @param toolName 工具名称
     * @param params 参数
     * @param conversationId 对话ID
     * @param messageId 消息ID
     * @return 执行结果
     */
    suspend fun executeTool(
        agentId: String,
        toolName: String,
        params: Map<String, Any>,
        conversationId: String,
        messageId: String
    ): MCPToolResult
    
    /**
     * 监听MCP服务状态变化
     * @return 状态变化流
     */
    fun observeServiceStatus(): Flow<Map<String, MCPServiceStatus>>
    
    /**
     * 清理资源
     */
    suspend fun cleanup()
}

/**
 * MCP处理结果的密封类
 * 表示MCP消息处理的不同结果状态
 */
sealed class MCPProcessResult {
    /**
     * 工具调用已执行完成
     * @param result 执行结果
     * @param toolCalls 执行的工具调用列表
     * @param results 工具执行结果列表
     */
    data class ToolCallExecuted(
        val result: String,
        val toolCalls: List<MCPToolCall> = emptyList(),
        val results: List<MCPToolResult> = emptyList()
    ) : MCPProcessResult()
    
    /**
     * 工具调用待处理
     * @param toolCall 待处理的工具调用
     */
    data class ToolCallPending(val toolCall: MCPToolCall) : MCPProcessResult()
    
    /**
     * 处理出错
     * @param message 错误信息
     */
    data class Error(val message: String) : MCPProcessResult()
    
    /**
     * 无需MCP操作
     * @param message 原始消息
     */
    data class NoAction(val message: String) : MCPProcessResult()
}

/**
 * MCP工具定义
 */
data class MCPTool(
    val name: String,
    val displayName: String,
    val description: String,
    val serviceId: String,
    val serviceName: String,
    val parameters: Map<String, MCPToolParameter> = emptyMap()
)

/**
 * MCP工具参数定义
 */
data class MCPToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = false,
    val defaultValue: Any? = null
)

/**
 * MCP工具调用
 */
data class MCPToolCall(
    val toolName: String,
    val serviceId: String,
    val methodName: String,
    val params: Map<String, Any>,
    val callId: String
)

/**
 * MCP工具执行结果
 */
data class MCPToolResult(
    val success: Boolean,
    val result: String? = null,
    val error: String? = null,
    val executionTime: Long = 0L,
    val toolCall: MCPToolCall? = null
)