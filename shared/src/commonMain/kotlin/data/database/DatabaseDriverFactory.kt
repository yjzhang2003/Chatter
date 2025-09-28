package data.database

import app.cash.sqldelight.db.SqlDriver

/**
 * 数据库驱动工厂接口
 * 为不同平台提供统一的数据库访问接口
 */
expect class DatabaseDriverFactory {
    /**
     * 创建数据库驱动
     */
    fun createDriver(): SqlDriver
}