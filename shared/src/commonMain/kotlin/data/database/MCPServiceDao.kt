package data.database

import app.cash.sqldelight.db.SqlDriver
import domain.model.MCPService
import domain.model.AgentMCPConfig
import domain.model.MCPCallLog
import domain.model.MCPServiceType
import domain.model.AuthType
import domain.model.CallStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * MCP服务数据访问对象
 * 提供MCP服务相关的数据库操作功能
 */
class MCPServiceDao(private val driver: SqlDriver) {
    
    private val database = ChatDatabase(driver)
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 获取所有MCP服务
     * @return MCP服务列表
     */
    suspend fun getAllMCPServices(): List<MCPService> {
        return try {
            database.chatDatabaseQueries.getAllMCPServices().executeAsList().map { row ->
                MCPService(
                    id = row.id,
                    name = row.name,
                    displayName = row.display_name,
                    description = row.description,
                    serviceType = MCPServiceType.fromString(row.service_type),
                    endpointUrl = row.endpoint_url,
                    apiVersion = row.api_version,
                    authType = AuthType.fromString(row.auth_type),
                    authConfig = if (row.auth_config.isNotEmpty()) json.decodeFromString(row.auth_config) else emptyMap(),
                    capabilities = if (row.capabilities.isNotEmpty()) json.decodeFromString(row.capabilities) else emptyList(),
                    isEnabled = row.is_enabled == 1L,
                    isSystem = row.is_system == 1L,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 根据ID获取MCP服务
     * @param id 服务ID
     * @return MCP服务对象，如果不存在则返回null
     */
    suspend fun getMCPServiceById(id: String): MCPService? {
        return try {
            val row = database.chatDatabaseQueries.getMCPServiceById(id).executeAsOneOrNull()
            row?.let {
                MCPService(
                    id = it.id,
                    name = it.name,
                    displayName = it.display_name,
                    description = it.description,
                    serviceType = MCPServiceType.fromString(it.service_type),
                    endpointUrl = it.endpoint_url,
                    apiVersion = it.api_version,
                    authType = AuthType.fromString(it.auth_type),
                    authConfig = if (it.auth_config.isNotEmpty()) json.decodeFromString(it.auth_config) else emptyMap(),
                    capabilities = if (it.capabilities.isNotEmpty()) json.decodeFromString(it.capabilities) else emptyList(),
                    isEnabled = it.is_enabled == 1L,
                    isSystem = it.is_system == 1L,
                    createdAt = Instant.fromEpochMilliseconds(it.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(it.updated_at)
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 根据服务类型获取MCP服务
     * @param serviceType 服务类型
     * @return MCP服务列表
     */
    suspend fun getMCPServicesByType(serviceType: MCPServiceType): List<MCPService> {
        return try {
            database.chatDatabaseQueries.getMCPServicesByType(serviceType.value).executeAsList().map { row ->
                MCPService(
                    id = row.id,
                    name = row.name,
                    displayName = row.display_name,
                    description = row.description,
                    serviceType = MCPServiceType.fromString(row.service_type),
                    endpointUrl = row.endpoint_url,
                    apiVersion = row.api_version,
                    authType = AuthType.fromString(row.auth_type),
                    authConfig = if (row.auth_config.isNotEmpty()) json.decodeFromString(row.auth_config) else emptyMap(),
                    capabilities = if (row.capabilities.isNotEmpty()) json.decodeFromString(row.capabilities) else emptyList(),
                    isEnabled = row.is_enabled == 1L,
                    isSystem = row.is_system == 1L,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取启用的MCP服务
     * @return 启用的MCP服务列表
     */
    suspend fun getEnabledMCPServices(): List<MCPService> {
        return try {
            database.chatDatabaseQueries.getEnabledMCPServices().executeAsList().map { row ->
                MCPService(
                    id = row.id,
                    name = row.name,
                    displayName = row.display_name,
                    description = row.description,
                    serviceType = MCPServiceType.fromString(row.service_type),
                    endpointUrl = row.endpoint_url,
                    apiVersion = row.api_version,
                    authType = AuthType.fromString(row.auth_type),
                    authConfig = if (row.auth_config.isNotEmpty()) json.decodeFromString(row.auth_config) else emptyMap(),
                    capabilities = if (row.capabilities.isNotEmpty()) json.decodeFromString(row.capabilities) else emptyList(),
                    isEnabled = row.is_enabled == 1L,
                    isSystem = row.is_system == 1L,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 插入MCP服务
     * @param service MCP服务对象
     * @return 是否插入成功
     */
    suspend fun insertMCPService(service: MCPService): Boolean {
        return try {
            database.chatDatabaseQueries.insertMCPService(
                id = service.id,
                name = service.name,
                display_name = service.displayName,
                description = service.description,
                service_type = service.serviceType.value,
                endpoint_url = service.endpointUrl,
                api_version = service.apiVersion,
                auth_type = service.authType.value,
                auth_config = json.encodeToString(service.authConfig),
                capabilities = json.encodeToString(service.capabilities),
                is_enabled = if (service.isEnabled) 1L else 0L,
                is_system = if (service.isSystem) 1L else 0L,
                created_at = service.createdAt.toEpochMilliseconds(),
                updated_at = service.updatedAt.toEpochMilliseconds()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新MCP服务
     * @param service MCP服务对象
     * @return 是否更新成功
     */
    suspend fun updateMCPService(service: MCPService): Boolean {
        return try {
            database.chatDatabaseQueries.updateMCPService(
                name = service.name,
                display_name = service.displayName,
                description = service.description,
                service_type = service.serviceType.value,
                endpoint_url = service.endpointUrl,
                api_version = service.apiVersion,
                auth_type = service.authType.value,
                auth_config = json.encodeToString(service.authConfig),
                capabilities = json.encodeToString(service.capabilities),
                is_enabled = if (service.isEnabled) 1L else 0L,
                updated_at = service.updatedAt.toEpochMilliseconds(),
                id = service.id
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除MCP服务
     * @param id 服务ID
     * @return 是否删除成功
     */
    suspend fun deleteMCPService(id: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteMCPService(id)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取智能体的MCP配置
     * @param agentId 智能体ID
     * @return MCP配置列表
     */
    suspend fun getAgentMCPConfigs(agentId: String): List<AgentMCPConfig> {
        return try {
            database.chatDatabaseQueries.getAgentMCPConfigs(agentId).executeAsList().map { row ->
                AgentMCPConfig(
                    id = row.id,
                    agentId = row.agent_id,
                    mcpServiceId = row.mcp_service_id,
                    isEnabled = row.is_enabled == 1L,
                    configOverride = if (row.config_override.isNotEmpty()) json.decodeFromString(row.config_override) else emptyMap(),
                    usageCount = row.usage_count.toInt(),
                    lastUsedAt = row.last_used_at?.let { Instant.fromEpochMilliseconds(it) },
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取智能体启用的MCP配置
     * @param agentId 智能体ID
     * @return 启用的MCP配置列表
     */
    suspend fun getEnabledAgentMCPConfigs(agentId: String): List<AgentMCPConfig> {
        return try {
            database.chatDatabaseQueries.getEnabledAgentMCPConfigs(agentId).executeAsList().map { row ->
                AgentMCPConfig(
                    id = row.id,
                    agentId = row.agent_id,
                    mcpServiceId = row.mcp_service_id,
                    isEnabled = row.is_enabled == 1L,
                    configOverride = if (row.config_override.isNotEmpty()) json.decodeFromString(row.config_override) else emptyMap(),
                    usageCount = row.usage_count.toInt(),
                    lastUsedAt = row.last_used_at?.let { Instant.fromEpochMilliseconds(it) },
                    createdAt = Instant.fromEpochMilliseconds(row.created_at),
                    updatedAt = Instant.fromEpochMilliseconds(row.updated_at)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 插入智能体MCP配置
     * @param config MCP配置对象
     * @return 是否插入成功
     */
    suspend fun insertAgentMCPConfig(config: AgentMCPConfig): Boolean {
        return try {
            database.chatDatabaseQueries.insertAgentMCPConfig(
                id = config.id,
                agent_id = config.agentId,
                mcp_service_id = config.mcpServiceId,
                is_enabled = if (config.isEnabled) 1L else 0L,
                config_override = json.encodeToString(config.configOverride),
                usage_count = config.usageCount.toLong(),
                last_used_at = config.lastUsedAt?.toEpochMilliseconds(),
                created_at = config.createdAt.toEpochMilliseconds(),
                updated_at = config.updatedAt.toEpochMilliseconds()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 更新智能体MCP配置
     * @param config MCP配置对象
     * @return 是否更新成功
     */
    suspend fun updateAgentMCPConfig(config: AgentMCPConfig): Boolean {
        return try {
            database.chatDatabaseQueries.updateAgentMCPConfig(
                is_enabled = if (config.isEnabled) 1L else 0L,
                config_override = json.encodeToString(config.configOverride),
                usage_count = config.usageCount.toLong(),
                last_used_at = config.lastUsedAt?.toEpochMilliseconds(),
                updated_at = config.updatedAt.toEpochMilliseconds(),
                id = config.id
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除智能体MCP配置
     * @param id 配置ID
     * @return 是否删除成功
     */
    suspend fun deleteAgentMCPConfig(id: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteAgentMCPConfig(id)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除智能体的所有MCP配置
     * @param agentId 智能体ID
     * @return 是否删除成功
     */
    suspend fun deleteAgentMCPConfigsByAgentId(agentId: String): Boolean {
        return try {
            database.chatDatabaseQueries.deleteAgentMCPConfigsByAgentId(agentId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取MCP调用日志
     * @param agentId 智能体ID
     * @param limit 限制数量
     * @return MCP调用日志列表
     */
    suspend fun getMCPCallLogs(agentId: String, limit: Int): List<MCPCallLog> {
        return try {
            database.chatDatabaseQueries.getMCPCallLogs(agentId, limit.toLong()).executeAsList().map { row ->
                MCPCallLog(
                    id = row.id,
                    agentId = row.agent_id,
                    mcpServiceId = row.mcp_service_id,
                    conversationId = row.conversation_id,
                    messageId = row.message_id,
                    methodName = row.method_name,
                    requestParams = if (row.request_params.isNotEmpty()) json.decodeFromString(row.request_params) else emptyMap(),
                    responseData = row.response_data,
                    status = CallStatus.fromString(row.status),
                    errorMessage = row.error_message,
                    executionTime = row.execution_time,
                    createdAt = Instant.fromEpochMilliseconds(row.created_at)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 插入MCP调用日志
     * @param log MCP调用日志对象
     * @return 是否插入成功
     */
    suspend fun insertMCPCallLog(log: MCPCallLog): Boolean {
        return try {
            database.chatDatabaseQueries.insertMCPCallLog(
                id = log.id,
                agent_id = log.agentId,
                mcp_service_id = log.mcpServiceId,
                conversation_id = log.conversationId,
                message_id = log.messageId,
                method_name = log.methodName,
                request_params = json.encodeToString(log.requestParams),
                response_data = log.responseData,
                status = log.status.value,
                error_message = log.errorMessage,
                execution_time = log.executionTime,
                created_at = log.createdAt.toEpochMilliseconds()
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除过期的MCP调用日志
     * @param beforeTime 删除此时间之前的日志
     * @return 是否删除成功
     */
    suspend fun deleteOldMCPCallLogs(beforeTime: Instant): Boolean {
        return try {
            database.chatDatabaseQueries.deleteMCPCallLogs(beforeTime.toEpochMilliseconds())
            true
        } catch (e: Exception) {
            false
        }
    }
}