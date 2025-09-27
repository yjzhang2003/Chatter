package presentation.ui.screen

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import data.repository.AIRepositoryImpl
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import domain.model.AIModel
import domain.model.Message
import domain.model.Sender
import domain.model.Status
import domain.repository.AIRepository
import kotlinx.coroutines.launch

/**
 * 聊天页面的ViewModel
 * 管理聊天状态和AI交互逻辑
 */
class ChatViewModel : ViewModel() {

    private val aiRepository: AIRepository = AIRepositoryImpl()

    private val _uiState = mutableStateOf(ChatUiState())
    val uiState: State<ChatUiState> = _uiState

    init {
        viewModelScope.launch {
            val currentModel = aiRepository.getCurrentModel()
            val apiKey = aiRepository.getApiKey(currentModel) ?: ""
            _uiState.value = _uiState.value.copy(
                apiKey = apiKey,
                currentModel = currentModel
            )
        }
    }

    /**
     * 设置API密钥
     */
    fun setApiKey(key: String) {
        viewModelScope.launch {
            val currentModel = aiRepository.getCurrentModel()
            aiRepository.setApiKey(currentModel, key)
            _uiState.value = _uiState.value.copy(
                apiKey = key, 
                status = Status.Success("API密钥更新成功")
            )
        }
    }

    /**
     * 生成内容
     */
    fun generateContent(message: String, images: List<ByteArray> = emptyList()) {
        viewModelScope.launch {
            addToMessages(message, images, Sender.User)
            addToMessages("", emptyList(), Sender.Bot, true)

            when (val response = aiRepository.generate(message, images)) {
                is Status.Success -> updateLastBotMessage(response.data, response)
                is Status.Error -> updateLastBotMessage(response.message, response)
                else -> {}
            }
        }
    }

    /**
     * 刷新当前模型信息
     */
    fun refreshCurrentModel() {
        viewModelScope.launch {
            val currentModel = aiRepository.getCurrentModel()
            val apiKey = aiRepository.getApiKey(currentModel) ?: ""
            _uiState.value = _uiState.value.copy(
                currentModel = currentModel,
                apiKey = apiKey
            )
        }
    }

    /**
     * 更新最后一条机器人消息
     */
    private fun updateLastBotMessage(text: String, status: Status) {
        val messages = _uiState.value.messages.toMutableList()
        if (messages.isNotEmpty() && messages.last().sender == Sender.Bot) {
            val last = messages.last()
            val updatedMessage = last.copy(text = text, isLoading = status == Status.Loading)
            messages[messages.lastIndex] = updatedMessage
            _uiState.value = _uiState.value.copy(
                messages = messages,
                status = status
            )
        }
    }

    /**
     * 添加消息到列表
     */
    private fun addToMessages(
        text: String,
        images: List<ByteArray>,
        sender: Sender,
        isLoading: Boolean = false
    ) {
        val message = Message(sender, text, images, isLoading)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + message,
            status = if (isLoading) Status.Loading else Status.Idle
        )
    }
}