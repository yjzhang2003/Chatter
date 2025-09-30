package presentation

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import presentation.theme.ChatterTheme
import presentation.ui.screen.ChatScreen
import presentation.ui.screen.ApiManagementScreen
import presentation.ui.screen.CustomModelConfigScreen
import presentation.ui.screen.ConversationListScreen
import presentation.ui.screen.ConversationDetailScreen
import presentation.ui.screen.SettingsScreen
import presentation.ui.screen.ChatViewModel
import presentation.ui.screen.CustomModelConfigViewModel
import presentation.ui.screen.ApiManagementViewModel
import presentation.ui.screen.ConversationListViewModel
import presentation.ui.screen.ConversationDetailViewModel
import presentation.ui.screen.AgentSelectionScreen
import presentation.ui.screen.AgentEditScreen
import presentation.ui.screen.AgentViewModel
import presentation.ui.component.BottomNavigationBar
import presentation.ui.component.BottomNavTab
import domain.model.Conversation
import domain.model.Agent
import data.repository.ConversationRepositoryDatabaseImpl
import data.repository.AgentRepositoryImpl
import data.database.ConversationDao
import data.database.ChatMessageDao
import data.database.AgentDao
import data.database.AgentMemoryDao
import data.database.MCPServiceDao
import di.PlatformModule
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

/**
 * 应用程序的主入口
 * 使用底部导航栏管理对话列表和设置功能
 */
@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(BottomNavTab.CONVERSATIONS) }
    var currentScreen by remember { mutableStateOf(Screen.ConversationList) }
    var editingModelId by remember { mutableStateOf<String?>(null) }
    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var selectedAgent by remember { mutableStateOf<Agent?>(null) }
    var isEditingAgent by remember { mutableStateOf(false) }
    val chatViewModel = remember { ChatViewModel() }
    val customModelConfigViewModel = remember { CustomModelConfigViewModel() }
    val apiManagementViewModel = remember { ApiManagementViewModel() }
    
    // 创建协程作用域
    val scope = rememberCoroutineScope()
    
    // 创建数据库依赖
    val databaseDriverFactory = remember { PlatformModule.provideDatabaseDriverFactory() }
    val sqlDriver = remember { databaseDriverFactory.createDriver() }
    val conversationDao = remember { ConversationDao(sqlDriver) }
    val chatMessageDao = remember { ChatMessageDao(sqlDriver) }
    val agentDao = remember { AgentDao(sqlDriver) }
    val agentMemoryDao = remember { AgentMemoryDao(sqlDriver) }
    val mcpServiceDao = remember { MCPServiceDao(sqlDriver) }
    val agentRepository = remember { AgentRepositoryImpl(agentDao, agentMemoryDao, mcpServiceDao) }
    
    // 对话仓库 - 使用基于数据库的实现
    val conversationRepository = remember { ConversationRepositoryDatabaseImpl(conversationDao, chatMessageDao) }
    val conversationListViewModel = ConversationListViewModel(conversationRepository)
    val conversationDetailViewModel = ConversationDetailViewModel(conversationRepository, agentRepository)
    val agentViewModel = remember { AgentViewModel(agentRepository) }
    
    ChatterTheme {
        Scaffold(
            bottomBar = {
                // 只在主要界面（对话列表和设置）显示底部导航栏
                if (currentScreen == Screen.ConversationList || currentScreen == Screen.Settings) {
                    BottomNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                            currentScreen = when (tab) {
                                BottomNavTab.CONVERSATIONS -> Screen.ConversationList
                                BottomNavTab.SETTINGS -> Screen.Settings
                            }
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            when (currentScreen) {
                Screen.ConversationList -> {
                    ConversationListScreen(
                        viewModel = conversationListViewModel,
                        onBackClick = { 
                            // 对话列表是主界面，不需要返回操作
                        },
                        onConversationClick = { conversation ->
                            selectedConversation = conversation
                            currentScreen = Screen.Chat
                        }
                    )
                }
                Screen.Chat -> {
                    selectedConversation?.let { conversation ->
                        // 根据对话的agentId获取当前智能体
                        val currentAgent = conversation.agentId?.let { agentId ->
                            agentViewModel.getAgentById(agentId)
                        }
                        
                        ConversationDetailScreen(
                            conversation = conversation,
                            viewModel = conversationDetailViewModel,
                            currentAgent = currentAgent,
                            onBackClick = { 
                                // 清理ViewModel状态，防止内存泄漏
                                conversationDetailViewModel.clearError()
                                conversationDetailViewModel.resetStatus()
                                currentScreen = Screen.ConversationList
                                selectedConversation = null
                            },
                            onAgentSettingsClick = {
                                currentScreen = Screen.AgentSelection
                            }
                        )
                    }
                }
                Screen.AgentSelection -> {
                    val agentUiState by agentViewModel.uiState
                    AgentSelectionScreen(
                        agents = agentUiState.agents,
                        currentAgentId = selectedConversation?.agentId,
                        onBackClick = {
                            currentScreen = Screen.Chat
                        },
                        onAgentSelected = { agent ->
                            // 更新当前对话的智能体
                            println("Debug: 选择智能体 - agentId: ${agent.id}, agentName: ${agent.name}")
                            selectedConversation?.let { conversation ->
                                println("Debug: 更新前对话信息 - conversationId: ${conversation.id}, 原agentId: ${conversation.agentId}")
                                val updatedConversation = conversation.copy(agentId = agent.id)
                                selectedConversation = updatedConversation
                                println("Debug: 更新后对话信息 - conversationId: ${updatedConversation.id}, 新agentId: ${updatedConversation.agentId}")
                                
                                // 持久化到数据库
                                scope.launch {
                                    val success = conversationRepository.updateConversation(updatedConversation)
                                    println("Debug: 数据库更新结果: $success")
                                    
                                    // 刷新ConversationDetailViewModel中的对话信息
                                    if (success) {
                                        println("Debug: 准备调用refreshConversation")
                                        // 确保在主线程中调用refreshConversation
                                        launch {
                                            conversationDetailViewModel.refreshConversation()
                                        }
                                        println("Debug: refreshConversation调用完成")
                                    } else {
                                        println("Debug: 数据库更新失败，跳过刷新")
                                    }
                                }
                            }
                            currentScreen = Screen.Chat
                        },
                        onCreateAgentClick = {
                            selectedAgent = null
                            isEditingAgent = false
                            currentScreen = Screen.AgentEdit
                        },
                        onEditAgentClick = { agent ->
                            selectedAgent = agent
                            isEditingAgent = true
                            currentScreen = Screen.AgentEdit
                        },
                        onDeleteAgentClick = { agent ->
                            // 删除智能体
                            agentViewModel.deleteAgent(agent.id)
                        }
                    )
                }
                Screen.AgentEdit -> {
                    AgentEditScreen(
                        agent = selectedAgent,
                        onBackClick = {
                            currentScreen = Screen.AgentSelection
                        },
                        onSaveClick = { name, description, systemPrompt, avatar ->
                            if (isEditingAgent && selectedAgent != null) {
                                agentViewModel.updateAgent(
                                    selectedAgent!!.id,
                                    name,
                                    description,
                                    systemPrompt,
                                    avatar
                                )
                            } else {
                                agentViewModel.createAgent(
                                    name,
                                    description,
                                    systemPrompt,
                                    avatar
                                )
                            }
                            currentScreen = Screen.AgentSelection
                        }
                    )
                }
                Screen.Settings -> {
                    SettingsScreen(
                        onApiManagementClick = { currentScreen = Screen.ApiManagement },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Screen.ApiManagement -> {
                    ApiManagementScreen(
                        viewModel = apiManagementViewModel,
                        onBackClick = { 
                            // 从API管理页面返回时刷新聊天页面的模型信息
                            chatViewModel.refreshCurrentModel()
                            selectedTab = BottomNavTab.SETTINGS
                            currentScreen = Screen.Settings 
                        },
                        onCustomModelConfigClick = { currentScreen = Screen.CustomModelConfig },
                        onAddCustomModelClick = { 
                            // 重置为新建模式
                            customModelConfigViewModel.resetToCreateMode()
                            editingModelId = null
                            currentScreen = Screen.CustomModelConfig 
                        },
                        onEditCustomModel = { customModel ->
                            // 设置编辑模式
                            customModelConfigViewModel.setEditMode(customModel.id)
                            editingModelId = customModel.id
                            currentScreen = Screen.CustomModelConfig
                        },
                        onDeleteCustomModel = { modelId ->
                            apiManagementViewModel.deleteCustomModel(modelId)
                        }
                    )
                }
                Screen.CustomModelConfig -> {
                    CustomModelConfigScreen(
                        viewModel = customModelConfigViewModel,
                        onNavigateBack = { 
                            // 从自定义模型配置页面返回时刷新API管理页面和聊天页面的模型信息
                            apiManagementViewModel.refreshData()
                            chatViewModel.refreshCurrentModel()
                            currentScreen = Screen.ApiManagement 
                        }
                    )
                }
                Screen.ConversationDetail -> {
                    selectedConversation?.let { conversation ->
                        ConversationDetailScreen(
                            conversation = conversation,
                            viewModel = conversationDetailViewModel,
                            onBackClick = { 
                                currentScreen = Screen.ConversationList
                                selectedConversation = null
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 应用程序的屏幕枚举
 */
enum class Screen {
    Chat,
    Settings,
    ConversationList,
    ApiManagement,
    CustomModelConfig,
    ConversationDetail,
    AgentSelection,
    AgentEdit
}

/**
 * 获取平台名称的期望声明
 * 各平台需要提供具体实现
 */
expect fun getPlatformName(): String