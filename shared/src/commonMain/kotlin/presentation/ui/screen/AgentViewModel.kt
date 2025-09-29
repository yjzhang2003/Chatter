package presentation.ui.screen

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import domain.model.Agent
import domain.repository.AgentRepository
import kotlinx.coroutines.launch

/**
 * 智能体管理ViewModel
 * 处理智能体的加载、创建、更新和删除等业务逻辑
 */
class AgentViewModel(
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(AgentUiState())
    val uiState: State<AgentUiState> = _uiState

    init {
        loadAgents()
    }

    /**
     * 加载所有智能体
     */
    fun loadAgents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val agents = agentRepository.getAllAgents()
                _uiState.value = _uiState.value.copy(
                    agents = agents,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载智能体失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 根据ID获取智能体
     */
    fun getAgentById(id: String): Agent? {
        return _uiState.value.agents.find { it.id == id }
    }

    /**
     * 创建新智能体
     */
    fun createAgent(
        name: String,
        description: String,
        systemPrompt: String,
        avatar: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val newAgent = Agent.create(
                    name = name,
                    description = description,
                    systemPrompt = systemPrompt,
                    avatar = avatar
                )
                
                val success = agentRepository.createAgent(newAgent)
                if (success) {
                    loadAgents() // 重新加载列表
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "创建智能体失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "创建智能体失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 更新智能体
     */
    fun updateAgent(
        agentId: String,
        name: String,
        description: String,
        systemPrompt: String,
        avatar: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val existingAgent = getAgentById(agentId)
                if (existingAgent != null) {
                    val updatedAgent = existingAgent.update(
                        name = name,
                        description = description,
                        systemPrompt = systemPrompt,
                        avatar = avatar
                    )
                    
                    val success = agentRepository.updateAgent(updatedAgent)
                    if (success) {
                        loadAgents() // 重新加载列表
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "更新智能体失败"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "智能体不存在"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "更新智能体失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 删除智能体
     */
    fun deleteAgent(agentId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val success = agentRepository.deleteAgent(agentId)
                if (success) {
                    loadAgents() // 重新加载列表
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "删除智能体失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "删除智能体失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 增加智能体使用次数
     */
    /**
     * 搜索智能体
     */
    fun searchAgents(query: String) {
        if (query.isBlank()) {
            loadAgents()
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val agents = agentRepository.searchAgents(query)
                _uiState.value = _uiState.value.copy(
                    agents = agents,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "搜索智能体失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取预设智能体
     */
    fun getPresetAgents(): List<Agent> {
        return _uiState.value.agents.filter { it.isPreset }
    }

    /**
     * 获取自定义智能体
     */
    fun getCustomAgents(): List<Agent> {
        return _uiState.value.agents.filter { !it.isPreset }
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * 智能体UI状态
 */
data class AgentUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 是否有智能体
     */
    val hasAgents: Boolean
        get() = agents.isNotEmpty()

    /**
     * 预设智能体数量
     */
    val presetAgentCount: Int
        get() = agents.count { it.isPreset }

    /**
     * 自定义智能体数量
     */
    val customAgentCount: Int
        get() = agents.count { !it.isPreset }
}