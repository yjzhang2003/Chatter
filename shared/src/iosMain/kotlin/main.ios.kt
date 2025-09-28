package presentation

import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS平台的getPlatformName实现
 */
actual fun getPlatformName(): String = "iOS"

/**
 * iOS主视图控制器
 */
fun MainViewController() = ComposeUIViewController { App() }