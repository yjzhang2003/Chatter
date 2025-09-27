package data.local

import android.content.Context
import android.content.SharedPreferences
import domain.model.AIModel
import domain.model.CustomAIModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Android平台的偏好设置管理器实现
 * 使用SharedPreferences进行本地存储
 */
actual class PreferencesManagerImpl(private val context: Context) : PreferencesManager {
    
    companion object {
        private const val PREFS_NAME = "chat_app_prefs"
        private const val KEY_API_KEY_PREFIX = "api_key_"
        private const val KEY_CURRENT_MODEL = "current_model"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_CUSTOM_MODEL_MANAGER = "custom_model_manager"
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * 保存指定模型的API密钥
     */
    override suspend fun saveApiKey(model: AIModel, apiKey: String) {
        sharedPreferences.edit()
            .putString(KEY_API_KEY_PREFIX + model.name, apiKey)
            .apply()
    }
    
    /**
     * 获取指定模型的API密钥
     */
    override suspend fun getApiKey(model: AIModel): String? {
        return sharedPreferences.getString(KEY_API_KEY_PREFIX + model.name, null)
    }
    
    /**
     * 保存当前选择的模型
     */
    override suspend fun saveCurrentModel(model: AIModel) {
        sharedPreferences.edit()
            .putString(KEY_CURRENT_MODEL, model.name)
            .apply()
    }
    
    /**
     * 获取当前选择的模型
     */
    override suspend fun getCurrentModel(): AIModel {
        val modelName = sharedPreferences.getString(KEY_CURRENT_MODEL, null)
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
        sharedPreferences.edit()
            .remove(KEY_API_KEY_PREFIX + model.name)
            .apply()
    }
    
    /**
     * 清除所有API密钥
     */
    override suspend fun clearAllApiKeys() {
        val editor = sharedPreferences.edit()
        AIModel.values().forEach { model ->
            editor.remove(KEY_API_KEY_PREFIX + model.name)
        }
        editor.apply()
    }
    
    /**
     * 保存自定义模型配置
     */
    override suspend fun saveCustomModel(customModel: CustomAIModel) {
        try {
            val jsonString = json.encodeToString(customModel)
            sharedPreferences.edit()
                .putString(KEY_CUSTOM_MODEL, jsonString)
                .apply()
        } catch (e: Exception) {
            // 序列化失败时忽略
        }
    }
    
    /**
     * 获取自定义模型配置
     */
    override suspend fun getCustomModel(): CustomAIModel? {
        return try {
            val jsonString = sharedPreferences.getString(KEY_CUSTOM_MODEL, null)
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
        sharedPreferences.edit()
            .remove(KEY_CUSTOM_MODEL)
            .apply()
    }
    
    /**
     * 保存自定义模型管理器
     */
    override suspend fun saveCustomModelManager(manager: domain.model.CustomModelManager) {
        try {
            val jsonString = json.encodeToString(manager)
            sharedPreferences.edit()
                .putString(KEY_CUSTOM_MODEL_MANAGER, jsonString)
                .apply()
        } catch (e: Exception) {
            // 序列化失败时忽略
        }
    }
    
    /**
     * 获取自定义模型管理器
     */
    override suspend fun getCustomModelManager(): domain.model.CustomModelManager? {
        return try {
            val jsonString = sharedPreferences.getString(KEY_CUSTOM_MODEL_MANAGER, null)
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
        sharedPreferences.edit()
            .remove(KEY_CUSTOM_MODEL_MANAGER)
            .remove(KEY_CUSTOM_MODEL)
            .apply()
    }
}