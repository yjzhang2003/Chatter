package di

import data.local.PreferencesManager

/**
 * 平台模块接口
 * 提供平台特定的依赖注入
 */
expect object PlatformModule {
    /**
     * 提供PreferencesManager实例
     */
    fun providePreferencesManager(): PreferencesManager
}