package domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 智能体记忆数据模型
 * 用于存储智能体的长期记忆信息
 */
@Serializable
data class AgentMemory(
    val id: String,
    val agentId: String,
    val conversationId: String? = null,
    val messageId: String? = null,
    val content: String,
    val memoryType: MemoryType = MemoryType.CONVERSATION,
    val importanceScore: Double = 0.5, // 0.0-1.0 重要性评分
    val accessCount: Int = 0, // 访问次数
    val lastAccessedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val tags: List<String> = emptyList(),
    val embeddingVector: String? = null // 向量化表示，用于语义检索
)

/**
 * 记忆类型枚举
 */
@Serializable
enum class MemoryType(val value: String) {
    CONVERSATION("conversation"), // 对话记忆
    FACT("fact"), // 事实记忆
    PREFERENCE("preference"), // 偏好记忆
    SKILL("skill"); // 技能记忆
    
    companion object {
        /**
         * 从字符串值创建记忆类型
         */
        fun fromString(value: String): MemoryType {
            return entries.find { it.value == value } ?: CONVERSATION
        }
    }
}

/**
 * 记忆关联数据模型
 * 用于存储记忆之间的关联关系
 */
@Serializable
data class MemoryRelation(
    val id: String,
    val sourceMemoryId: String,
    val targetMemoryId: String,
    val relationType: RelationType,
    val strength: Double = 0.5, // 关联强度 0.0-1.0
    val createdAt: Instant
)

/**
 * 记忆关联类型枚举
 */
@Serializable
enum class RelationType(val value: String) {
    SIMILAR("similar"), // 相似
    RELATED("related"), // 相关
    CONFLICT("conflict"), // 冲突
    UPDATE("update"); // 更新
    
    companion object {
        /**
         * 从字符串值创建关联类型
         */
        fun fromString(value: String): RelationType {
            return entries.find { it.value == value } ?: RELATED
        }
    }
}