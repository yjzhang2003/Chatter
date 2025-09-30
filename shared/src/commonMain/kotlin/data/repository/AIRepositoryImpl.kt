package data.repository

import domain.model.Status
import domain.model.AIModel
import domain.repository.AIRepository
import domain.service.AIServiceFactory
import data.local.PreferencesManager
import di.PlatformModule
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.runBlocking

/**
 * AI Repository的实现类
 * 整合多种AI服务，提供统一的访问接口
 */
class AIRepositoryImpl : AIRepository {
    
    private val preferencesManager: PreferencesManager = PlatformModule.providePreferencesManager()
    private val serviceFactory = AIServiceFactory
    
    /**
     * 生成内容
     */
    override suspend fun generate(
        prompt: String, 
        images: List<ByteArray>,
        contextMessages: List<domain.model.ChatMessage>
    ): Status {
        return try {
            val currentModel = getCurrentModel()
            val aiService = serviceFactory.createService(currentModel)
            
            // 检查API密钥
            val apiKey = getApiKey(currentModel)
            if (currentModel.requiresApiKey && apiKey.isNullOrEmpty()) {
                return Status.Error("请先设置${currentModel.displayName}的API密钥")
            }
            
            // 设置API密钥到服务中
            if (!apiKey.isNullOrEmpty()) {
                aiService.setApiKey(apiKey)
            }
            
            // 检查多模态支持
            if (images.isNotEmpty() && !aiService.supportsMultimodal()) {
                return Status.Error("${currentModel.displayName}不支持图片输入")
            }
            
            // 调用AI服务生成内容，传递历史上下文消息
            val response = aiService.generateContent(prompt, images, contextMessages)
            
            // 直接返回AI服务的响应状态
            response
            
        } catch (e: IOException) {
            Status.Error("无法连接到服务器，请检查网络连接后重试")
        } catch (e: Exception) {
            Status.Error("发生未知错误：${e.message}")
        }
    }
    
    /**
     * 获取当前选择的AI模型
     */
    override suspend fun getCurrentModel(): AIModel {
        return preferencesManager.getCurrentModel()
    }
    
    /**
     * 设置当前使用的AI模型
     */
    override suspend fun setCurrentModel(model: AIModel) {
        preferencesManager.saveCurrentModel(model)
    }
    
    /**
     * 获取指定模型的API密钥
     */
    override suspend fun getApiKey(model: AIModel): String? {
        return preferencesManager.getApiKey(model)
    }
    
    /**
     * 设置指定模型的API密钥
     */
    override suspend fun setApiKey(model: AIModel, key: String) {
        preferencesManager.saveApiKey(model, key)
        
        // 同时更新对应AI服务的API密钥
        val aiService = serviceFactory.createService(model)
        aiService.setApiKey(key)
    }
    
    /**
     * 验证指定模型的API密钥
     */
    override suspend fun validateApiKey(model: AIModel, key: String): Boolean {
        return try {
            val aiService = serviceFactory.createService(model)
            aiService.setApiKey(key)
            aiService.validateApiKey()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查指定模型是否支持多模态输入
     */
    override fun supportsMultimodal(model: AIModel): Boolean {
        return when (model) {
            AIModel.GEMINI_PRO -> true
            AIModel.KIMI -> false
            AIModel.DOUBAO -> false
            AIModel.CUSTOM -> {
                // 对于自定义模型，优先从管理器获取活跃模型的配置，兼容旧数据
                val activeModelSupports = runBlocking {
                    preferencesManager.getCustomModelManager()?.getActiveModel()?.supportsMultimodal
                }
                activeModelSupports ?: runBlocking {
                    preferencesManager.getCustomModel()?.supportsMultimodal
                } ?: false
            }
        }
    }
}