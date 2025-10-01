package presentation.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import domain.model.CustomAIModel
import kotlinx.coroutines.launch

/**
 * 自定义模型配置界面
 * 允许用户配置自定义AI模型的参数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomModelConfigScreen(
    viewModel: CustomModelConfigViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var showApiKey by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (uiState.isEditMode) "编辑自定义模型" else "自定义模型配置",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
        
        // 基本信息
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "基本信息",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 模型显示名称
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = viewModel::updateDisplayName,
                    label = { Text("模型显示名称") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("例如：GPT-4") },
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
                
                // API URL
                OutlinedTextField(
                    value = uiState.apiUrl,
                    onValueChange = viewModel::updateApiUrl,
                    label = { Text("API URL") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("例如：https://api.openai.com/v1/chat/completions") },
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
                
                // 模型名称
                OutlinedTextField(
                    value = uiState.modelName,
                    onValueChange = viewModel::updateModelName,
                    label = { Text("模型名称") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("例如：gpt-4") },
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }
        }
        
        // 请求格式
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "请求格式",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 请求格式选择
                CustomAIModel.RequestFormat.values().forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.requestFormat == format,
                            onClick = { viewModel.updateRequestFormat(format) }
                        )
                        Text(
                            text = when (format) {
                                CustomAIModel.RequestFormat.OPENAI_COMPATIBLE -> "OpenAI 兼容格式"
                                CustomAIModel.RequestFormat.GEMINI -> "Gemini 格式"
                                CustomAIModel.RequestFormat.CUSTOM -> "自定义格式"
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        
        // 高级设置
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "高级设置",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // API密钥要求
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.apiKeyRequired,
                        onCheckedChange = viewModel::updateApiKeyRequired
                    )
                    Text(
                        text = "需要API密钥",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                // API密钥输入（当需要API密钥时显示）
                if (uiState.apiKeyRequired) {
                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = viewModel::updateApiKey,
                        label = { Text("API密钥") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        placeholder = { Text("请输入API密钥") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (showApiKey) "隐藏密钥" else "显示密钥"
                                )
                            }
                        },
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )
                }
                
                // 多模态支持
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.supportsMultimodal,
                        onCheckedChange = viewModel::updateSupportsMultimodal
                    )
                    Text(
                        text = "支持图片输入",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                // 最大Token数
                OutlinedTextField(
                    value = uiState.maxTokens?.toString() ?: "",
                    onValueChange = { value ->
                        val tokens = value.toIntOrNull()
                        viewModel.updateMaxTokens(tokens)
                    },
                    label = { Text("最大Token数（可选）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("例如：4096") }
                )
                
                // 温度参数
                OutlinedTextField(
                    value = uiState.temperature?.toString() ?: "",
                    onValueChange = { value ->
                        val temp = value.toDoubleOrNull()
                        if (temp == null || (temp >= 0.0 && temp <= 2.0)) {
                            viewModel.updateTemperature(temp)
                        }
                    },
                    label = { Text("温度参数（0.0-2.0，可选）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("例如：0.7") }
                )
            }
        }
        
        // 错误信息
        if (uiState.errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                val errorMsg = uiState.errorMessage
                if (errorMsg != null) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        
        // 操作按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 取消按钮
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("取消")
            }
            
            // 保存按钮
            Button(
                onClick = {
                    scope.launch {
                        if (viewModel.saveConfiguration()) {
                            onNavigateBack()
                        }
                    }
                },
                enabled = !uiState.isLoading && uiState.isValid,
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (uiState.isEditMode) "更新" else "保存")
                }
            }
        }
        
        // 预设模板
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "快速配置模板",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // OpenAI 模板
                OutlinedButton(
                    onClick = { viewModel.loadOpenAITemplate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("使用 OpenAI 模板")
                }
                
                // Claude 模板
                OutlinedButton(
                    onClick = { viewModel.loadClaudeTemplate() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("使用 Claude 模板")
                }
            }
        }
        }
    }
}