package domain.model

import kotlinx.serialization.Serializable

/**
 * 自定义模型管理器
 * 负责管理多个自定义AI模型的配置
 */
@Serializable
data class CustomModelManager(
    val models: Map<String, CustomAIModel> = emptyMap(),
    val activeModelId: String? = null
) {
    
    /**
     * 添加或更新自定义模型
     */
    fun addOrUpdateModel(model: CustomAIModel): CustomModelManager {
        return copy(
            models = models + (model.id to model)
        )
    }
    
    /**
     * 删除自定义模型
     */
    fun removeModel(modelId: String): CustomModelManager {
        val newModels = models - modelId
        val newActiveId = if (activeModelId == modelId) {
            newModels.keys.firstOrNull()
        } else {
            activeModelId
        }
        
        return copy(
            models = newModels,
            activeModelId = newActiveId
        )
    }
    
    /**
     * 设置活跃的自定义模型
     */
    fun setActiveModel(modelId: String): CustomModelManager {
        return if (models.containsKey(modelId)) {
            copy(activeModelId = modelId)
        } else {
            this
        }
    }
    
    /**
     * 获取活跃的自定义模型
     */
    fun getActiveModel(): CustomAIModel? {
        return activeModelId?.let { models[it] }
    }
    
    /**
     * 获取所有自定义模型列表
     */
    fun getAllModels(): List<CustomAIModel> {
        return models.values.toList()
    }
    
    /**
     * 根据ID获取模型
     */
    fun getModelById(id: String): CustomAIModel? {
        return models[id]
    }
    
    /**
     * 检查是否有自定义模型
     */
    fun hasModels(): Boolean {
        return models.isNotEmpty()
    }
    
    /**
     * 检查是否有活跃的自定义模型
     */
    fun hasActiveModel(): Boolean {
        return activeModelId != null && models.containsKey(activeModelId)
    }
    
    companion object {
        /**
         * 创建空的管理器
         */
        fun empty(): CustomModelManager {
            return CustomModelManager()
        }
        
        /**
         * 从单个模型创建管理器（用于迁移旧数据）
         */
        fun fromSingleModel(model: CustomAIModel): CustomModelManager {
            return CustomModelManager(
                models = mapOf(model.id to model),
                activeModelId = model.id
            )
        }
    }
}