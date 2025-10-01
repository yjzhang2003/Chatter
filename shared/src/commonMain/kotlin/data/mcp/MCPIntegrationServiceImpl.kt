package data.mcp

import domain.mcp.MCPIntegrationService
import domain.mcp.MCPServiceManager
import domain.mcp.MCPProcessResult
import domain.mcp.MCPTool
import domain.mcp.MCPToolCall
import domain.mcp.MCPToolResult
import domain.mcp.MCPToolParameter
import domain.mcp.MCPServiceStatus
import domain.model.Agent
import domain.model.MCPService
import domain.model.MCPServiceType
import domain.repository.AgentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * MCP集成服务实现
 * 负责将MCP功能集成到聊天系统中
 */
class MCPIntegrationServiceImpl(
    private val mcpServiceManager: MCPServiceManager,
    private val agentRepository: AgentRepository,
    private val json: Json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : MCPIntegrationService {
    
    // 工具名称到服务映射
    private val toolServiceMapping = mutableMapOf<String, String>()
    
    /**
     * 初始化MCP集成服务
     */
    override suspend fun initialize() {
        mcpServiceManager.initialize()
        buildToolServiceMapping()
    }
    
    /**
     * 处理智能体消息，检测并执行MCP调用
     */
    override suspend fun processMessage(
        agent: Agent,
        message: String,
        conversationId: String
    ): MCPProcessResult {
        val toolCalls = detectToolCalls(message)
        
        if (toolCalls.isEmpty()) {
            return MCPProcessResult.NoAction(message)
        }
        
        // 验证智能体是否有权限使用这些工具
        val availableTools = getAvailableTools(agent.id)
        val availableToolNames = availableTools.map { it.name }.toSet()
        
        val validToolCalls = toolCalls.filter { call ->
            availableToolNames.contains(call.toolName)
        }
        
        if (validToolCalls.isEmpty()) {
            return MCPProcessResult.Error("没有可用的工具调用权限")
        }
        
        // 执行工具调用
        return try {
            val results = mutableListOf<MCPToolResult>()
            for (toolCall in validToolCalls) {
                val result = executeTool(
                    agentId = agent.id,
                    toolName = toolCall.toolName,
                    params = toolCall.params,
                    conversationId = conversationId,
                    messageId = generateRandomId()
                )
                results.add(result)
            }
            
            // 检查是否所有工具调用都成功
            val allSuccessful = results.all { it.success }
            if (allSuccessful) {
                val processedMessage = buildProcessedMessage(message, results)
                MCPProcessResult.ToolCallExecuted(processedMessage, validToolCalls, results)
            } else {
                val errors = results.filter { !it.success }.mapNotNull { it.error }
                MCPProcessResult.Error("工具调用失败: ${errors.joinToString(", ")}")
            }
        } catch (e: Exception) {
            MCPProcessResult.Error("工具调用异常: ${e.message}")
        }
    }
    
    /**
     * 获取智能体可用的MCP工具列表
     */
    override suspend fun getAvailableTools(agentId: String): List<MCPTool> {
        val availableServices = mcpServiceManager.getAvailableServices(agentId)
        val tools = mutableListOf<MCPTool>()
        
        for (service in availableServices) {
            val serviceTools = getServiceTools(service)
            tools.addAll(serviceTools)
        }
        
        return tools
    }
    
    /**
     * 执行MCP工具调用
     */
    override suspend fun executeTool(
        agentId: String,
        toolName: String,
        params: Map<String, Any>,
        conversationId: String,
        messageId: String
    ): MCPToolResult {
        val serviceId = toolServiceMapping[toolName]
        if (serviceId == null) {
            return MCPToolResult(
                success = false,
                error = "未找到工具对应的服务: $toolName"
            )
        }
        
        val methodName = getMethodNameForTool(toolName)
        val result = mcpServiceManager.callService(
            agentId = agentId,
            serviceId = serviceId,
            methodName = methodName,
            params = params,
            conversationId = conversationId,
            messageId = messageId
        )
        
        return MCPToolResult(
            success = result.success,
            result = result.data?.toString(),
            error = result.error,
            executionTime = result.executionTime,
            toolCall = MCPToolCall(
                toolName = toolName,
                serviceId = serviceId,
                methodName = methodName,
                params = params,
                callId = generateRandomId()
            )
        )
    }
    
    /**
     * 监听MCP服务状态变化
     */
    override fun observeServiceStatus(): Flow<Map<String, MCPServiceStatus>> {
        return mcpServiceManager.observeServiceStatus()
    }
    
    /**
     * 清理资源
     */
    override suspend fun cleanup() {
        mcpServiceManager.cleanup()
        toolServiceMapping.clear()
    }
    
    /**
     * 检测消息中的工具调用
     */
    private fun detectToolCalls(message: String): List<MCPToolCall> {
        val toolCalls = mutableListOf<MCPToolCall>()
        
        // 简单的工具调用检测逻辑
        // 实际实现中可能需要更复杂的解析逻辑
        val toolCallPattern = Regex("""@(\w+)\((.*?)\)""")
        val matches = toolCallPattern.findAll(message)
        
        for (match in matches) {
            val toolName = match.groupValues[1]
            val paramsString = match.groupValues[2]
            val params = parseToolParams(paramsString)
            
            val serviceId = toolServiceMapping[toolName]
            if (serviceId != null) {
                toolCalls.add(
                    MCPToolCall(
                        toolName = toolName,
                        serviceId = serviceId,
                        methodName = getMethodNameForTool(toolName),
                        params = params,
                        callId = generateRandomId()
                    )
                )
            }
        }
        
        return toolCalls
    }
    
    /**
     * 解析工具参数
     */
    private fun parseToolParams(paramsString: String): Map<String, Any> {
        if (paramsString.isBlank()) return emptyMap()
        
        val params = mutableMapOf<String, Any>()
        val paramPairs = paramsString.split(",")
        
        for (pair in paramPairs) {
            val keyValue = pair.split("=", limit = 2)
            if (keyValue.size == 2) {
                val key = keyValue[0].trim()
                val value = keyValue[1].trim().removeSurrounding("\"", "\"")
                params[key] = value
            }
        }
        
        return params
    }
    
    /**
     * 获取服务的工具列表
     */
    private fun getServiceTools(service: MCPService): List<MCPTool> {
        return when (service.serviceType) {
            MCPServiceType.RIDE_HAILING -> listOf(
                MCPTool(
                    name = "book_ride",
                    displayName = "预约打车",
                    description = "预约网约车服务",
                    serviceId = service.id,
                    serviceName = service.displayName,
                    parameters = mapOf(
                        "from" to MCPToolParameter("from", "string", "出发地", true),
                        "to" to MCPToolParameter("to", "string", "目的地", true),
                        "ride_type" to MCPToolParameter("ride_type", "string", "车型", false, "standard")
                    )
                ),
                MCPTool(
                    name = "cancel_ride",
                    displayName = "取消打车",
                    description = "取消已预约的网约车",
                    serviceId = service.id,
                    serviceName = service.displayName,
                    parameters = mapOf(
                        "ride_id" to MCPToolParameter("ride_id", "string", "订单ID", true)
                    )
                )
            )
            MCPServiceType.WEATHER -> listOf(
                MCPTool(
                    name = "get_weather",
                    displayName = "查询天气",
                    description = "查询指定地点的天气信息",
                    serviceId = service.id,
                    serviceName = service.displayName,
                    parameters = mapOf(
                        "location" to MCPToolParameter("location", "string", "地点", true),
                        "days" to MCPToolParameter("days", "number", "天数", false, 1)
                    )
                )
            )
            MCPServiceType.GITHUB -> listOf(
                MCPTool(
                    name = "create_issue",
                    displayName = "创建Issue",
                    description = "在GitHub仓库中创建Issue",
                    serviceId = service.id,
                    serviceName = service.displayName,
                    parameters = mapOf(
                        "repo" to MCPToolParameter("repo", "string", "仓库名", true),
                        "title" to MCPToolParameter("title", "string", "标题", true),
                        "body" to MCPToolParameter("body", "string", "内容", false)
                    )
                )
            )
            MCPServiceType.CALENDAR -> listOf(
                MCPTool(
                    name = "create_event",
                    displayName = "创建日程",
                    description = "创建日历事件",
                    serviceId = service.id,
                    serviceName = service.displayName,
                    parameters = mapOf(
                        "title" to MCPToolParameter("title", "string", "标题", true),
                        "start_time" to MCPToolParameter("start_time", "string", "开始时间", true),
                        "end_time" to MCPToolParameter("end_time", "string", "结束时间", true)
                    )
                )
            )
            else -> emptyList()
        }
    }
    
    /**
     * 生成随机ID
     */
    private fun generateRandomId(): String {
        return Random.nextLong().toString()
    }
    
    /**
     * 构建处理后的消息
     */
    private fun buildProcessedMessage(originalMessage: String, results: List<MCPToolResult>): String {
        val resultTexts = results.mapNotNull { it.result }
        return if (resultTexts.isNotEmpty()) {
            "$originalMessage\n\n工具执行结果:\n${resultTexts.joinToString("\n")}"
        } else {
            originalMessage
        }
    }
    
    /**
     * 构建工具到服务的映射
     */
    private suspend fun buildToolServiceMapping() {
        val allServices = agentRepository.getAllMCPServices()
        
        for (service in allServices) {
            val tools = getServiceTools(service)
            for (tool in tools) {
                toolServiceMapping[tool.name] = service.id
            }
        }
    }
    
    /**
     * 获取工具对应的方法名
     */
    private fun getMethodNameForTool(toolName: String): String {
        return toolName // 简单映射，实际可能需要更复杂的转换
    }
}