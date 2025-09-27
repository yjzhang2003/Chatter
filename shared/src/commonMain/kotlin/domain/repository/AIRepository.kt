package domain.repository

import domain.model.Status
import domain.model.AIModel

/**
 * 通用AI Repository接口
 * 支持多种AI模型的统一访问
 */
interface AIRepository {
    
    /**
     * 生成内容
     * @param prompt 文本提示
     * @param images 图片列表（可选）
     * @return 生成结果状态
     */
    suspend fun generate(prompt: String, images: List<ByteArray> = emptyList()): Status
    
    /**
     * 获取当前选择的AI模型
     */
    suspend fun getCurrentModel(): AIModel
    
    /**
     * 设置当前使用的AI模型
     */
    suspend fun setCurrentModel(model: AIModel)
    
    /**
     * 获取指定模型的API密钥
     */
    suspend fun getApiKey(model: AIModel): String?
    
    /**
     * 设置指定模型的API密钥
     */
    suspend fun setApiKey(model: AIModel, key: String)
    
    /**
     * 验证指定模型的API密钥是否有效
     */
    suspend fun validateApiKey(model: AIModel, key: String): Boolean
    
    /**
     * 检查指定模型是否支持多模态输入
     */
    fun supportsMultimodal(model: AIModel): Boolean
}