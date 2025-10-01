package utils

import androidx.compose.ui.graphics.ImageBitmap
import com.preat.peekaboo.image.picker.toImageBitmap
import io.ktor.util.decodeBase64Bytes

/**
 * 图片处理工具类
 * 提供Base64字符串与ImageBitmap之间的转换功能
 */
object ImageUtils {
    
    /**
     * 将Base64编码的字符串转换为ImageBitmap
     * 
     * @param base64String Base64编码的图片字符串
     * @return ImageBitmap对象，如果转换失败则返回null
     */
    fun base64ToImageBitmap(base64String: String): ImageBitmap? {
        return try {
            // 移除可能的data URL前缀 (如 "data:image/jpeg;base64,")
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            
            // 将Base64字符串解码为ByteArray
            val byteArray = cleanBase64.decodeBase64Bytes()
            
            // 使用peekaboo库将ByteArray转换为ImageBitmap
            byteArray.toImageBitmap()
        } catch (e: Exception) {
            // 如果转换失败，打印错误信息并返回null
            println("Failed to convert Base64 to ImageBitmap: ${e.message}")
            null
        }
    }
    
    /**
     * 检查字符串是否为有效的Base64格式
     * 
     * @param base64String 待检查的字符串
     * @return 如果是有效的Base64格式则返回true
     */
    fun isValidBase64(base64String: String): Boolean {
        return try {
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            cleanBase64.decodeBase64Bytes()
            true
        } catch (e: Exception) {
            false
        }
    }
}