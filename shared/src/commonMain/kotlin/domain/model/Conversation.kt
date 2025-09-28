package domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 对话数据模型
 * 表示一个完整的对话会话
 */
@Serializable
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val messageCount: Int = 0,
    val lastMessage: String = "",
    val aiModel: AIModel = AIModel.getDefault(),
    val agentId: String? = null
) {
    /**
     * 生成对话的显示标题
     * 如果标题为空或过长，则使用默认格式
     */
    fun getDisplayTitle(): String {
        return when {
            title.isBlank() -> "新对话"
            title.length > 30 -> "${title.take(27)}..."
            else -> title
        }
    }

    /**
     * 检查对话是否为空（没有消息）
     */
    fun isEmpty(): Boolean = messageCount == 0

    /**
     * 获取对话的简短描述
     */
    fun getDescription(): String {
        return when {
            lastMessage.isBlank() -> "暂无消息"
            lastMessage.length > 50 -> "${lastMessage.take(47)}..."
            else -> lastMessage
        }
    }

    /**
     * 检查对话是否需要更新
     */
    fun needsUpdate(newMessageCount: Int, newLastMessage: String): Boolean {
        return messageCount != newMessageCount || lastMessage != newLastMessage
    }

    /**
     * 创建更新后的对话副本
     */
    fun updateStats(newMessageCount: Int, newLastMessage: String): Conversation {
        return copy(
            messageCount = newMessageCount,
            lastMessage = newLastMessage,
            updatedAt = Clock.System.now()
        )
    }

    companion object {
        /**
         * 创建新对话
         */
        fun create(
            id: String = generateId(),
            title: String = "",
            aiModel: AIModel = AIModel.getDefault(),
            agentId: String? = null
        ): Conversation {
            return Conversation(
                id = id,
                title = title,
                aiModel = aiModel,
                agentId = agentId
            )
        }

        /**
         * 生成唯一的对话ID
         */
        private fun generateId(): String {
            return "conv_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
        }
    }
}