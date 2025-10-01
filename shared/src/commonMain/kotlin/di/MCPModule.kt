package di

import domain.mcp.MCPClient
import domain.mcp.MCPServiceManager
import domain.mcp.MCPIntegrationService
import data.mcp.MCPClientImpl
import data.mcp.MCPServiceManagerImpl
import data.mcp.MCPIntegrationServiceImpl
import domain.repository.AgentRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logger
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * MCP模块
 * 提供MCP相关组件的依赖注入配置
 */
object MCPModule {
    
    /**
     * 提供HttpClient实例
     * 配置了内容协商、超时和日志记录
     */
    fun provideHttpClient(): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                    explicitNulls = false
                })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 30000
                requestTimeoutMillis = 30000
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
        }
    }
    
    /**
     * 提供Json序列化器实例
     */
    fun provideJson(): Json {
        return Json {
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
    
    /**
     * 提供MCPClient实例
     */
    fun provideMCPClient(): MCPClient {
        return MCPClientImpl(
            httpClient = provideHttpClient(),
            json = provideJson()
        )
    }
    
    /**
     * 提供MCPServiceManager实例
     */
    fun provideMCPServiceManager(agentRepository: AgentRepository): MCPServiceManager {
        return MCPServiceManagerImpl(
            agentRepository = agentRepository,
            httpClient = provideHttpClient()
        )
    }
    
    /**
     * 提供MCPIntegrationService实例
     */
    fun provideMCPIntegrationService(agentRepository: AgentRepository): MCPIntegrationService {
        return MCPIntegrationServiceImpl(
            mcpServiceManager = provideMCPServiceManager(agentRepository),
            agentRepository = agentRepository
        )
    }
}