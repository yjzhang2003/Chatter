package data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android平台数据库驱动工厂实现
 */
actual class DatabaseDriverFactory(private val context: Context) {
    /**
     * 创建Android平台的数据库驱动
     */
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(ChatDatabase.Schema, context, "chat.db")
    }
}