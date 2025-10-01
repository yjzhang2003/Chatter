package presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 添加MCP服务对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMCPServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, endpoint: String, description: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var endpointError by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "添加 MCP 服务",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                // 服务名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = false
                    },
                    label = { Text("服务名称") },
                    placeholder = { Text("例如：文件系统服务") },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("请输入服务名称") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 服务端点
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { 
                        endpoint = it
                        endpointError = false
                    },
                    label = { Text("服务端点") },
                    placeholder = { Text("例如：http://localhost:3000/mcp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = endpointError,
                    supportingText = if (endpointError) {
                        { Text("请输入有效的服务端点URL") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 服务描述（可选）
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("服务描述（可选）") },
                    placeholder = { Text("描述此MCP服务的功能...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // 验证输入
                            nameError = name.isBlank()
                            endpointError = endpoint.isBlank() || !isValidUrl(endpoint)
                            
                            if (!nameError && !endpointError) {
                                onConfirm(name.trim(), endpoint.trim(), description.trim())
                            }
                        }
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}

/**
 * 简单的URL验证
 */
private fun isValidUrl(url: String): Boolean {
    return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ws://") || url.startsWith("wss://")
}