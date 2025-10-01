package domain.mcp

import domain.model.MCPService
import domain.model.MCPCallLog
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement

/**
 * MCP客户端接口
 * 定义与MCP服务通信的核心功能
 */
interface MCPClient {
    
    /**
     * 初始化MCP服务连接
     * @param service MCP服务配置
     * @return 初始化是否成功
     */
    suspend fun initialize(service: MCPService): Boolean
    
    /**
     * 调用MCP服务方法
     * @param methodName 方法名称
     * @param params 请求参数
     * @return MCP调用结果
     */
    suspend fun call(methodName: String, params: Map<String, Any> = emptyMap()): MCPCallResult
    
    /**
     * 获取服务能力列表
     * @return 服务能力列表
     */
    suspend fun getCapabilities(): List<String>
    
    /**
     * 检查服务连接状态
     * @return 连接是否正常
     */
    suspend fun isConnected(): Boolean
    
    /**
     * 关闭连接
     */
    suspend fun disconnect()
}

/**
 * MCP调用结果数据模型
 */
@Serializable
data class MCPCallResult(
    val success: Boolean,
    val data: JsonObject? = null,
    val error: String? = null,
    val executionTime: Long = 0L
)

/**
 * MCP请求数据模型
 */
@Serializable
data class MCPRequest(
    val method: String,
    val params: Map<String, JsonElement> = emptyMap(),
    val id: String? = null
)

/**
 * MCP响应数据模型
 */
@Serializable
data class MCPResponse(
    val result: JsonObject? = null,
    val error: MCPError? = null,
    val id: String? = null
)

/**
 * MCP错误数据模型
 */
@Serializable
data class MCPError(
    val code: Int,
    val message: String,
    val data: JsonObject? = null
)