package domain.service

import data.network.service.CustomAIService
import data.network.service.DoubaoAIService
import data.network.service.GeminiAIService
import data.network.service.KimiAIService
import domain.model.AIModel
import domain.model.CustomAIModel
import domain.model.CustomModelManager
import data.local.PreferencesManager
import di.PlatformModule

/**
 * AI服务工厂
 * 根据AI模型类型创建对应的服务实例
 */
object AIServiceFactory {
    
    private val preferencesManager: PreferencesManager = PlatformModule.providePreferencesManager()
    
    /**
     * 创建AI服务实例
     * @param model AI模型类型
     * @return 对应的AI服务实例
     */
    suspend fun createService(model: AIModel): AIService {
        return when (model) {
            AIModel.GEMINI_PRO -> GeminiAIService()
            AIModel.KIMI -> KimiAIService()
            AIModel.DOUBAO -> DoubaoAIService()
            AIModel.CUSTOM -> {
                // 获取自定义模型管理器
                val customModelManager = preferencesManager.getCustomModelManager() ?: CustomModelManager.empty()
                
                // 获取活跃的自定义模型
                val activeCustomModel = customModelManager.getActiveModel()
                    ?: customModelManager.getAllModels().firstOrNull()
                    ?: CustomAIModel.createOpenAICompatible(
                        id = "default-custom",
                        displayName = "自定义模型",
                        apiUrl = "https://api.openai.com/v1/chat/completions",
                        modelName = "gpt-3.5-turbo"
                    )
                
                CustomAIService(activeCustomModel)
            }
        }
    }
}