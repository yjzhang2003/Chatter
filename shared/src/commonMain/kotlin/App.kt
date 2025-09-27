import androidx.compose.runtime.*
import presentation.theme.ChatGeminiTheme
import presentation.ui.screen.ChatScreen
import presentation.ui.screen.ApiManagementScreen
import presentation.ui.screen.CustomModelConfigScreen
import presentation.ui.screen.ChatViewModel
import presentation.ui.screen.CustomModelConfigViewModel
import presentation.ui.screen.ApiManagementViewModel

/**
 * 应用程序的主入口
 * 管理聊天页面、API管理页面和自定义模型配置页面之间的导航
 */
@Composable
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.Chat) }
    var editingModelId by remember { mutableStateOf<String?>(null) }
    val chatViewModel = remember { ChatViewModel() }
    val customModelConfigViewModel = remember { CustomModelConfigViewModel() }
    val apiManagementViewModel = remember { ApiManagementViewModel() }
    
    ChatGeminiTheme {
        when (currentScreen) {
            Screen.Chat -> {
                ChatScreen(
                    viewModel = chatViewModel,
                    onApiManagementClick = { currentScreen = Screen.ApiManagement }
                )
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
    ApiManagement,
    CustomModelConfig
}

expect fun getPlatformName(): String