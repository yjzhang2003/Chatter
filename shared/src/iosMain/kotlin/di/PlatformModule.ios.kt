package di

import data.local.PreferencesManager
import data.local.PreferencesManagerImpl
import data.database.DatabaseDriverFactory

/**
 * iOS平台的依赖注入模块
 */
actual object PlatformModule {
    
    /**
     * 提供PreferencesManager实例
     */
    actual fun providePreferencesManager(): PreferencesManager {
        return PreferencesManagerImpl()
    }
    
    /**
     * 提供DatabaseDriverFactory实例
     */
    actual fun provideDatabaseDriverFactory(): DatabaseDriverFactory {
        return DatabaseDriverFactory()
    }
}