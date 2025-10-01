package data.mcp

import domain.mcp.MCPServiceManager
import domain.mcp.MCPClient
import domain.mcp.MCPCallResult
import domain.mcp.MCPServiceStatus
import domain.mcp.MCPServiceConfig
import domain.model.MCPService
import domain.model.AgentMCPConfig
import domain.model.MCPCallLog
import domain.model.CallStatus
import domain.repository.AgentRepository
import io.ktor.client.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * MCP服务管理器实现
 * 负责管理MCP服务的生命周期、调度和状态监控
 */
class MCPServiceManagerImpl(
    private val agentRepository: AgentRepository,
    private val httpClient: HttpClient,
    private val json: Json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : MCPServiceManager {
    
    // 服务配置缓存
    private val serviceConfigs = mutableMapOf<String, MCPServiceConfig>()
    
    // 服务状态流
    private val _serviceStatusFlow = MutableStateFlow<Map<String, MCPServiceStatus>>(emptyMap())
    
    // 并发控制
    private val mutex = Mutex()
    
    /**
     * 初始化服务管理器
     */
    override suspend fun initialize() {
        mutex.withLock {
            // 加载所有启用的MCP服务
            val enabledServices = agentRepository.getEnabledMCPServices()
            
            for (service in enabledServices) {
                val client = MCPClientImpl(httpClient, json)
                val config = MCPServiceConfig(
                    service = service,
                    client = client,
                    status = MCPServiceStatus.DISCONNECTED
                )
                
                serviceConfigs[service.id] = config
                
                // 尝试初始化连接
                try {
                    updateServiceStatus(service.id, MCPServiceStatus.CONNECTING)
                    val initialized = client.initialize(service)
                    updateServiceStatus(
                        service.id, 
                        if (initialized) MCPServiceStatus.CONNECTED else MCPServiceStatus.ERROR
                    )
                } catch (e: Exception) {
                    updateServiceStatus(service.id, MCPServiceStatus.ERROR)
                }
            }
        }
    }
    
    /**
     * 注册MCP服务
     */
    override suspend fun registerService(service: MCPService): Boolean {
        return mutex.withLock {
            try {
                val client = MCPClientImpl(httpClient, json)
                val config = MCPServiceConfig(
                    service = service,
                    client = client,
                    status = MCPServiceStatus.DISCONNECTED
                )
                
                serviceConfigs[service.id] = config
                
                // 尝试初始化连接
                updateServiceStatus(service.id, MCPServiceStatus.CONNECTING)
                val initialized = client.initialize(service)
                updateServiceStatus(
                    service.id,
                    if (initialized) MCPServiceStatus.CONNECTED else MCPServiceStatus.ERROR
                )
                
                initialized
            } catch (e: Exception) {
                updateServiceStatus(service.id, MCPServiceStatus.ERROR)
                false
            }
        }
    }
    
    /**
     * 注销MCP服务
     */
    override suspend fun unregisterService(serviceId: String): Boolean {
        return mutex.withLock {
            val config = serviceConfigs[serviceId]
            if (config != null) {
                config.client?.disconnect()
                serviceConfigs.remove(serviceId)
                updateServiceStatus(serviceId, MCPServiceStatus.DISCONNECTED)
                true
            } else {
                false
            }
        }
    }
    
    /**
     * 获取可用的MCP服务
     */
    override suspend fun getAvailableServices(agentId: String): List<MCPService> {
        val agentConfigs = agentRepository.getEnabledAgentMCPConfigs(agentId)
        val enabledServiceIds = agentConfigs.map { it.mcpServiceId }.toSet()
        
        return serviceConfigs.values
            .filter { config ->
                config.service.isEnabled && 
                enabledServiceIds.contains(config.service.id) &&
                config.status == MCPServiceStatus.CONNECTED
            }
            .map { it.service }
    }
    
    /**
     * 调用MCP服务
     */
    override suspend fun callService(
        agentId: String,
        serviceId: String,
        methodName: String,
        params: Map<String, Any>,
        conversationId: String?,
        messageId: String?
    ): MCPCallResult {
        val config = serviceConfigs[serviceId]
        if (config == null) {
            return MCPCallResult(
                success = false,
                error = "服务未找到: $serviceId"
            )
        }
        
        if (config.status != MCPServiceStatus.CONNECTED) {
            return MCPCallResult(
                success = false,
                error = "服务未连接: ${config.service.displayName}"
            )
        }
        
        val client = config.client
        if (client == null) {
            return MCPCallResult(
                success = false,
                error = "客户端未初始化"
            )
        }
        
        // 执行调用
        val result = client.call(methodName, params)
        
        // 记录调用日志
        val callLog = MCPCallLog(
            id = Random.nextLong().toString(),
            agentId = agentId,
            mcpServiceId = serviceId,
            conversationId = conversationId,
            messageId = messageId,
            methodName = methodName,
            requestParams = params.mapValues { it.value.toString() },
            responseData = result.data?.toString(),
            status = if (result.success) CallStatus.SUCCESS else CallStatus.ERROR,
            errorMessage = result.error,
            executionTime = result.executionTime,
            createdAt = Clock.System.now()
        )
        
        // 异步记录日志
        try {
            agentRepository.logMCPCall(callLog)
        } catch (e: Exception) {
            // 日志记录失败不影响主流程
        }
        
        return result
    }
    
    /**
     * 获取服务状态
     */
    override suspend fun getServiceStatus(serviceId: String): MCPServiceStatus {
        return serviceConfigs[serviceId]?.status ?: MCPServiceStatus.DISCONNECTED
    }
    
    /**
     * 监听服务状态变化
     */
    override fun observeServiceStatus(): Flow<Map<String, MCPServiceStatus>> {
        return _serviceStatusFlow.asStateFlow()
    }
    
    /**
     * 获取调用历史
     */
    override suspend fun getCallHistory(agentId: String, limit: Int): List<MCPCallLog> {
        return agentRepository.getMCPCallLogs(agentId, limit)
    }
    
    /**
     * 清理资源
     */
    override suspend fun cleanup() {
        mutex.withLock {
            for (config in serviceConfigs.values) {
                config.client?.disconnect()
            }
            serviceConfigs.clear()
            _serviceStatusFlow.value = emptyMap()
        }
    }
    
    /**
     * 更新服务状态
     */
    private fun updateServiceStatus(serviceId: String, status: MCPServiceStatus) {
        serviceConfigs[serviceId]?.let { config ->
            serviceConfigs[serviceId] = config.copy(status = status)
        }
        
        val currentStatuses = _serviceStatusFlow.value.toMutableMap()
        currentStatuses[serviceId] = status
        _serviceStatusFlow.value = currentStatuses
    }
}