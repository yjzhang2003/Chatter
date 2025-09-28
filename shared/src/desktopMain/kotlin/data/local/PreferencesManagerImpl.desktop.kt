package data.local

import domain.model.AIModel
import domain.model.CustomAIModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

/**
 * Desktop平台的偏好设置管理器实现
 * 使用Java Properties进行本地存储
 */
actual class PreferencesManagerImpl : PreferencesManager {
    
    companion object {
        private const val PREFS_FILE_NAME = "chat_gemini.properties"
        private const val KEY_CURRENT_MODEL = "current_model"
        private const val KEY_API_KEY_PREFIX = "api_key_"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_CUSTOM_MODEL_MANAGER = "custom_model_manager"
    }
    
    private val prefsFile: File by lazy {
        val userHome = System.getProperty("user.home")
        val configDir = File(userHome, ".config/Chatter")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        File(configDir, PREFS_FILE_NAME)
    }
    
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private fun loadProperties(): Properties {
        val properties = Properties()
        if (prefsFile.exists()) {
            try {
                FileInputStream(prefsFile).use { input ->
                    properties.load(input)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return properties
    }
    
    private fun saveProperties(properties: Properties) {
        try {
            FileOutputStream(prefsFile).use { output ->
                properties.store(output, "Chatter Preferences")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 保存指定模型的API密钥
     */
    override suspend fun saveApiKey(model: AIModel, apiKey: String) {
        val properties = loadProperties()
        properties.setProperty(KEY_API_KEY_PREFIX + model.name, apiKey)
        saveProperties(properties)
    }
    
    /**
     * 获取指定模型的API密钥
     */
    override suspend fun getApiKey(model: AIModel): String? {
        val properties = loadProperties()
        return properties.getProperty(KEY_API_KEY_PREFIX + model.name)
    }
    
    /**
     * 保存当前选择的模型
     */
    override suspend fun saveCurrentModel(model: AIModel) {
        val properties = loadProperties()
        properties.setProperty(KEY_CURRENT_MODEL, model.name)
        saveProperties(properties)
    }
    
    /**
     * 获取当前选择的模型
     */
    override suspend fun getCurrentModel(): AIModel {
        val properties = loadProperties()
        val modelName = properties.getProperty(KEY_CURRENT_MODEL)
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
        val properties = loadProperties()
        properties.remove(KEY_API_KEY_PREFIX + model.name)
        saveProperties(properties)
    }
    
    /**
     * 清除所有API密钥
     */
    override suspend fun clearAllApiKeys() {
        val properties = loadProperties()
        AIModel.values().forEach { model ->
            properties.remove(KEY_API_KEY_PREFIX + model.name)
        }
        saveProperties(properties)
    }
    
    /**
     * 保存自定义模型配置
     */
    override suspend fun saveCustomModel(customModel: CustomAIModel) {
        try {
            val jsonString = json.encodeToString(customModel)
            val properties = loadProperties()
            properties.setProperty(KEY_CUSTOM_MODEL, jsonString)
            saveProperties(properties)
        } catch (e: Exception) {
            // 序列化失败时忽略
        }
    }
    
    /**
     * 获取自定义模型配置
     */
    override suspend fun getCustomModel(): CustomAIModel? {
        return try {
            val properties = loadProperties()
            val jsonString = properties.getProperty(KEY_CUSTOM_MODEL)
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
        val properties = loadProperties()
        properties.remove(KEY_CUSTOM_MODEL)
        saveProperties(properties)
    }
    
    /**
     * 保存自定义模型管理器
     */
    override suspend fun saveCustomModelManager(manager: domain.model.CustomModelManager) {
        try {
            val jsonString = json.encodeToString(manager)
            val properties = loadProperties()
            properties.setProperty(KEY_CUSTOM_MODEL_MANAGER, jsonString)
            saveProperties(properties)
        } catch (e: Exception) {
            // 序列化失败时忽略
        }
    }
    
    /**
     * 获取自定义模型管理器
     */
    override suspend fun getCustomModelManager(): domain.model.CustomModelManager? {
        return try {
            val properties = loadProperties()
            val jsonString = properties.getProperty(KEY_CUSTOM_MODEL_MANAGER)
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
        val properties = loadProperties()
        properties.remove(KEY_CUSTOM_MODEL_MANAGER)
        properties.remove(KEY_CUSTOM_MODEL)
        saveProperties(properties)
    }
}