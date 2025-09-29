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
 * Kimi AI服务实现
 * 集成Moonshot AI的Kimi模型
 */
@OptIn(ExperimentalSerializationApi::class)
class KimiAIService : AIService {
    
    companion object {
        private const val BASE_URL = "https://api.moonshot.cn/v1"
        private const val TIMEOUT = 30000L
        private const val MODEL_NAME = "moonshot-v1-8k"
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
            return Status.Error("API key is required for Kimi")
        }
        
        return try {
            // 构建消息列表，包含历史上下文
            val messages = mutableListOf<KimiMessage>()
            
            // 添加历史消息
            contextMessages.forEach { message ->
                when (message.sender) {
                    domain.model.MessageSender.USER -> {
                        messages.add(KimiMessage(role = "user", content = message.content))
                    }
                    domain.model.MessageSender.AI -> {
                        messages.add(KimiMessage(role = "assistant", content = message.content))
                    }
                    domain.model.MessageSender.SYSTEM -> {
                        messages.add(KimiMessage(role = "system", content = message.content))
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(KimiMessage(role = "user", content = prompt))
            
            val request = KimiRequest(
                model = MODEL_NAME,
                messages = messages,
                temperature = 0.3
            )
            
            val response = client.post("$BASE_URL/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(request))
            }
            
            val responseText = response.bodyAsText()
            val kimiResponse = Json.decodeFromString<KimiResponse>(responseText)
            
            if (kimiResponse.choices.isNotEmpty()) {
                Status.Success(kimiResponse.choices[0].message.content)
            } else {
                Status.Error("No response from Kimi")
            }
            
        } catch (e: Exception) {
            Status.Error("Kimi API error: ${e.message}")
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
private data class KimiRequest(
    val model: String,
    val messages: List<KimiMessage>,
    val temperature: Double
)

@Serializable
private data class KimiMessage(
    val role: String,
    val content: String
)

@Serializable
private data class KimiResponse(
    val choices: List<KimiChoice>
)

@Serializable
private data class KimiChoice(
    val message: KimiMessage
)