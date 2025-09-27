package presentation.ui.screen

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import domain.model.AIModel
import domain.model.CustomModelManager
import data.local.PreferencesManager
import di.PlatformModule

/**
 * API管理页面的UI状态
 */
data class ApiManagementUiState(
    val currentModel: AIModel = AIModel.getDefault(),
    val apiKeys: Map<AIModel, String> = emptyMap(),
    val hasCustomModel: Boolean = false,
    val customModel: domain.model.CustomAIModel? = null,
    val customModelManager: CustomModelManager = CustomModelManager.empty(),
    val customModels: List<domain.model.CustomAIModel> = emptyList(),
    val activeCustomModel: domain.model.CustomAIModel? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * API管理页面的ViewModel
 * 负责管理API密钥和模型选择的业务逻辑
 */
class ApiManagementViewModel {
    
    private val _uiState = MutableStateFlow(ApiManagementUiState())
    val uiState: StateFlow<ApiManagementUiState> = _uiState.asStateFlow()
    
    private val preferencesManager: PreferencesManager = PlatformModule.providePreferencesManager()
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    init {
        loadApiKeys()
    }
    
    /**
     * 加载已保存的API密钥和当前模型
     */
    private fun loadApiKeys() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val currentModel = preferencesManager.getCurrentModel()
                val savedKeys = mutableMapOf<AIModel, String>()
                
                // 加载所有模型的API密钥
                AIModel.values().forEach { model ->
                    val apiKey = preferencesManager.getApiKey(model)
                    if (!apiKey.isNullOrEmpty()) {
                        savedKeys[model] = apiKey
                    }
                }
                
                // 检查是否有自定义模型配置
                val customModel = preferencesManager.getCustomModel()
                val hasCustomModel = customModel != null
                
                // 加载自定义模型管理器
                val customModelManager = preferencesManager.getCustomModelManager() ?: CustomModelManager.empty()
                val customModels = customModelManager.getAllModels()
                val activeCustomModel = customModelManager.getActiveModel()
                
                _uiState.value = _uiState.value.copy(
                    currentModel = currentModel,
                    apiKeys = savedKeys,
                    hasCustomModel = hasCustomModel,
                    customModel = customModel,
                    customModelManager = customModelManager,
                    customModels = customModels,
                    activeCustomModel = activeCustomModel,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "加载配置失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 更新指定模型的API密钥
     */
    fun updateApiKey(model: AIModel, apiKey: String) {
        val updatedKeys = _uiState.value.apiKeys.toMutableMap()
        if (apiKey.isNotEmpty()) {
            updatedKeys[model] = apiKey
        } else {
            updatedKeys.remove(model)
        }
        
        _uiState.value = _uiState.value.copy(
            apiKeys = updatedKeys
        )
        
        // 异步保存到本地存储
        viewModelScope.launch {
            try {
                if (apiKey.isNotEmpty()) {
                    preferencesManager.saveApiKey(model, apiKey)
                } else {
                    preferencesManager.clearApiKey(model)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "保存API密钥失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 选择当前使用的模型
     */
    fun selectModel(model: AIModel) {
        val apiKey = _uiState.value.apiKeys[model]
        
        // 对于自定义模型，检查是否已配置
        if (model == AIModel.CUSTOM && _uiState.value.customModels.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "请先配置自定义模型"
            )
            return
        }
        
        // 检查是否有API密钥（如果需要的话）
        if (model.requiresApiKey && (apiKey.isNullOrEmpty())) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "请先设置${model.displayName}的API密钥"
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(
            currentModel = model,
            errorMessage = null
        )
        
        // 异步保存当前选择的模型到本地存储
        viewModelScope.launch {
            try {
                preferencesManager.saveCurrentModel(model)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "保存模型选择失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * 刷新配置数据
     */
    fun refreshData() {
        loadApiKeys()
    }
    
    /**
     * 添加新的自定义模型
     */
    fun addCustomModel(customModel: domain.model.CustomAIModel) {
        viewModelScope.launch {
            try {
                val currentManager = _uiState.value.customModelManager
                val updatedManager = currentManager.addOrUpdateModel(customModel)
                
                preferencesManager.saveCustomModelManager(updatedManager)
                
                _uiState.value = _uiState.value.copy(
                    customModelManager = updatedManager,
                    customModels = updatedManager.getAllModels(),
                    activeCustomModel = updatedManager.getActiveModel(),
                    hasCustomModel = updatedManager.getAllModels().isNotEmpty(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "添加自定义模型失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 更新自定义模型
     */
    fun updateCustomModel(customModel: domain.model.CustomAIModel) {
        viewModelScope.launch {
            try {
                val currentManager = _uiState.value.customModelManager
                val updatedManager = currentManager.addOrUpdateModel(customModel)
                
                preferencesManager.saveCustomModelManager(updatedManager)
                
                _uiState.value = _uiState.value.copy(
                    customModelManager = updatedManager,
                    customModels = updatedManager.getAllModels(),
                    activeCustomModel = updatedManager.getActiveModel(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "更新自定义模型失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 删除自定义模型
     */
    fun deleteCustomModel(modelId: String) {
        viewModelScope.launch {
            try {
                val currentManager = _uiState.value.customModelManager
                val updatedManager = currentManager.removeModel(modelId)
                
                preferencesManager.saveCustomModelManager(updatedManager)
                
                _uiState.value = _uiState.value.copy(
                    customModelManager = updatedManager,
                    customModels = updatedManager.getAllModels(),
                    activeCustomModel = updatedManager.getActiveModel(),
                    hasCustomModel = updatedManager.getAllModels().isNotEmpty(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除自定义模型失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 设置活跃的自定义模型
     */
    fun setActiveCustomModel(modelId: String) {
        viewModelScope.launch {
            try {
                val currentManager = _uiState.value.customModelManager
                val updatedManager = currentManager.setActiveModel(modelId)
                
                preferencesManager.saveCustomModelManager(updatedManager)
                
                _uiState.value = _uiState.value.copy(
                    customModelManager = updatedManager,
                    activeCustomModel = updatedManager.getActiveModel(),
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "设置活跃模型失败: ${e.message}"
                )
            }
        }
    }
}