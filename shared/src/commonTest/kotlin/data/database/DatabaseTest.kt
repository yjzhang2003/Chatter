package data.database

import domain.model.Conversation
import domain.model.ChatMessage
import domain.model.MessageSender
import domain.model.MessageMetadata
import domain.model.AIModel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * 数据库功能测试类
 * 测试对话和消息的数据库操作功能
 */
class DatabaseTest {
    
    private lateinit var conversationDao: ConversationDao
    private lateinit var chatMessageDao: ChatMessageDao
    
    @BeforeTest
    fun setup() {
        // 使用内存数据库进行测试
        val driver = createInMemoryDriver()
        conversationDao = ConversationDao(driver)
        chatMessageDao = ChatMessageDao(driver)
    }
    
    /**
     * 测试对话的创建和查询
     */
    @Test
    fun testConversationCRUD() = runTest {
        // 创建测试对话
        val conversation = Conversation.create(
            title = "测试对话",
            aiModel = AIModel.GEMINI_PRO
        )
        
        // 测试插入
        val insertResult = conversationDao.insertConversation(conversation)
        assertTrue(insertResult, "对话插入应该成功")
        
        // 测试查询单个对话
        val retrievedConversation = conversationDao.getConversationById(conversation.id)
        assertNotNull(retrievedConversation, "应该能够查询到插入的对话")
        assertEquals(conversation.id, retrievedConversation.id)
        assertEquals(conversation.title, retrievedConversation.title)
        assertEquals(conversation.aiModel, retrievedConversation.aiModel)
        
        // 测试查询所有对话
        val allConversations = conversationDao.getAllConversations()
        assertEquals(1, allConversations.size, "应该有一个对话")
        assertEquals(conversation.id, allConversations[0].id)
        
        // 测试更新对话
        val updatedConversation = conversation.copy(
            title = "更新后的对话标题",
            messageCount = 5,
            lastMessage = "最后一条消息"
        )
        val updateResult = conversationDao.updateConversation(updatedConversation)
        assertTrue(updateResult, "对话更新应该成功")
        
        // 验证更新结果
        val updatedRetrieved = conversationDao.getConversationById(conversation.id)
        assertNotNull(updatedRetrieved)
        assertEquals("更新后的对话标题", updatedRetrieved.title)
        assertEquals(5, updatedRetrieved.messageCount)
        assertEquals("最后一条消息", updatedRetrieved.lastMessage)
        
        // 测试删除对话
        val deleteResult = conversationDao.deleteConversation(conversation.id)
        assertTrue(deleteResult, "对话删除应该成功")
        
        // 验证删除结果
        val deletedConversation = conversationDao.getConversationById(conversation.id)
        assertNull(deletedConversation, "删除后应该查询不到对话")
        
        val emptyList = conversationDao.getAllConversations()
        assertEquals(0, emptyList.size, "删除后对话列表应该为空")
    }
    
    /**
     * 测试消息的创建和查询
     */
    @Test
    fun testMessageCRUD() = runTest {
        // 先创建一个对话
        val conversation = Conversation.create(
            title = "测试对话",
            aiModel = AIModel.GEMINI_PRO
        )
        conversationDao.insertConversation(conversation)
        
        // 创建测试消息
        val userMessage = ChatMessage.create(
            conversationId = conversation.id,
            content = "用户消息内容",
            sender = MessageSender.USER,
            images = listOf("image1.jpg", "image2.jpg")
        )
        
        val aiMessage = ChatMessage.create(
            conversationId = conversation.id,
            content = "AI回复内容",
            sender = MessageSender.AI,
            aiModel = AIModel.GEMINI_PRO
        ).copy(
            metadata = MessageMetadata(
                tokenCount = 150,
                processingTime = 2500L,
                temperature = 0.7f
            )
        )
        
        // 测试插入消息
        val insertUserResult = chatMessageDao.insertMessage(userMessage)
        assertTrue(insertUserResult, "用户消息插入应该成功")
        
        val insertAiResult = chatMessageDao.insertMessage(aiMessage)
        assertTrue(insertAiResult, "AI消息插入应该成功")
        
        // 测试查询单个消息
        val retrievedUserMessage = chatMessageDao.getMessageById(userMessage.id)
        assertNotNull(retrievedUserMessage, "应该能够查询到用户消息")
        assertEquals(userMessage.content, retrievedUserMessage.content)
        assertEquals(MessageSender.USER, retrievedUserMessage.sender)
        assertEquals(2, retrievedUserMessage.images.size)
        
        val retrievedAiMessage = chatMessageDao.getMessageById(aiMessage.id)
        assertNotNull(retrievedAiMessage, "应该能够查询到AI消息")
        assertEquals(aiMessage.content, retrievedAiMessage.content)
        assertEquals(MessageSender.AI, retrievedAiMessage.sender)
        assertEquals(AIModel.GEMINI_PRO, retrievedAiMessage.aiModel)
        assertNotNull(retrievedAiMessage.metadata)
        assertEquals(150, retrievedAiMessage.metadata?.tokenCount)
        assertEquals(2500L, retrievedAiMessage.metadata?.processingTime)
        assertEquals(0.7f, retrievedAiMessage.metadata?.temperature)
        
        // 测试按对话ID查询消息
        val conversationMessages = chatMessageDao.getMessagesByConversationId(conversation.id)
        assertEquals(2, conversationMessages.size, "对话应该有两条消息")
        
        // 验证消息顺序（按创建时间升序）
        assertEquals(MessageSender.USER, conversationMessages[0].sender)
        assertEquals(MessageSender.AI, conversationMessages[1].sender)
        
        // 测试获取最后一条消息
        val lastMessage = chatMessageDao.getLastMessageByConversationId(conversation.id)
        assertNotNull(lastMessage)
        assertEquals(MessageSender.AI, lastMessage.sender)
        assertEquals(aiMessage.content, lastMessage.content)
        
        // 测试获取消息数量
        val messageCount = chatMessageDao.getMessageCountByConversationId(conversation.id)
        assertEquals(2, messageCount)
        
        // 测试更新消息
        val updatedMessage = aiMessage.copy(
            content = "更新后的AI回复",
            isLoading = false,
            metadata = aiMessage.metadata?.copy(tokenCount = 200)
        )
        val updateResult = chatMessageDao.updateMessage(updatedMessage)
        assertTrue(updateResult, "消息更新应该成功")
        
        // 验证更新结果
        val updatedRetrieved = chatMessageDao.getMessageById(aiMessage.id)
        assertNotNull(updatedRetrieved)
        assertEquals("更新后的AI回复", updatedRetrieved.content)
        assertEquals(200, updatedRetrieved.metadata?.tokenCount)
        
        // 测试删除单条消息
        val deleteResult = chatMessageDao.deleteMessage(userMessage.id)
        assertTrue(deleteResult, "消息删除应该成功")
        
        // 验证删除结果
        val remainingMessages = chatMessageDao.getMessagesByConversationId(conversation.id)
        assertEquals(1, remainingMessages.size, "删除后应该剩余一条消息")
        assertEquals(MessageSender.AI, remainingMessages[0].sender)
        
        // 测试删除对话的所有消息
        val deleteAllResult = chatMessageDao.deleteMessagesByConversationId(conversation.id)
        assertTrue(deleteAllResult, "删除对话所有消息应该成功")
        
        val emptyMessages = chatMessageDao.getMessagesByConversationId(conversation.id)
        assertEquals(0, emptyMessages.size, "删除后消息列表应该为空")
    }
    
    /**
     * 测试搜索功能
     */
    @Test
    fun testSearchFunctionality() = runTest {
        // 创建测试数据
        val conversation1 = Conversation.create(title = "技术讨论", aiModel = AIModel.GEMINI_PRO)
        val conversation2 = Conversation.create(title = "日常聊天", aiModel = AIModel.KIMI)
        
        conversationDao.insertConversation(conversation1)
        conversationDao.insertConversation(conversation2)
        
        val message1 = ChatMessage.create(
            conversationId = conversation1.id,
            content = "讨论Kotlin多平台开发",
            sender = MessageSender.USER
        )
        val message2 = ChatMessage.create(
            conversationId = conversation1.id,
            content = "Kotlin是一门很棒的编程语言",
            sender = MessageSender.AI
        )
        val message3 = ChatMessage.create(
            conversationId = conversation2.id,
            content = "今天天气不错",
            sender = MessageSender.USER
        )
        
        chatMessageDao.insertMessage(message1)
        chatMessageDao.insertMessage(message2)
        chatMessageDao.insertMessage(message3)
        
        // 测试对话搜索
        val techConversations = conversationDao.searchConversations("技术")
        assertEquals(1, techConversations.size)
        assertEquals("技术讨论", techConversations[0].title)
        
        val chatConversations = conversationDao.searchConversations("聊天")
        assertEquals(1, chatConversations.size)
        assertEquals("日常聊天", chatConversations[0].title)
        
        // 测试消息搜索
        val kotlinMessages = chatMessageDao.searchMessages("Kotlin", null)
        assertEquals(2, kotlinMessages.size)
        
        val weatherMessages = chatMessageDao.searchMessages("天气", null)
        assertEquals(1, weatherMessages.size)
        assertEquals("今天天气不错", weatherMessages[0].content)
        
        // 测试限定对话范围的消息搜索
        val conversation1Messages = chatMessageDao.searchMessages("Kotlin", conversation1.id)
        assertEquals(2, conversation1Messages.size)
        
        val conversation2Messages = chatMessageDao.searchMessages("Kotlin", conversation2.id)
        assertEquals(0, conversation2Messages.size)
    }
    
    /**
     * 测试上下文消息获取
     */
    @Test
    fun testContextMessages() = runTest {
        // 创建对话
        val conversation = Conversation.create(title = "上下文测试", aiModel = AIModel.GEMINI_PRO)
        conversationDao.insertConversation(conversation)
        
        // 创建多条消息
        val messages = mutableListOf<ChatMessage>()
        for (i in 1..10) {
            val message = ChatMessage.create(
                conversationId = conversation.id,
                content = "消息 $i",
                sender = if (i % 2 == 1) MessageSender.USER else MessageSender.AI
            )
            messages.add(message)
            chatMessageDao.insertMessage(message)
            
            // 添加小延迟确保时间戳不同
            kotlinx.coroutines.delay(1)
        }
        
        // 测试获取最近5条消息
        val contextMessages = chatMessageDao.getContextMessages(conversation.id, 5)
        assertEquals(5, contextMessages.size)
        
        // 验证消息顺序（应该是最新的5条，按时间升序排列）
        assertEquals("消息 6", contextMessages[0].content)
        assertEquals("消息 7", contextMessages[1].content)
        assertEquals("消息 8", contextMessages[2].content)
        assertEquals("消息 9", contextMessages[3].content)
        assertEquals("消息 10", contextMessages[4].content)
        
        // 测试获取所有消息
        val allMessages = chatMessageDao.getContextMessages(conversation.id, 20)
        assertEquals(10, allMessages.size)
        assertEquals("消息 1", allMessages[0].content)
        assertEquals("消息 10", allMessages[9].content)
    }
    
    /**
     * 测试统计功能
     */
    @Test
    fun testStatistics() = runTest {
        // 测试初始状态
        val initialCount = conversationDao.getConversationCount()
        assertEquals(0, initialCount)
        
        // 创建测试数据
        val conversation1 = Conversation.create(title = "对话1", aiModel = AIModel.GEMINI_PRO)
        val conversation2 = Conversation.create(title = "对话2", aiModel = AIModel.KIMI)
        
        conversationDao.insertConversation(conversation1)
        conversationDao.insertConversation(conversation2)
        
        // 测试对话计数
        val conversationCount = conversationDao.getConversationCount()
        assertEquals(2, conversationCount)
        
        // 添加消息并测试统计更新
        val message1 = ChatMessage.create(
            conversationId = conversation1.id,
            content = "第一条消息",
            sender = MessageSender.USER
        )
        val message2 = ChatMessage.create(
            conversationId = conversation1.id,
            content = "第二条消息",
            sender = MessageSender.AI
        )
        
        chatMessageDao.insertMessage(message1)
        chatMessageDao.insertMessage(message2)
        
        // 测试消息计数
        val messageCount = chatMessageDao.getMessageCountByConversationId(conversation1.id)
        assertEquals(2, messageCount)
        
        // 测试更新对话统计
        val updateStatsResult = conversationDao.updateConversationStats(
            conversationId = conversation1.id,
            messageCount = messageCount,
            lastMessage = message2.content
        )
        assertTrue(updateStatsResult)
        
        // 验证统计更新
        val updatedConversation = conversationDao.getConversationById(conversation1.id)
        assertNotNull(updatedConversation)
        assertEquals(2, updatedConversation.messageCount)
        assertEquals("第二条消息", updatedConversation.lastMessage)
    }
    
    /**
     * 创建内存数据库驱动（用于测试）
     */
    private fun createInMemoryDriver(): app.cash.sqldelight.db.SqlDriver {
        val driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(
            url = "jdbc:sqlite::memory:",
            properties = java.util.Properties()
        )
        ChatDatabase.Schema.create(driver)
        return driver
    }
}