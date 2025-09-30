package domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * MCP服务数据模型
 * 用于存储MCP服务的配置信息
 */
@Serializable
data class MCPService(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String = "",
    val serviceType: MCPServiceType,
    val endpointUrl: String,
    val apiVersion: String = "1.0",
    val authType: AuthType = AuthType.NONE,
    val authConfig: Map<String, String> = emptyMap(),
    val capabilities: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val isSystem: Boolean = false, // 是否为系统预设服务
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * MCP服务类型枚举
 */
@Serializable
enum class MCPServiceType(val value: String, val displayName: String) {
    TRANSPORT("transport", "交通出行"),
    RIDE_HAILING("ride_hailing", "网约车"),
    WEATHER("weather", "天气服务"),
    CALENDAR("calendar", "日历服务"),
    GITHUB("github", "GitHub"),
    EMAIL("email", "邮件服务"),
    FILE_SYSTEM("file_system", "文件系统"),
    WEB_SEARCH("web_search", "网络搜索"),
    DATABASE("database", "数据库"),
    API("api", "API服务"),
    OTHER("other", "其他");
    
    companion object {
        /**
         * 从字符串值创建服务类型
         */
        fun fromString(value: String): MCPServiceType {
            return entries.find { it.value == value } ?: OTHER
        }
    }
}

/**
 * 认证类型枚举
 */
@Serializable
enum class AuthType(val value: String) {
    NONE("none"),
    API_KEY("api_key"),
    OAUTH("oauth"),
    BEARER("bearer");
    
    companion object {
        /**
         * 从字符串值创建认证类型
         */
        fun fromString(value: String): AuthType {
            return entries.find { it.value == value } ?: NONE
        }
    }
}

/**
 * 智能体MCP配置数据模型
 * 用于存储智能体与MCP服务的关联配置
 */
@Serializable
data class AgentMCPConfig(
    val id: String,
    val agentId: String,
    val mcpServiceId: String,
    val isEnabled: Boolean = true,
    val configOverride: Map<String, String> = emptyMap(),
    val usageCount: Int = 0,
    val lastUsedAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * MCP调用日志数据模型
 * 用于记录MCP服务的调用历史
 */
@Serializable
data class MCPCallLog(
    val id: String,
    val agentId: String,
    val mcpServiceId: String,
    val conversationId: String? = null,
    val messageId: String? = null,
    val methodName: String,
    val requestParams: Map<String, String> = emptyMap(),
    val responseData: String? = null,
    val status: CallStatus,
    val errorMessage: String? = null,
    val executionTime: Long? = null, // 执行时间（毫秒）
    val createdAt: Instant
)

/**
 * 调用状态枚举
 */
@Serializable
enum class CallStatus(val value: String) {
    SUCCESS("success"),
    ERROR("error"),
    TIMEOUT("timeout");
    
    companion object {
        /**
         * 从字符串值创建调用状态
         */
        fun fromString(value: String): CallStatus {
            return entries.find { it.value == value } ?: ERROR
        }
    }
}

/**
 * MCP服务统计数据模型
 */
@Serializable
data class MCPServiceStats(
    val mcpServiceId: String,
    val totalCalls: Int,
    val successCalls: Int,
    val avgExecutionTime: Double
) {
    val successRate: Double
        get() = if (totalCalls > 0) successCalls.toDouble() / totalCalls else 0.0
}