package data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS平台数据库驱动工厂实现
 */
actual class DatabaseDriverFactory {
    /**
     * 创建iOS平台的数据库驱动
     */
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(ChatDatabase.Schema, "chat.db")
    }
}