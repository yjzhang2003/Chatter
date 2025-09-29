package data.network.service

import domain.model.Status
import domain.service.AIService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 豆包AI服务实现
 * 集成字节跳动的豆包大模型
 */
@OptIn(ExperimentalSerializationApi::class)
class DoubaoAIService : AIService {
    
    companion object {
        private const val BASE_URL = "https://ark.cn-beijing.volces.com/api/v3"
        private const val TIMEOUT = 30000L
        private const val MODEL_NAME = "doubao-lite-4k"
    }
    
    private var apiKey: String = ""
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                explicitNulls = false
                encodeDefaults = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = TIMEOUT
            socketTimeoutMillis = TIMEOUT
            requestTimeoutMillis = TIMEOUT
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
    
    /**
     * 生成内容
     */
    override suspend fun generateContent(
        prompt: String, 
        images: List<ByteArray>,
        contextMessages: List<domain.model.ChatMessage>
    ): Status {
        if (apiKey.isEmpty()) {
            return Status.Error("API key is required for Doubao")
        }
        
        return try {
            // 构建消息列表，包含历史上下文
            val messages = mutableListOf<DoubaoMessage>()
            
            // 添加历史消息
            contextMessages.forEach { message ->
                when (message.sender) {
                    domain.model.MessageSender.USER -> {
                        messages.add(DoubaoMessage(role = "user", content = message.content))
                    }
                    domain.model.MessageSender.AI -> {
                        messages.add(DoubaoMessage(role = "assistant", content = message.content))
                    }
                    domain.model.MessageSender.SYSTEM -> {
                        messages.add(DoubaoMessage(role = "system", content = message.content))
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(DoubaoMessage(role = "user", content = prompt))
            
            val request = DoubaoRequest(
                model = MODEL_NAME,
                messages = messages
            )
            
            val response = client.post("$BASE_URL/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(request))
            }
            
            val responseText = response.bodyAsText()
            val doubaoResponse = Json.decodeFromString<DoubaoResponse>(responseText)
            
            if (doubaoResponse.choices.isNotEmpty()) {
                Status.Success(doubaoResponse.choices[0].message.content)
            } else {
                Status.Error("No response from Doubao")
            }
            
        } catch (e: Exception) {
            Status.Error("Doubao API error: ${e.message}")
        }
    }
    
    /**
     * 获取API密钥
     */
    override fun getApiKey(): String = apiKey
    
    /**
     * 设置API密钥
     */
    override fun setApiKey(key: String) {
        apiKey = key
    }
    
    /**
     * 验证API密钥
     */
    override suspend fun validateApiKey(): Boolean {
        return try {
            generateContent("test", emptyList()) is Status.Success
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 不支持多模态输入
     */
    override fun supportsMultimodal(): Boolean = false
}

@Serializable
private data class DoubaoRequest(
    val model: String,
    val messages: List<DoubaoMessage>
)

@Serializable
private data class DoubaoMessage(
    val role: String,
    val content: String
)

@Serializable
private data class DoubaoResponse(
    val choices: List<DoubaoChoice>
)

@Serializable
private data class DoubaoChoice(
    val message: DoubaoMessage
)