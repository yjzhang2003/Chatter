package presentation

import androidx.compose.runtime.*
import presentation.theme.ChatGeminiTheme
import presentation.ui.screen.ChatScreen
import presentation.ui.screen.ApiManagementScreen
import presentation.ui.screen.CustomModelConfigScreen
import presentation.ui.screen.ConversationListScreen
import presentation.ui.screen.ConversationDetailScreen
import presentation.ui.screen.ChatViewModel
import presentation.ui.screen.CustomModelConfigViewModel
import presentation.ui.screen.ApiManagementViewModel
import presentation.ui.screen.ConversationListViewModel
import presentation.ui.screen.ConversationDetailViewModel
import domain.model.Conversation
import data.repository.ConversationRepositoryImpl
import data.database.ConversationDao
import data.database.ChatMessageDao
import di.PlatformModule

/**
 * 应用程序的主入口
 * 管理聊天页面、API管理页面、自定义模型配置页面、对话列表页面和对话详情页面之间的导航
 */
@Composable
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.Chat) }
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
    
    ChatGeminiTheme {
        when (currentScreen) {
            Screen.Chat -> {
                ChatScreen(
                    viewModel = chatViewModel,
                    onApiManagementClick = { currentScreen = Screen.ApiManagement },
                    onConversationListClick = { currentScreen = Screen.ConversationList }
                )
            }
            Screen.ConversationList -> {
                ConversationListScreen(
                    viewModel = conversationListViewModel,
                    onBackClick = { currentScreen = Screen.Chat },
                    onConversationClick = { conversation ->
                        selectedConversation = conversation
                        currentScreen = Screen.ConversationDetail
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
            Screen.ApiManagement -> {
                ApiManagementScreen(
                    viewModel = apiManagementViewModel,
                    onBackClick = { 
                        // 从API管理页面返回时刷新聊天页面的模型信息
                        chatViewModel.refreshCurrentModel()
                        currentScreen = Screen.Chat 
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
        }
    }
}

/**
 * 应用程序的屏幕枚举
 */
enum class Screen {
    Chat,
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