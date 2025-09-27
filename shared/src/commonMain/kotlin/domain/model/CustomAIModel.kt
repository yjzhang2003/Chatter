package domain.model

import kotlinx.serialization.Serializable

/**
 * 自定义AI模型配置
 * 允许用户配置自己的AI模型API
 */
@Serializable
data class CustomAIModel(
    val id: String,
    val displayName: String,
    val description: String,
    val apiUrl: String,
    val modelName: String,
    val apiKeyRequired: Boolean = true,
    val supportsMultimodal: Boolean = false,
    val requestFormat: RequestFormat = RequestFormat.OPENAI_COMPATIBLE,
    val headers: Map<String, String> = emptyMap(),
    val maxTokens: Int? = null,
    val temperature: Double? = null
) {
    /**
     * 支持的请求格式类型
     */
    enum class RequestFormat {
        OPENAI_COMPATIBLE,  // OpenAI兼容格式 (如GPT、Claude等)
        GEMINI,            // Google Gemini格式
        CUSTOM             // 自定义格式
    }
    
    companion object {
        /**
         * 创建OpenAI兼容的模型配置
         */
        fun createOpenAICompatible(
            id: String,
            displayName: String,
            apiUrl: String,
            modelName: String,
            description: String = "自定义OpenAI兼容模型"
        ): CustomAIModel {
            return CustomAIModel(
                id = id,
                displayName = displayName,
                description = description,
                apiUrl = apiUrl,
                modelName = modelName,
                requestFormat = RequestFormat.OPENAI_COMPATIBLE,
                headers = mapOf("Content-Type" to "application/json")
            )
        }
        
        /**
         * 创建Gemini兼容的模型配置
         */
        fun createGeminiCompatible(
            id: String,
            displayName: String,
            apiUrl: String,
            modelName: String,
            description: String = "自定义Gemini兼容模型"
        ): CustomAIModel {
            return CustomAIModel(
                id = id,
                displayName = displayName,
                description = description,
                apiUrl = apiUrl,
                modelName = modelName,
                requestFormat = RequestFormat.GEMINI,
                supportsMultimodal = true
            )
        }
    }
}