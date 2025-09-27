package data.local

import domain.model.AIModel
import domain.model.CustomAIModel
import platform.Foundation.NSUserDefaults
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * iOS平台的偏好设置管理器实现
 * 使用UserDefaults进行本地存储
 */
actual class PreferencesManagerImpl : PreferencesManager {
    
    companion object {
        private const val KEY_API_KEY_PREFIX = "api_key_"
        private const val KEY_CURRENT_MODEL = "current_model"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_CUSTOM_MODEL_MANAGER = "custom_model_manager"
    }
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 保存指定模型的API密钥
     */
    override suspend fun saveApiKey(model: AIModel, apiKey: String) {
        userDefaults.setObject(apiKey, KEY_API_KEY_PREFIX + model.name)
        userDefaults.synchronize()
    }
    
    /**
     * 获取指定模型的API密钥
     */
    override suspend fun getApiKey(model: AIModel): String? {
        return userDefaults.stringForKey(KEY_API_KEY_PREFIX + model.name)
    }
    
    /**
     * 保存当前选择的模型
     */
    override suspend fun saveCurrentModel(model: AIModel) {
        userDefaults.setObject(model.name, KEY_CURRENT_MODEL)
        userDefaults.synchronize()
    }
    
    /**
     * 获取当前选择的模型
     */
    override suspend fun getCurrentModel(): AIModel {
        val modelName = userDefaults.stringForKey(KEY_CURRENT_MODEL)
        return if (modelName != null) {
            AIModel.fromName(modelName) ?: AIModel.getDefault()
        } else {
            AIModel.getDefault()
        }
    }
    
    /**
     * 清除指定模型的API密钥
     */
    override suspend fun clearApiKey(model: AIModel) {
        userDefaults.removeObjectForKey(KEY_API_KEY_PREFIX + model.name)
        userDefaults.synchronize()
    }
    
    /**
     * 清除所有API密钥
     */
    override suspend fun clearAllApiKeys() {
        AIModel.values().forEach { model ->
            userDefaults.removeObjectForKey(KEY_API_KEY_PREFIX + model.name)
        }
        userDefaults.synchronize()
    }
    
    /**
     * 保存自定义模型配置
     */
    override suspend fun saveCustomModel(customModel: CustomAIModel) {
        try {
            val jsonString = json.encodeToString(customModel)
            userDefaults.setObject(jsonString, KEY_CUSTOM_MODEL)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // 序列化失败时忽略
        }
    }
    
    /**
     * 获取自定义模型配置
     */
    override suspend fun getCustomModel(): CustomAIModel? {
        return try {
            val jsonString = userDefaults.stringForKey(KEY_CUSTOM_MODEL)
            if (jsonString != null) {
                json.decodeFromString<CustomAIModel>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            // 反序列化失败时返回null
            null
        }
    }
    
    /**
     * 清除自定义模型配置
     */
    override suspend fun clearCustomModel() {
        userDefaults.removeObjectForKey(KEY_CUSTOM_MODEL)
        userDefaults.synchronize()
    }
    
    /**
     * 保存自定义模型管理器
     */
    override suspend fun saveCustomModelManager(manager: domain.model.CustomModelManager) {
        try {
            val jsonString = json.encodeToString(manager)
            userDefaults.setObject(jsonString, KEY_CUSTOM_MODEL_MANAGER)
            userDefaults.synchronize()
        } catch (e: Exception) {
            // 序列化失败时忽略
        }
    }
    
    /**
     * 获取自定义模型管理器
     */
    override suspend fun getCustomModelManager(): domain.model.CustomModelManager? {
        return try {
            val jsonString = userDefaults.stringForKey(KEY_CUSTOM_MODEL_MANAGER)
            if (jsonString != null) {
                json.decodeFromString<domain.model.CustomModelManager>(jsonString)
            } else {
                // 尝试从旧的单个模型迁移
                val oldModel = getCustomModel()
                if (oldModel != null) {
                    val manager = domain.model.CustomModelManager.fromSingleModel(oldModel)
                    saveCustomModelManager(manager)
                    clearCustomModel() // 清除旧数据
                    manager
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            // 反序列化失败时返回null
            null
        }
    }
    
    /**
     * 清除所有自定义模型
     */
    override suspend fun clearAllCustomModels() {
        userDefaults.removeObjectForKey(KEY_CUSTOM_MODEL_MANAGER)
        userDefaults.removeObjectForKey(KEY_CUSTOM_MODEL)
        userDefaults.synchronize()
    }
}