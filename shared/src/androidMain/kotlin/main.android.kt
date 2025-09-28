package presentation

import androidx.compose.runtime.Composable

/**
 * Android平台的getPlatformName实现
 */
actual fun getPlatformName(): String = "Android"

/**
 * Android主视图组件
 */
@Composable fun MainView() = App()
