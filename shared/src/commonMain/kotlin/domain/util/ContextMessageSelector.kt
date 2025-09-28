package domain.util

import domain.model.ChatMessage
import domain.model.AIModel

/**
 * 上下文消息选择器
 * 根据token限制和AI模型特性优化上下文消息选择策略
 */
object ContextMessageSelector {
    
    // 不同AI模型的默认token限制
    private val MODEL_TOKEN_LIMITS = mapOf(
        AIModel.GEMINI_PRO to 30720,      // Gemini Pro 1.0 上下文窗口
        AIModel.KIMI to 200000,           // Kimi 长上下文窗口
        AIModel.DOUBAO to 32768,          // 豆包模型上下文窗口
        AIModel.CUSTOM to 4096            // 自定义模型默认值（通常是GPT-3.5级别）
    )
    
    // 为响应预留的token数量
    private const val RESPONSE_RESERVE_TOKENS = 1000
    
    // 平均每个字符对应的token数量（中文约1.5，英文约0.25，取中间值）
    private const val AVERAGE_TOKENS_PER_CHAR = 0.8
    
    /**
     * 根据token限制选择上下文消息
     * @param messages 所有可用的消息列表
     * @param currentPrompt 当前用户输入的提示
     * @param aiModel 使用的AI模型
     * @param maxContextRatio 上下文占总token的最大比例
     * @return 选择的上下文消息列表
     */
    fun selectContextMessages(
        messages: List<ChatMessage>,
        currentPrompt: String,
        aiModel: AIModel = AIModel.GEMINI_PRO,
        maxContextRatio: Double = 0.7
    ): List<ChatMessage> {
        if (messages.isEmpty()) return emptyList()
        
        // 获取模型的token限制
        val modelTokenLimit = MODEL_TOKEN_LIMITS[aiModel] ?: MODEL_TOKEN_LIMITS[AIModel.CUSTOM]!!
        
        // 计算可用于上下文的token数量
        val availableTokens = ((modelTokenLimit - RESPONSE_RESERVE_TOKENS) * maxContextRatio).toInt()
        
        // 计算当前提示的token数量
        val currentPromptTokens = estimateTokenCount(currentPrompt)
        val remainingTokens = availableTokens - currentPromptTokens
        
        // 添加调试日志
        println("Debug: ContextMessageSelector - 模型: $aiModel, 总token限制: $modelTokenLimit")
        println("Debug: ContextMessageSelector - 可用token: $availableTokens, 当前提示token: $currentPromptTokens, 剩余token: $remainingTokens")
        println("Debug: ContextMessageSelector - 输入消息数量: ${messages.size}")
        
        if (remainingTokens <= 0) {
            // 如果当前提示已经占用了所有可用token，返回空上下文
            println("Debug: ContextMessageSelector - 剩余token不足，返回空上下文")
            return emptyList()
        }
        
        // 从最新的消息开始选择，确保上下文的连贯性
        val selectedMessages = mutableListOf<ChatMessage>()
        var usedTokens = 0
        
        // 倒序遍历消息（从最新到最旧）
        for (message in messages.reversed()) {
            val messageTokens = estimateTokenCount(message.content)
            
            // 检查添加这条消息是否会超出token限制
            if (usedTokens + messageTokens <= remainingTokens) {
                selectedMessages.add(0, message) // 添加到列表开头以保持时间顺序
                usedTokens += messageTokens
                println("Debug: ContextMessageSelector - 添加消息: ${message.sender} - ${message.content.take(50)}...")
            } else {
                // 如果添加会超出限制，尝试截断消息内容
                val availableForThisMessage = remainingTokens - usedTokens
                if (availableForThisMessage > 100) { // 至少保留100个token的消息才有意义
                    val truncatedContent = truncateMessageContent(message.content, availableForThisMessage)
                    if (truncatedContent.isNotEmpty()) {
                        val truncatedMessage = message.copy(content = truncatedContent)
                        selectedMessages.add(0, truncatedMessage)
                        println("Debug: ContextMessageSelector - 添加截断消息: ${message.sender} - ${truncatedContent.take(50)}...")
                    }
                }
                break // 无法添加更多消息
            }
        }
        
        println("Debug: ContextMessageSelector - 最终选择了 ${selectedMessages.size} 条消息，使用token: $usedTokens")
        return selectedMessages
    }
    
    /**
     * 估算文本的token数量
     * @param text 要估算的文本
     * @return 估算的token数量
     */
    private fun estimateTokenCount(text: String): Int {
        return (text.length * AVERAGE_TOKENS_PER_CHAR).toInt()
    }
    
    /**
     * 截断消息内容以适应token限制
     * @param content 原始消息内容
     * @param maxTokens 最大允许的token数量
     * @return 截断后的消息内容
     */
    private fun truncateMessageContent(content: String, maxTokens: Int): String {
        val maxChars = (maxTokens / AVERAGE_TOKENS_PER_CHAR).toInt()
        
        return if (content.length <= maxChars) {
            content
        } else {
            // 尝试在句子边界截断
            val truncated = content.take(maxChars)
            val lastSentenceEnd = truncated.lastIndexOfAny(charArrayOf('。', '！', '？', '.', '!', '?'))
            
            if (lastSentenceEnd > maxChars * 0.5) {
                // 如果找到了合适的句子边界，在那里截断
                truncated.take(lastSentenceEnd + 1)
            } else {
                // 否则直接截断并添加省略号
                truncated.take(maxChars - 3) + "..."
            }
        }
    }
    
    /**
     * 获取推荐的上下文消息数量
     * @param aiModel 使用的AI模型
     * @return 推荐的消息数量
     */
    fun getRecommendedMessageCount(aiModel: AIModel): Int {
        return when (aiModel) {
            AIModel.KIMI -> 50        // Kimi支持长上下文，可以包含更多消息
            AIModel.GEMINI_PRO -> 30  // Gemini Pro有较大的上下文窗口
            AIModel.DOUBAO -> 20      // 豆包中等上下文窗口
            AIModel.CUSTOM -> 15      // 自定义模型保守估计
        }
    }
    
    /**
     * 检查消息是否应该被包含在上下文中
     * @param message 要检查的消息
     * @return 是否应该包含
     */
    private fun shouldIncludeMessage(message: ChatMessage): Boolean {
        // 过滤掉空消息或加载中的消息
        return message.content.isNotBlank() && !message.isLoading
    }
}