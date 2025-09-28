package domain.model

/**
 * 支持的AI模型枚举
 * 定义了应用支持的各种大语言模型
 */
enum class AIModel(
    val displayName: String,
    val description: String,
    val requiresApiKey: Boolean = true
) {
    GEMINI_PRO(
        displayName = "Gemini Pro",
        description = "Google's advanced AI model with multimodal capabilities",
        requiresApiKey = true
    ),
    KIMI(
        displayName = "Kimi",
        description = "Moonshot AI's conversational model with long context",
        requiresApiKey = true
    ),
    DOUBAO(
        displayName = "豆包",
        description = "ByteDance's AI assistant model",
        requiresApiKey = true
    ),
    CUSTOM(
        displayName = "自定义模型",
        description = "用户自定义配置的AI模型",
        requiresApiKey = true
    );

    companion object {
        /**
         * 根据名称获取AI模型
         */
        fun fromName(name: String): AIModel? {
            return values().find { it.name == name }
        }

        /**
         * 根据字符串获取AI模型（用于数据库存储）
         */
        fun fromString(name: String): AIModel {
            return values().find { it.name == name } ?: getDefault()
        }

        /**
         * 获取默认模型
         */
        fun getDefault(): AIModel = GEMINI_PRO
        
        /**
         * 获取所有预定义模型（不包括自定义模型）
         */
        fun getPredefinedModels(): List<AIModel> {
            return values().filter { it != CUSTOM }
        }
    }
}