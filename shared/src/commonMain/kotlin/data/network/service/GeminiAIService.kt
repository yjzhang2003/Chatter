package data.network.service

import data.network.GeminiService
import domain.model.Status
import domain.service.AIService
import io.ktor.utils.io.errors.IOException

/**
 * Gemini AI服务实现
 * 基于现有的GeminiService实现AIService接口
 */
class GeminiAIService : AIService {
    
    private val geminiService = GeminiService()
    
    /**
     * 生成内容
     */
    /**
     * 生成内容，支持上下文消息传递
     * @param prompt 用户输入的提示
     * @param images 图片列表
     * @param contextMessages 上下文消息列表
     * @return 生成结果状态
     */
    override suspend fun generateContent(
        prompt: String, 
        images: List<ByteArray>,
        contextMessages: List<domain.model.ChatMessage>
    ): Status {
        return try {
            // 添加调试日志
            println("Debug: GeminiAIService 接收到 ${contextMessages.size} 条上下文消息")
            
            val response = geminiService.generateContent(prompt, images, contextMessages)
            
            response.error?.let {
                Status.Error(it.message)
            } ?: response.getText()?.let {
                Status.Success(it)
            } ?: Status.Error("An error occurred, please retry.")
            
        } catch (e: IOException) {
            Status.Error("Unable to connect to the server. Please check your internet connection and try again.")
        } catch (e: Exception) {
            Status.Error("An error occurred, please retry.")
        }
    }
    
    /**
     * 获取API密钥
     */
    override fun getApiKey(): String {
        return geminiService.getApiKey()
    }
    
    /**
     * 设置API密钥
     */
    override fun setApiKey(key: String) {
        geminiService.setApiKey(key)
    }
    
    /**
     * 验证API密钥
     */
    override suspend fun validateApiKey(): Boolean {
        return try {
            val testResponse = geminiService.generateContent("test", emptyList())
            testResponse.error == null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 支持多模态输入
     */
    override fun supportsMultimodal(): Boolean = true
}