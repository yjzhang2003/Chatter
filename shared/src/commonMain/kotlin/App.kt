package presentation

import androidx.compose.runtime.*
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
import presentation.ui.component.BottomNavigationBar
import presentation.ui.component.BottomNavTab
import domain.model.Conversation
import data.repository.ConversationRepositoryImpl
import data.database.ConversationDao
import data.database.ChatMessageDao
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
    val chatViewModel = remember { ChatViewModel() }
    val customModelConfigViewModel = remember { CustomModelConfigViewModel() }
    val apiManagementViewModel = remember { ApiManagementViewModel() }
    
    // 创建数据库依赖
    val databaseDriverFactory = remember { PlatformModule.provideDatabaseDriverFactory() }
    val sqlDriver = remember { databaseDriverFactory.createDriver() }
    val conversationDao = remember { ConversationDao(sqlDriver) }
    val chatMessageDao = remember { ChatMessageDao(sqlDriver) }
    val conversationRepository = remember { ConversationRepositoryImpl() }
    
    val conversationListViewModel = remember { ConversationListViewModel(conversationRepository) }
    val conversationDetailViewModel = remember { ConversationDetailViewModel(conversationRepository) }
    
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
                        ConversationDetailScreen(
                            conversation = conversation,
                            viewModel = conversationDetailViewModel,
                            onBackClick = { 
                                // 清理ViewModel状态，防止内存泄漏
                                conversationDetailViewModel.clearError()
                                conversationDetailViewModel.resetStatus()
                                currentScreen = Screen.ConversationList
                                selectedConversation = null
                            }
                        )
                    }
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
    ConversationDetail
}

/**
 * 获取平台名称的期望声明
 * 各平台需要提供具体实现
 */
expect fun getPlatformName(): String