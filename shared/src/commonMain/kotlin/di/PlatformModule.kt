package di

import data.local.PreferencesManager
import data.database.DatabaseDriverFactory

/**
 * 平台模块接口
 * 提供平台特定的依赖注入
 */
expect object PlatformModule {
    /**
     * 提供PreferencesManager实例
     */
    fun providePreferencesManager(): PreferencesManager
    
    /**
     * 提供DatabaseDriverFactory实例
     */
    fun provideDatabaseDriverFactory(): DatabaseDriverFactory
}