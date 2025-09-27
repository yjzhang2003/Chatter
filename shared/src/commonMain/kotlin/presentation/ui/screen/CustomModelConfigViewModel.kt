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
import domain.model.CustomAIModel
import domain.model.AIModel
import data.local.PreferencesManager
import kotlinx.datetime.Clock
import di.PlatformModule
import domain.model.CustomModelManager

/**
 * 自定义模型配置界面的UI状态
 */
data class CustomModelConfigUiState(
    val displayName: String = "",
    val apiUrl: String = "",
    val modelName: String = "",
    val apiKey: String = "",
    val requestFormat: CustomAIModel.RequestFormat = CustomAIModel.RequestFormat.OPENAI_COMPATIBLE,
    val apiKeyRequired: Boolean = true,
    val supportsMultimodal: Boolean = false,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val headers: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false,
    val editingModelId: String? = null
) {
    /**
     * 检查配置是否有效
     */
    val isValid: Boolean
        get() = displayName.isNotBlank() && apiUrl.isNotBlank() && modelName.isNotBlank() &&
                (!apiKeyRequired || apiKey.isNotBlank())
}

/**
 * 自定义模型配置ViewModel
 * 负责管理自定义模型的配置状态和业务逻辑
 */
class CustomModelConfigViewModel {
    
    private val _uiState = MutableStateFlow(CustomModelConfigUiState())
    val uiState: StateFlow<CustomModelConfigUiState> = _uiState.asStateFlow()
    
    private val preferencesManager: PreferencesManager = PlatformModule.providePreferencesManager()
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    init {
        loadExistingConfiguration()
    }
    
    /**
     * 加载现有配置
     */
    private fun loadExistingConfiguration() {
        viewModelScope.launch {
            try {
                val customModelManager = preferencesManager.getCustomModelManager()
                val activeModel = customModelManager?.getActiveModel()
                
                if (activeModel != null) {
                    val apiKey = preferencesManager.getApiKey(AIModel.CUSTOM) ?: ""
                    
                    _uiState.value = _uiState.value.copy(
                        displayName = activeModel.displayName,
                        apiUrl = activeModel.apiUrl,
                        modelName = activeModel.modelName,
                        apiKey = apiKey,
                        requestFormat = activeModel.requestFormat,
                        apiKeyRequired = activeModel.apiKeyRequired,
                        supportsMultimodal = activeModel.supportsMultimodal,
                        maxTokens = activeModel.maxTokens,
                        temperature = activeModel.temperature,
                        headers = activeModel.headers,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "加载配置失败: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 设置编辑模式并加载指定模型的配置
     */
    fun setEditMode(modelId: String) {
        viewModelScope.launch {
            try {
                val customModelManager = preferencesManager.getCustomModelManager()
                val model = customModelManager?.getAllModels()?.find { it.id == modelId }
                
                if (model != null) {
                    val apiKey = preferencesManager.getApiKey(AIModel.CUSTOM) ?: ""
                    
                    _uiState.value = _uiState.value.copy(
                        displayName = model.displayName,
                        apiUrl = model.apiUrl,
                        modelName = model.modelName,
                        apiKey = apiKey,
                        requestFormat = model.requestFormat,
                        apiKeyRequired = model.apiKeyRequired,
                        supportsMultimodal = model.supportsMultimodal,
                        maxTokens = model.maxTokens,
                        temperature = model.temperature,
                        headers = model.headers,
                        isEditMode = true,
                        editingModelId = modelId,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "加载模型配置失败: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 重置为创建模式
     */
    fun resetToCreateMode() {
        _uiState.value = CustomModelConfigUiState()
    }
    
    /**
     * 更新显示名称
     */
    fun updateDisplayName(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name)
    }
    
    /**
     * 更新API URL
     */
    fun updateApiUrl(url: String) {
        _uiState.value = _uiState.value.copy(apiUrl = url)
    }
    
    /**
     * 更新模型名称
     */
    fun updateModelName(name: String) {
        _uiState.value = _uiState.value.copy(modelName = name)
    }
    
    /**
     * 更新API密钥
     */
    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }
    
    /**
     * 更新请求格式
     */
    fun updateRequestFormat(format: CustomAIModel.RequestFormat) {
        _uiState.value = _uiState.value.copy(requestFormat = format)
    }
    
    /**
     * 更新是否需要API密钥
     */
    fun updateApiKeyRequired(required: Boolean) {
        _uiState.value = _uiState.value.copy(apiKeyRequired = required)
    }
    
    /**
     * 更新是否支持多模态
     */
    fun updateSupportsMultimodal(supports: Boolean) {
        _uiState.value = _uiState.value.copy(supportsMultimodal = supports)
    }
    
    /**
     * 更新最大令牌数
     */
    fun updateMaxTokens(tokens: Int?) {
        _uiState.value = _uiState.value.copy(maxTokens = tokens)
    }
    
    /**
     * 更新温度参数
     */
    fun updateTemperature(temp: Double?) {
        _uiState.value = _uiState.value.copy(temperature = temp)
    }
    
    /**
     * 保存配置
     */
    suspend fun saveConfiguration(): Boolean {
        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val currentState = _uiState.value
            
            // 创建自定义模型对象
            val customModel = CustomAIModel(
                id = currentState.editingModelId ?: generateUniqueId(),
                displayName = currentState.displayName,
                description = "自定义模型配置",
                apiUrl = currentState.apiUrl,
                modelName = currentState.modelName,
                requestFormat = currentState.requestFormat,
                apiKeyRequired = currentState.apiKeyRequired,
                supportsMultimodal = currentState.supportsMultimodal,
                maxTokens = currentState.maxTokens,
                temperature = currentState.temperature,
                headers = currentState.headers
            )
            
            // 获取当前的CustomModelManager
            val currentManager = preferencesManager.getCustomModelManager() ?: CustomModelManager.empty()
            
            val updatedManager = if (currentState.isEditMode) {
                // 编辑模式：更新现有模型
                val updatedModels = currentManager.getAllModels().map { model ->
                    if (model.id == currentState.editingModelId) customModel else model
                }
                currentManager.copy(models = updatedModels.associateBy { it.id })
            } else {
                // 新建模式：添加新模型并设为活跃模型
                val updatedModels = currentManager.getAllModels() + customModel
                currentManager.copy(
                    models = updatedModels.associateBy { it.id },
                    activeModelId = customModel.id
                )
            }
            
            // 保存更新后的CustomModelManager
            preferencesManager.saveCustomModelManager(updatedManager)
            
            // 如果需要API密钥，保存API密钥
            if (currentState.apiKeyRequired && currentState.apiKey.isNotBlank()) {
                preferencesManager.saveApiKey(AIModel.CUSTOM, currentState.apiKey)
            }
            
            _uiState.value = _uiState.value.copy(isLoading = false)
            true
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "保存配置失败: ${e.message}"
            )
            false
        }
    }
    
    /**
     * 生成唯一ID
     */
    private fun generateUniqueId(): String {
        return "custom_${Clock.System.now().toEpochMilliseconds()}"
    }
    
    /**
     * 加载OpenAI模板
     */
    fun loadOpenAITemplate() {
        _uiState.value = _uiState.value.copy(
            displayName = "OpenAI GPT",
            apiUrl = "https://api.openai.com/v1/chat/completions",
            modelName = "gpt-3.5-turbo",
            requestFormat = CustomAIModel.RequestFormat.OPENAI_COMPATIBLE,
            apiKeyRequired = true,
            supportsMultimodal = false,
            maxTokens = 4096,
            temperature = 0.7,
            headers = emptyMap()
        )
    }
    
    /**
     * 加载Claude模板
     */
    fun loadClaudeTemplate() {
        _uiState.value = _uiState.value.copy(
            displayName = "Anthropic Claude",
            apiUrl = "https://api.anthropic.com/v1/messages",
            modelName = "claude-3-sonnet-20240229",
            requestFormat = CustomAIModel.RequestFormat.CUSTOM,
            apiKeyRequired = true,
            supportsMultimodal = true,
            maxTokens = 4096,
            temperature = 0.7,
            headers = mapOf("anthropic-version" to "2023-06-01")
        )
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}