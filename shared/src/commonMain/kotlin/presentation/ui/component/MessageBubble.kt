package presentation.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.markdownColor
import domain.model.Message
import domain.model.ChatMessage
import domain.model.MessageSender
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import presentation.theme.Gray700
import utils.rememberCachedImage
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.layout.ContentScale

/**
 * MessageBubble组件 - 支持Message类型
 */
@Composable
inline fun MessageBubble(message: Message, modifier: Modifier = Modifier) {
    val bubbleColor =
        if (message.isBotMessage) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.secondaryContainer

    var visibility by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        visibility = true
    }
    AnimatedVisibility(
        visible = visibility,
        enter = slideInHorizontally()
                + expandHorizontally(expandFrom = Alignment.Start)
                + scaleIn(transformOrigin = TransformOrigin(0.5f, 0f))
                + fadeIn(initialAlpha = 0.3f),
    ) {
        Box(
            contentAlignment = if (!message.isBotMessage) Alignment.CenterEnd else Alignment.CenterStart,
            modifier = modifier
                .padding(
                    start = if (message.isBotMessage) 0.dp else 50.dp,
                    end = if (message.isBotMessage) 50.dp else 0.dp,
                )
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 20.dp,
                                    bottomEnd = 20.dp,
                                    topEnd = if (message.isBotMessage) 20.dp else 2.dp,
                                    topStart = if (message.isBotMessage) 2.dp else 20.dp
                                )
                            )
                            .background(color = bubbleColor)
                            .padding(vertical = 5.dp, horizontal = 16.dp),
                    ) {
                        Column {
                            Text(
                                text = message.sender.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Left,
                                color = if (message.isBotMessage) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary,
                            )
                            if (message.isBotMessage && message.isLoading) {
                                LoadingAnimation(
                                    circleSize = 8.dp,
                                    spaceBetween = 5.dp,
                                    travelDistance = 10.dp,
                                    circleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 14.dp)
                                )
                            } else {
                                Markdown(
                                    content = message.text,
                                    colors = markdownColor(
                                        text = LocalContentColor.current,
                                        codeText = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.wrapContentWidth()
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = message.time,
                                    textAlign = TextAlign.End,
                                    fontSize = 12.sp,
                                    color = Gray700
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * MessageBubble组件 - 支持ChatMessage类型
 */
@Composable
inline fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val bubbleColor =
        if (message.sender == MessageSender.AI) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.secondaryContainer

    var visibility by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        visibility = true
    }
    AnimatedVisibility(
        visible = visibility,
        enter = slideInHorizontally()
                + expandHorizontally(expandFrom = Alignment.Start)
                + scaleIn(transformOrigin = TransformOrigin(0.5f, 0f))
                + fadeIn(initialAlpha = 0.3f),
    ) {
        Box(
            contentAlignment = if (message.sender == MessageSender.USER) Alignment.CenterEnd else Alignment.CenterStart,
            modifier = modifier
                .padding(
                    start = if (message.sender == MessageSender.AI) 0.dp else 50.dp,
                    end = if (message.sender == MessageSender.AI) 50.dp else 0.dp,
                )
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 20.dp,
                                    bottomEnd = 20.dp,
                                    topEnd = if (message.sender == MessageSender.AI) 20.dp else 2.dp,
                                    topStart = if (message.sender == MessageSender.AI) 2.dp else 20.dp
                                )
                            )
                            .background(color = bubbleColor)
                            .padding(vertical = 5.dp, horizontal = 16.dp),
                    ) {
                        Column {
                            Text(
                                text = when (message.sender) {
                                    MessageSender.USER -> "You"
                                    MessageSender.AI -> "Chatter"
                                    MessageSender.SYSTEM -> "System"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Left,
                                color = if (message.sender == MessageSender.AI) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary,
                            )
                            if (message.sender == MessageSender.AI && message.isLoading) {
                                LoadingAnimation(
                                    circleSize = 8.dp,
                                    spaceBetween = 5.dp,
                                    travelDistance = 10.dp,
                                    circleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 14.dp)
                                )
                            } else {
                                // 显示图片（如果有）
                                if (message.hasImages()) {
                                    LazyRow {
                                        items(message.images) { base64Image ->
                                            val imageState = rememberCachedImage(base64Image)
                                            val imageBitmap = imageState.value
                                            if (imageBitmap != null) {
                                                Image(
                                                    bitmap = imageBitmap,
                                                    contentDescription = "Message Image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .heightIn(100.dp, 200.dp)
                                                        .widthIn(100.dp, 200.dp)
                                                        .padding(end = 8.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                )
                                            }
                                        }
                                    }
                                    if (message.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                                
                                // 显示文本内容（如果有）
                                if (message.content.isNotBlank()) {
                                    Markdown(
                                        content = message.content,
                                        colors = markdownColor(
                                            text = LocalContentColor.current,
                                            codeText = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.wrapContentWidth()
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = message.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).let { datetime ->
                                        val hour = if (datetime.hour < 10) "0${datetime.hour}" else datetime.hour
                                        val minute = if (datetime.minute < 10) "0${datetime.minute}" else datetime.minute
                                        "${hour}:${minute}"
                                    },
                                    textAlign = TextAlign.End,
                                    fontSize = 12.sp,
                                    color = Gray700
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
