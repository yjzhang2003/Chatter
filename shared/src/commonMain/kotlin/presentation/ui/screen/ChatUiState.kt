package presentation.ui.screen

import domain.model.Message
import domain.model.Status
import domain.model.AIModel

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val status: Status = Status.Idle,
    val apiKey: String = "",
    val currentModel: AIModel = AIModel.getDefault()
)