package presentation.ui.screen

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

import domain.model.MCPService
import domain.model.AgentMCPConfig
import domain.model.MCPServiceType
import domain.mcp.MCPServiceStatus
import domain.repository.AgentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * MCP管理界面的ViewModel
 * 负责管理MCP服务的状态和操作
 */
class MCPManagementViewModel(
    private val agentRepository: AgentRepository
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
    
    private val _uiState = MutableStateFlow(MCPManagementUiState())
    val uiState: StateFlow<MCPManagementUiState> = _uiState.asStateFlow()
    
    private val _mcpServices = mutableStateOf<List<MCPService>>(emptyList())
    val mcpServices: State<List<MCPService>> = _mcpServices
    
    init {
        loadMCPServices()
    }
    
    /**
     * 加载MCP服务列表
     */
    private fun loadMCPServices() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // 获取所有MCP服务
                val services = agentRepository.getAllMCPServices()
                _mcpServices.value = services
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载MCP服务失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 刷新MCP服务列表
     */
    fun refreshServices() {
        loadMCPServices()
    }
    
    /**
     * 切换MCP服务状态
     */
    fun toggleMCPService(service: MCPService, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updatedService = service.copy(isEnabled = enabled)
                agentRepository.updateMCPService(updatedService)
                
                // 刷新服务列表
                loadMCPServices()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "切换服务状态失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 添加新的MCP服务
     */
    fun addMCPService(name: String, endpoint: String, description: String = "") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val newService = MCPService(
                    id = generateServiceId(),
                    name = name,
                    displayName = name,
                    description = description,
                    serviceType = MCPServiceType.OTHER,
                    endpointUrl = endpoint,
                    apiVersion = "1.0",
                    authType = domain.model.AuthType.NONE,
                    authConfig = emptyMap(),
                    capabilities = emptyList(),
                    isEnabled = true,
                    isSystem = false,
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now()
                )
                
                // 使用agentRepository添加服务
                agentRepository.createMCPService(newService)
                loadMCPServices()
                
                _uiState.value = _uiState.value.copy(
                    showAddDialog = false,
                    error = null,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "添加MCP服务失败: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 删除MCP服务
     */
    fun removeMCPService(serviceId: String) {
        viewModelScope.launch {
            try {
                agentRepository.deleteMCPService(serviceId)
                loadMCPServices()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "删除MCP服务失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 显示添加服务对话框
     */
    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }
    
    /**
     * 隐藏添加服务对话框
     */
    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }
    
    /**
     * 显示服务配置对话框
     */
    fun showConfigDialog(service: MCPService) {
        _uiState.value = _uiState.value.copy(
            showConfigDialog = true,
            selectedService = service
        )
    }
    
    /**
     * 隐藏服务配置对话框
     */
    fun hideConfigDialog() {
        _uiState.value = _uiState.value.copy(
            showConfigDialog = false,
            selectedService = null
        )
    }
    
    /**
     * 为智能体配置MCP服务
     * @param agentId 智能体ID
     * @param mcpServiceId MCP服务ID
     * @param isEnabled 是否启用
     */
    fun configureAgentMCPService(agentId: String, mcpServiceId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val success = agentRepository.configureAgentMCPService(agentId, mcpServiceId, isEnabled)
                if (success) {
                    // 重新加载数据
                    loadMCPServices()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "配置MCP服务失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "配置MCP服务失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取智能体的MCP配置
     * @param agentId 智能体ID
     */
    suspend fun getAgentMCPConfigs(agentId: String): List<AgentMCPConfig> {
        return try {
            agentRepository.getAgentMCPConfigs(agentId)
        } catch (e: Exception) {
            emptyList()
        }
    }
 
 /**
  * 清除错误状态
  */
 fun clearError() {
     _uiState.value = _uiState.value.copy(error = null)
 }
 
 /**
     * 生成服务ID
     */
    private fun generateServiceId(): String {
        return "mcp_${Clock.System.now().toEpochMilliseconds()}"
    }
}

/**
 * MCP管理界面的UI状态
 */
data class MCPManagementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAddDialog: Boolean = false,
    val showConfigDialog: Boolean = false,
    val selectedService: MCPService? = null
)