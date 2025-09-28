package data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * 桌面平台数据库驱动工厂实现
 */
actual class DatabaseDriverFactory {
    /**
     * 创建桌面平台的数据库驱动
     */
    actual fun createDriver(): SqlDriver {
        val databasePath = getDatabasePath()
        val driver = JdbcSqliteDriver("jdbc:sqlite:$databasePath")
        ChatDatabase.Schema.create(driver)
        return driver
    }
    
    /**
     * 获取数据库文件路径
     */
    private fun getDatabasePath(): String {
        val userHome = System.getProperty("user.home")
        val configDir = File(userHome, ".config/Chatter")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        return File(configDir, "chat.db").absolutePath
    }
}