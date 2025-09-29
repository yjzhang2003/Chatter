package domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 聊天消息数据模型
 * 表示对话中的单条消息，支持文本和图片内容
 */
@Serializable
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val content: String,
    val sender: MessageSender,
    val createdAt: Instant = Clock.System.now(),
    val images: List<String> = emptyList(), // 存储图片的Base64编码或路径
    val isLoading: Boolean = false,
    val aiModel: AIModel? = null, // 用于记录生成此消息的AI模型
    val metadata: MessageMetadata? = null
) {
    /**
     * 检查消息是否包含图片
     */
    fun hasImages(): Boolean = images.isNotEmpty()

    /**
     * 获取消息的显示内容
     * 如果内容过长，则截断显示
     */
    fun getDisplayContent(maxLength: Int = 100): String {
        return if (content.length <= maxLength) {
            content
        } else {
            "${content.take(maxLength)}..."
        }
    }

    /**
     * 检查消息是否为用户消息
     */
    fun isUserMessage(): Boolean = sender == MessageSender.USER

    /**
     * 检查消息是否为AI消息
     */
    fun isAiMessage(): Boolean = sender == MessageSender.AI

    /**
     * 获取消息的字符数
     */
    fun getCharacterCount(): Int = content.length

    /**
     * 创建消息的副本，更新加载状态
     */
    fun withLoadingState(loading: Boolean): ChatMessage {
        return copy(isLoading = loading)
    }

    /**
     * 创建消息的副本，更新内容
     */
    fun withContent(newContent: String): ChatMessage {
        return copy(content = newContent, isLoading = false)
    }

    companion object {
        /**
         * 创建消息（通用方法）
         * @param conversationId 对话ID
         * @param content 消息内容
         * @param sender 发送者
         * @param images 图片列表
         * @param aiModel AI模型
         * @param isLoading 是否加载中
         * @return 创建的消息对象
         */
        fun create(
            conversationId: String,
            content: String,
            sender: MessageSender,
            images: List<String> = emptyList(),
            aiModel: AIModel? = null,
            isLoading: Boolean = false
        ): ChatMessage {
            return ChatMessage(
                id = generateId(),
                conversationId = conversationId,
                content = content,
                sender = sender,
                images = images,
                aiModel = aiModel,
                isLoading = isLoading
            )
        }

        /**
         * 创建用户消息
         */
        fun createUserMessage(
            conversationId: String,
            content: String,
            images: List<String> = emptyList()
        ): ChatMessage {
            return ChatMessage(
                id = generateId(),
                conversationId = conversationId,
                content = content,
                sender = MessageSender.USER,
                images = images
            )
        }

        /**
         * 创建AI消息
         */
        fun createAiMessage(
            conversationId: String,
            content: String = "",
            aiModel: AIModel? = null,
            isLoading: Boolean = true
        ): ChatMessage {
            return ChatMessage(
                id = generateId(),
                conversationId = conversationId,
                content = content,
                sender = MessageSender.AI,
                aiModel = aiModel,
                isLoading = isLoading
            )
        }

        /**
         * 生成唯一的消息ID
         */
        private fun generateId(): String {
            return "msg_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
        }
    }
}

/**
 * 消息发送者枚举
 * 定义消息的发送者类型
 */
@Serializable
enum class MessageSender {
    USER,   // 用户
    AI,     // AI助手
    SYSTEM  // 系统消息（如智能体的系统提示词）
}

/**
 * 消息元数据
 * 用于存储消息的额外信息
 */
@Serializable
data class MessageMetadata(
    val tokenCount: Int? = null,           // Token数量
    val processingTime: Long? = null,      // 处理时间（毫秒）
    val temperature: Float? = null,        // 温度参数
    val maxTokens: Int? = null,           // 最大Token数
    val errorMessage: String? = null       // 错误信息
) {
    /**
     * 检查是否有错误
     */
    fun hasError(): Boolean = !errorMessage.isNullOrBlank()

    /**
     * 获取处理时间的可读格式
     */
    fun getFormattedProcessingTime(): String? {
        return processingTime?.let { time ->
            when {
                time < 1000 -> "${time}ms"
                time < 60000 -> "${time / 1000.0}s"
                else -> "${time / 60000}m ${(time % 60000) / 1000}s"
            }
        }
    }
}