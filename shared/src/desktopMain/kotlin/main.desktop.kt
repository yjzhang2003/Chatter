package presentation

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

/**
 * Desktop平台的getPlatformName实现
 */
actual fun getPlatformName(): String = "Desktop"

/**
 * Desktop主视图组件
 */
@Composable fun MainView() = App()

/**
 * Desktop预览组件
 */
@Preview
@Composable
fun AppPreview() {
    App()
}