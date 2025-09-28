package di

import android.content.Context
import data.local.PreferencesManager
import data.local.PreferencesManagerImpl
import data.database.DatabaseDriverFactory

/**
 * Android平台的依赖注入模块
 */
actual object PlatformModule {
    
    private var applicationContext: Context? = null
    
    /**
     * 初始化平台模块
     * 需要在Application中调用
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
    
    /**
     * 提供PreferencesManager实例
     */
    actual fun providePreferencesManager(): PreferencesManager {
        val context = applicationContext 
            ?: throw IllegalStateException("PlatformModule未初始化，请在Application中调用initialize()")
        return PreferencesManagerImpl(context)
    }
    
    /**
     * 提供DatabaseDriverFactory实例
     */
    actual fun provideDatabaseDriverFactory(): DatabaseDriverFactory {
        val context = applicationContext 
            ?: throw IllegalStateException("PlatformModule未初始化，请在Application中调用initialize()")
        return DatabaseDriverFactory(context)
    }
}