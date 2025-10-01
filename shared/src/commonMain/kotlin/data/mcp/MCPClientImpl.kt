package data.mcp

import domain.mcp.MCPClient
import domain.mcp.MCPCallResult
import domain.mcp.MCPRequest
import domain.mcp.MCPResponse
import domain.mcp.MCPError
import domain.model.MCPService
import domain.model.AuthType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * MCP客户端实现
 * 基于HTTP协议与MCP服务进行通信
 */
class MCPClientImpl(
    private val httpClient: HttpClient,
    private val json: Json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : MCPClient {
    
    private var service: MCPService? = null
    private var isInitialized = false
    
    /**
     * 初始化MCP服务连接
     */
    override suspend fun initialize(service: MCPService): Boolean {
        return try {
            this.service = service
            
            // 测试连接
            val testResult = testConnection()
            isInitialized = testResult
            testResult
        } catch (e: Exception) {
            isInitialized = false
            false
        }
    }
    
    /**
     * 调用MCP服务方法
     */
    override suspend fun call(methodName: String, params: Map<String, Any>): MCPCallResult {
        val currentService = service ?: return MCPCallResult(
            success = false,
            error = "服务未初始化"
        )
        
        if (!isInitialized) {
            return MCPCallResult(
                success = false,
                error = "服务连接未建立"
            )
        }
        
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        return try {
            val jsonParams = params.mapValues { (_, value) ->
                when (value) {
                    is String -> JsonPrimitive(value)
                    is Number -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    else -> JsonPrimitive(value.toString())
                }
            }
            
            val request = MCPRequest(
                method = methodName,
                params = jsonParams,
                id = generateRequestId()
            )
            
            val response = withTimeout(30.seconds) {
                httpClient.post(currentService.endpointUrl) {
                    contentType(ContentType.Application.Json)
                    setAuth(currentService)
                    setBody(json.encodeToString(MCPRequest.serializer(), request))
                }
            }
            
            val endTime = Clock.System.now().toEpochMilliseconds()
            val executionTime = endTime - startTime
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val responseBody = response.bodyAsText()
                    val mcpResponse = json.decodeFromString<MCPResponse>(responseBody)
                    
                    if (mcpResponse.error != null) {
                        MCPCallResult(
                            success = false,
                            error = mcpResponse.error.message,
                            executionTime = executionTime
                        )
                    } else {
                        MCPCallResult(
                            success = true,
                            data = mcpResponse.result,
                            executionTime = executionTime
                        )
                    }
                }
                else -> {
                    MCPCallResult(
                        success = false,
                        error = "HTTP错误: ${response.status.value} ${response.status.description}",
                        executionTime = executionTime
                    )
                }
            }
        } catch (e: Exception) {
            val endTime = Clock.System.now().toEpochMilliseconds()
            MCPCallResult(
                success = false,
                error = "调用异常: ${e.message}",
                executionTime = endTime - startTime
            )
        }
    }
    
    /**
     * 获取服务能力列表
     */
    override suspend fun getCapabilities(): List<String> {
        val currentService = service ?: return emptyList()
        
        return try {
            val result = call("capabilities", emptyMap())
            if (result.success && result.data != null) {
                val capabilities = result.data["capabilities"]?.jsonArray
                capabilities?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: currentService.capabilities
            } else {
                currentService.capabilities
            }
        } catch (e: Exception) {
            currentService.capabilities
        }
    }
    
    /**
     * 检查服务连接状态
     */
    override suspend fun isConnected(): Boolean {
        return isInitialized && testConnection()
    }
    
    /**
     * 关闭连接
     */
    override suspend fun disconnect() {
        isInitialized = false
        service = null
    }
    
    /**
     * 测试连接
     */
    private suspend fun testConnection(): Boolean {
        val currentService = service ?: return false
        
        return try {
            val response = withTimeout(10.seconds) {
                httpClient.get("${currentService.endpointUrl}/health") {
                    setAuth(currentService)
                }
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            // 如果健康检查失败，尝试调用基本方法
            try {
                val result = call("ping", emptyMap())
                result.success
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 设置认证信息
     */
    private fun HttpRequestBuilder.setAuth(service: MCPService) {
        when (service.authType) {
            AuthType.API_KEY -> {
                val apiKey = service.authConfig["api_key"]
                val headerName = service.authConfig["api_key_header"] ?: "X-API-Key"
                if (apiKey != null) {
                    header(headerName, apiKey)
                }
            }
            AuthType.BEARER -> {
                val token = service.authConfig["token"]
                val headerName = service.authConfig["token_header"] ?: "Authorization"
                if (token != null) {
                    header(headerName, "Bearer $token")
                }
            }
            AuthType.OAUTH -> {
                val accessToken = service.authConfig["access_token"]
                if (accessToken != null) {
                    header("Authorization", "Bearer $accessToken")
                }
            }
            AuthType.NONE -> {
                // 无需认证
            }
        }
    }
    
    /**
     * 生成请求ID
     */
    private fun generateRequestId(): String {
        return "req_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}