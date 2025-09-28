package data.network.service

import domain.model.CustomAIModel
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
import io.ktor.util.encodeBase64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

/**
 * 自定义AI服务实现
 * 支持用户配置的自定义AI模型API调用
 */
@OptIn(ExperimentalSerializationApi::class)
class CustomAIService(
    private val customModel: CustomAIModel
) : AIService {
    
    companion object {
        private const val TIMEOUT = 30000L
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
        if (customModel.apiKeyRequired && apiKey.isEmpty()) {
            return Status.Error("API key is required for ${customModel.displayName}")
        }
        
        // 检查多模态支持
        if (images.isNotEmpty() && !customModel.supportsMultimodal) {
            return Status.Error("${customModel.displayName}不支持图片输入")
        }
        
        return try {
            when (customModel.requestFormat) {
                CustomAIModel.RequestFormat.OPENAI_COMPATIBLE -> {
                    callOpenAICompatibleAPI(prompt, images, contextMessages)
                }
                CustomAIModel.RequestFormat.GEMINI -> {
                    callGeminiCompatibleAPI(prompt, images, contextMessages)
                }
                CustomAIModel.RequestFormat.CUSTOM -> {
                    // 对于自定义格式，使用OpenAI兼容格式作为默认
                    callOpenAICompatibleAPI(prompt, images, contextMessages)
                }
            }
        } catch (e: Exception) {
            Status.Error("${customModel.displayName} API error: ${e.message}")
        }
    }
    
    /**
     * 调用OpenAI兼容的API
     */
    private suspend fun callOpenAICompatibleAPI(
        prompt: String, 
        images: List<ByteArray>,
        contextMessages: List<domain.model.ChatMessage>
    ): Status {
        val messages = mutableListOf<OpenAIMessage>()
        
        // 添加历史上下文消息
        contextMessages.forEach { message ->
            when (message.sender) {
                domain.model.MessageSender.USER -> {
                    messages.add(OpenAIMessage(role = "user", content = JsonPrimitive(message.content)))
                }
                domain.model.MessageSender.AI -> {
                    messages.add(OpenAIMessage(role = "assistant", content = JsonPrimitive(message.content)))
                }
            }
        }
        
        // 添加当前用户消息
        if (images.isNotEmpty() && customModel.supportsMultimodal) {
            // 多模态消息
            val contentArray = buildJsonArray {
                add(buildJsonObject {
                    put("type", JsonPrimitive("text"))
                    put("text", JsonPrimitive(prompt))
                })
                
                images.forEach { image ->
                    add(buildJsonObject {
                        put("type", JsonPrimitive("image_url"))
                        put("image_url", buildJsonObject {
                            put("url", JsonPrimitive("data:image/png;base64,${image.encodeBase64()}"))
                        })
                    })
                }
            }
            
            messages.add(OpenAIMessage(role = "user", content = contentArray))
        } else {
            // 纯文本消息
            messages.add(OpenAIMessage(role = "user", content = JsonPrimitive(prompt)))
        }
        
        val request = OpenAIRequest(
            model = customModel.modelName,
            messages = messages,
            temperature = customModel.temperature,
            maxTokens = customModel.maxTokens
        )
        
        val response = client.post(customModel.apiUrl) {
            headers {
                if (customModel.apiKeyRequired) {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                customModel.headers.forEach { (key, value) ->
                    append(key, value)
                }
            }
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        
        val responseText = response.bodyAsText()
        
        return try {
            // 创建专用的JSON解析器，确保正确处理响应
            val jsonParser = Json {
                isLenient = true
                explicitNulls = false
                encodeDefaults = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
            
            // 首先尝试解析为错误响应
            try {
                val errorResponse = jsonParser.decodeFromString<OpenAIErrorResponse>(responseText)
                if (errorResponse.error != null) {
                    return Status.Error("${customModel.displayName} API error: ${errorResponse.error.message}")
                }
            } catch (e: Exception) {
                // 如果不是错误响应格式，继续尝试正常响应
            }
            
            // 解析为正常响应
            val openAIResponse = jsonParser.decodeFromString<OpenAIResponse>(responseText)
            
            if (openAIResponse.choices.isNotEmpty()) {
                val content = openAIResponse.choices[0].message.content
                Status.Success(content ?: "")
            } else {
                Status.Error("No response from ${customModel.displayName}")
            }
        } catch (e: Exception) {
            // 如果JSON解析失败，提供详细的错误信息
            val errorMessage = when {
                responseText.contains("\"error\"") -> "API返回错误响应，请检查API配置和密钥"
                responseText.contains("\"choices\"") -> "响应格式解析失败: ${e.message}"
                else -> "未知响应格式: ${e.message}"
            }
            Status.Error("${customModel.displayName} API error: $errorMessage\n原始响应: ${responseText.take(200)}...")
        }
    }
    
    /**
     * 调用Gemini兼容的API
     */
    private suspend fun callGeminiCompatibleAPI(
        prompt: String, 
        images: List<ByteArray>,
        contextMessages: List<domain.model.ChatMessage>
    ): Status {
        // 构建Gemini格式的contents数组
        val contents = mutableListOf<JsonObject>()
        
        // 添加历史上下文消息
        contextMessages.forEach { message ->
            val role = when (message.sender) {
                domain.model.MessageSender.USER -> "user"
                domain.model.MessageSender.AI -> "model"
            }
            
            val parts = listOf(Json.parseToJsonElement("""{"text": "${message.content}"}""") as JsonObject)
            val content = Json.parseToJsonElement(
                """{"role": "$role", "parts": ${Json.encodeToString(parts)}}"""
            ) as JsonObject
            contents.add(content)
        }
        
        // 添加当前用户消息
        val parts = mutableListOf<JsonObject>()
        
        // 添加文本部分
        parts.add(Json.parseToJsonElement("""{"text": "$prompt"}""") as JsonObject)
        
        // 添加图片部分
        images.forEach { image ->
            val imageData = Json.parseToJsonElement(
                """{"inlineData": {"mimeType": "image/png", "data": "${image.encodeBase64()}"}}"""
            ) as JsonObject
            parts.add(imageData)
        }
        
        val currentContent = Json.parseToJsonElement(
            """{"role": "user", "parts": ${Json.encodeToString(parts)}}"""
        ) as JsonObject
        contents.add(currentContent)
        
        val request = Json.parseToJsonElement(
            """{"contents": ${Json.encodeToString(contents)}}"""
        ) as JsonObject
        
        val url = if (customModel.apiUrl.contains("?")) {
            "${customModel.apiUrl}&key=$apiKey"
        } else {
            "${customModel.apiUrl}?key=$apiKey"
        }
        
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(request))
        }
        
        val responseText = response.bodyAsText()
        val jsonResponse = Json.parseToJsonElement(responseText) as JsonObject
        
        return try {
            val candidates = jsonResponse["candidates"]?.let { Json.decodeFromJsonElement<JsonArray>(it) }
            val text = candidates?.firstOrNull()?.let { Json.decodeFromJsonElement<JsonObject>(it) }
                ?.get("content")?.let { Json.decodeFromJsonElement<JsonObject>(it) }
                ?.get("parts")?.let { Json.decodeFromJsonElement<JsonArray>(it) }
                ?.firstOrNull()?.let { Json.decodeFromJsonElement<JsonObject>(it) }
                ?.get("text")?.jsonPrimitive?.content
            
            if (text != null) {
                Status.Success(text)
            } else {
                Status.Error("No response from ${customModel.displayName}")
            }
        } catch (e: Exception) {
            Status.Error("Failed to parse response: ${e.message}")
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
     * 检查是否支持多模态输入
     */
    override fun supportsMultimodal(): Boolean = customModel.supportsMultimodal
}

// OpenAI兼容格式的数据类
@Serializable
private data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    val temperature: Double? = null,
    val maxTokens: Int? = null
)

@Serializable
private data class OpenAIMessage(
    val role: String,
    val content: JsonElement // 使用JsonElement支持动态内容
)

@Serializable
private data class OpenAIContent(
    val type: String,
    val text: String? = null,
    val imageUrl: OpenAIImageUrl? = null
)

@Serializable
private data class OpenAIImageUrl(
    val url: String
)

/**
 * OpenAI格式的响应数据类
 */
@Serializable
private data class OpenAIResponse(
    val choices: List<OpenAIChoice>,
    val id: String? = null,
    @SerialName("object") val objectType: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val usage: OpenAIUsage? = null
)

@Serializable
private data class OpenAIChoice(
    val message: OpenAIResponseMessage,
    val index: Int? = null,
    val finish_reason: String? = null
)

@Serializable
private data class OpenAIResponseMessage(
    val content: String?,
    val role: String? = null
)

@Serializable
private data class OpenAIUsage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)

/**
 * OpenAI错误响应数据类
 */
@Serializable
private data class OpenAIErrorResponse(
    val error: OpenAIError? = null
)

@Serializable
private data class OpenAIError(
    val message: String,
    val type: String? = null,
    val code: String? = null
)