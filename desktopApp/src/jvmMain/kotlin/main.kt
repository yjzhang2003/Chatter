import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import presentation.MainView

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MainView()
    }
}