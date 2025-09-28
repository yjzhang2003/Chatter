package domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 智能体数据模型
 * 表示一个AI助手角色，包含其特定的提示词和配置
 */
@Serializable
data class Agent(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val avatar: String = "",
    val isPreset: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val usageCount: Int = 0
) {
    /**
     * 获取智能体的显示名称
     * 如果名称为空或过长，则使用默认格式
     */
    fun getDisplayName(): String {
        return when {
            name.isBlank() -> "未命名智能体"
            name.length > 20 -> "${name.take(17)}..."
            else -> name
        }
    }

    /**
     * 获取智能体的简短描述
     */
    fun getShortDescription(): String {
        return when {
            description.isBlank() -> "暂无描述"
            description.length > 50 -> "${description.take(47)}..."
            else -> description
        }
    }

    /**
     * 检查智能体是否为自定义智能体
     */
    fun isCustom(): Boolean = !isPreset

    /**
     * 更新使用次数
     */
    fun incrementUsage(): Agent {
        return copy(
            usageCount = usageCount + 1,
            updatedAt = Clock.System.now()
        )
    }

    /**
     * 更新智能体信息
     */
    fun update(
        name: String? = null,
        description: String? = null,
        systemPrompt: String? = null,
        avatar: String? = null
    ): Agent {
        return copy(
            name = name ?: this.name,
            description = description ?: this.description,
            systemPrompt = systemPrompt ?: this.systemPrompt,
            avatar = avatar ?: this.avatar,
            updatedAt = Clock.System.now()
        )
    }

    companion object {
        /**
         * 创建新的智能体
         */
        fun create(
            id: String = generateId(),
            name: String,
            description: String = "",
            systemPrompt: String,
            avatar: String = "",
            isPreset: Boolean = false
        ): Agent {
            return Agent(
                id = id,
                name = name,
                description = description,
                systemPrompt = systemPrompt,
                avatar = avatar,
                isPreset = isPreset
            )
        }

        /**
         * 创建预设智能体
         */
        fun createPreset(
            id: String,
            name: String,
            description: String,
            systemPrompt: String,
            avatar: String = ""
        ): Agent {
            return create(
                id = id,
                name = name,
                description = description,
                systemPrompt = systemPrompt,
                avatar = avatar,
                isPreset = true
            )
        }

        /**
         * 生成唯一ID
         */
        private fun generateId(): String {
            return "agent_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
        }

        /**
         * 默认智能体（通用助手）
         */
        fun getDefault(): Agent {
            return createPreset(
                id = "default_assistant",
                name = "通用助手",
                description = "友好、有帮助的AI助手，可以协助处理各种任务",
                systemPrompt = "你是一个友好、有帮助的AI助手。请以礼貌、专业的方式回答用户的问题，并尽力提供准确、有用的信息。",
                avatar = "🤖"
            )
        }
    }
}