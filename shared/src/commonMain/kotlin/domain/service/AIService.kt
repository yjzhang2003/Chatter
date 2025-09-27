package domain.service

import domain.model.Status

/**
 * AI服务通用接口
 * 为不同的AI模型提供统一的抽象层
 */
interface AIService {
    
    /**
     * 生成内容
     * @param prompt 文本提示
     * @param images 图片数据列表（可选）
     * @return 生成结果状态
     */
    suspend fun generateContent(prompt: String, images: List<ByteArray> = emptyList()): Status
    
    /**
     * 获取API密钥
     * @return 当前设置的API密钥
     */
    fun getApiKey(): String
    
    /**
     * 设置API密钥
     * @param key API密钥
     */
    fun setApiKey(key: String)
    
    /**
     * 验证API密钥是否有效
     * @return 是否有效
     */
    suspend fun validateApiKey(): Boolean
    
    /**
     * 检查服务是否支持多模态输入（文本+图片）
     * @return 是否支持多模态
     */
    fun supportsMultimodal(): Boolean
}