package utils

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片缓存管理器
 * 提供图片缓存功能，避免重复解码Base64图片数据
 * 
 * 功能特点：
 * 1. 内存缓存 - 避免重复解码相同的Base64图片
 * 2. 异步加载 - 在后台线程进行图片解码
 * 3. 自动清理 - 防止内存泄漏
 */
object ImageCacheManager {
    
    // 图片缓存映射表，key为Base64字符串的hash，value为ImageBitmap
    private val imageCache = mutableMapOf<String, ImageBitmap>()
    
    // 最大缓存数量，防止内存溢出
    private const val MAX_CACHE_SIZE = 100
    
    /**
     * 获取缓存的图片，如果不存在则异步加载并缓存
     * 
     * @param base64String Base64编码的图片字符串
     * @return ImageBitmap对象，如果加载失败则返回null
     */
    suspend fun getCachedImage(base64String: String): ImageBitmap? {
        // 生成缓存key（使用hash避免存储大字符串）
        val cacheKey = base64String.hashCode().toString()
        
        // 检查缓存中是否已存在
        imageCache[cacheKey]?.let { cachedImage ->
            return cachedImage
        }
        
        // 缓存中不存在，异步解码图片
        return withContext(Dispatchers.Default) {
            try {
                val imageBitmap = decodeBase64ToImageBitmap(base64String)
                
                // 将解码结果添加到缓存
                if (imageBitmap != null) {
                    // 检查缓存大小，如果超过限制则清理最旧的缓存
                    if (imageCache.size >= MAX_CACHE_SIZE) {
                        val oldestKey = imageCache.keys.first()
                        imageCache.remove(oldestKey)
                    }
                    
                    imageCache[cacheKey] = imageBitmap
                }
                
                imageBitmap
            } catch (e: Exception) {
                println("Failed to decode and cache image: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 获取ByteArray格式的缓存图片
     * 
     * @param byteArray 图片的ByteArray数据
     * @return ImageBitmap对象，如果转换失败则返回null
     */
    suspend fun getCachedImageFromByteArray(byteArray: ByteArray): ImageBitmap? {
        // 生成缓存key
        val cacheKey = "bytes_${byteArray.contentHashCode()}"
        
        // 检查缓存
        imageCache[cacheKey]?.let { cachedImage ->
            return cachedImage
        }
        
        // 缓存中不存在，异步转换
        return withContext(Dispatchers.Default) {
            try {
                val imageBitmap = byteArray.toImageBitmap()
                
                // 添加到缓存
                if (imageCache.size >= MAX_CACHE_SIZE) {
                    val oldestKey = imageCache.keys.first()
                    imageCache.remove(oldestKey)
                }
                
                imageCache[cacheKey] = imageBitmap
                imageBitmap
            } catch (e: Exception) {
                println("Failed to convert ByteArray to ImageBitmap: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 清理所有缓存
     */
    fun clearCache() {
        imageCache.clear()
    }
    
    /**
     * 获取当前缓存大小
     */
    fun getCacheSize(): Int = imageCache.size
    
    /**
     * 内部方法：将Base64字符串解码为ImageBitmap
     */
    private fun decodeBase64ToImageBitmap(base64String: String): ImageBitmap? {
        return try {
            // 移除可能的data URL前缀
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            
            // 解码为ByteArray并转换为ImageBitmap
            val byteArray = cleanBase64.decodeBase64Bytes()
            byteArray.toImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Compose组件专用的图片缓存Hook
 * 在Compose组件中使用，提供记忆化的图片加载功能
 * 
 * @param base64String Base64编码的图片字符串
 * @return State<ImageBitmap?> 图片状态，初始为null，加载完成后更新
 */
@Composable
fun rememberCachedImage(base64String: String): State<ImageBitmap?> {
    // 使用remember避免重复创建State
    val imageState = remember(base64String) { mutableStateOf<ImageBitmap?>(null) }
    
    // 使用LaunchedEffect在组件首次创建或base64String变化时加载图片
    LaunchedEffect(base64String) {
        imageState.value = ImageCacheManager.getCachedImage(base64String)
    }
    
    return imageState
}

/**
 * ByteArray版本的图片缓存Hook
 * 
 * @param byteArray 图片的ByteArray数据
 * @return State<ImageBitmap?> 图片状态
 */
@Composable
fun rememberCachedImageFromByteArray(byteArray: ByteArray): State<ImageBitmap?> {
    val imageState = remember(byteArray.contentHashCode()) { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(byteArray.contentHashCode()) {
        imageState.value = ImageCacheManager.getCachedImageFromByteArray(byteArray)
    }
    
    return imageState
}