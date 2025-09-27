package data.local

import domain.model.AIModel
import domain.model.CustomAIModel

/**
 * 本地偏好设置管理器接口
 * 用于保存和读取API密钥、当前选择的模型等配置信息
 */
interface PreferencesManager {
    
    /**
     * 保存指定模型的API密钥
     */
    suspend fun saveApiKey(model: AIModel, apiKey: String)
    
    /**
     * 获取指定模型的API密钥
     */
    suspend fun getApiKey(model: AIModel): String?
    
    /**
     * 保存当前选择的模型
     */
    suspend fun saveCurrentModel(model: AIModel)
    
    /**
     * 获取当前选择的模型
     */
    suspend fun getCurrentModel(): AIModel
    
    /**
     * 清除指定模型的API密钥
     */
    suspend fun clearApiKey(model: AIModel)
    
    /**
     * 清除所有API密钥
     */
    suspend fun clearAllApiKeys()
    
    /**
     * 保存自定义模型配置（兼容旧版本）
     */
    suspend fun saveCustomModel(customModel: CustomAIModel)
    
    /**
     * 获取自定义模型配置（兼容旧版本）
     */
    suspend fun getCustomModel(): CustomAIModel?
    
    /**
     * 清除自定义模型配置（兼容旧版本）
     */
    suspend fun clearCustomModel()
    
    /**
     * 保存自定义模型管理器
     */
    suspend fun saveCustomModelManager(manager: domain.model.CustomModelManager)
    
    /**
     * 获取自定义模型管理器
     */
    suspend fun getCustomModelManager(): domain.model.CustomModelManager?
    
    /**
     * 清除所有自定义模型
     */
    suspend fun clearAllCustomModels()
}

/**
 * 本地偏好设置管理器的默认实现
 * 使用平台特定的存储机制
 */
expect class PreferencesManagerImpl : PreferencesManager