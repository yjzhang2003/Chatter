package presentation.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.model.AIModel
import presentation.theme.Gray700

/**
 * API管理屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiManagementScreen(
    viewModel: ApiManagementViewModel = ApiManagementViewModel(),
    onBackClick: () -> Unit = {},
    onCustomModelConfigClick: () -> Unit = {},
    onAddCustomModelClick: () -> Unit = {},
    onEditCustomModel: (domain.model.CustomAIModel) -> Unit = {},
    onDeleteCustomModel: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "API管理", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddCustomModelClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加自定义模型"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 当前模型显示
            item {
                Text(
                    text = "当前模型",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            item {
                CurrentModelCard(
                    currentModel = uiState.currentModel,
                    customModel = uiState.activeCustomModel,
                    onModelSelect = { model -> viewModel.selectModel(model) }
                )
            }
            
            // 自定义模型列表
            if (uiState.customModels.isNotEmpty()) {
                item {
                    Text(
                        text = "自定义模型",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                
                items(uiState.customModels) { customModel ->
                    CustomModelItemCard(
                        customModel = customModel,
                        isSelected = uiState.currentModel == AIModel.CUSTOM && 
                                   uiState.activeCustomModel?.id == customModel.id,
                        isActive = uiState.activeCustomModel?.id == customModel.id,
                        onModelSelect = { 
                            viewModel.setActiveCustomModel(customModel.id)
                            viewModel.selectModel(AIModel.CUSTOM)
                        },
                        onEditClick = { onEditCustomModel(customModel) },
                        onDeleteClick = { onDeleteCustomModel(customModel.id) }
                    )
                }
            }
            
            // 预定义模型配置
            item {
                Text(
                    text = "预定义模型",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            
            items(AIModel.values().filter { it != AIModel.CUSTOM }) { model ->
                ModelConfigCard(
                    model = model,
                    apiKey = uiState.apiKeys[model] ?: "",
                    isSelected = uiState.currentModel == model,
                    onApiKeyChange = { newKey -> viewModel.updateApiKey(model, newKey) },
                    onModelSelect = { viewModel.selectModel(model) }
                )
            }
        }
    }
}

/**
 * 自定义模型卡片（旧版本，保留兼容性）
 */
@Composable
private fun CustomModelCard(
    isSelected: Boolean,
    onConfigureClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigureClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            ) 
        else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自定义模型",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "配置您自己的AI模型",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray700,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * 当前模型卡片
 */
@Composable
private fun CurrentModelCard(
    currentModel: AIModel,
    customModel: domain.model.CustomAIModel? = null,
    onModelSelect: (AIModel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "当前使用模型",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (currentModel == AIModel.CUSTOM && customModel != null) {
                    customModel.displayName
                } else {
                    currentModel.displayName
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (currentModel == AIModel.CUSTOM && customModel != null) {
                Text(
                    text = customModel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * 模型配置卡片
 */
@Composable
private fun ModelConfigCard(
    model: AIModel,
    apiKey: String,
    isSelected: Boolean,
    customModel: domain.model.CustomAIModel? = null,
    onApiKeyChange: (String) -> Unit,
    onModelSelect: () -> Unit,
    onEditCustomModel: (() -> Unit)? = null
) {
    var showApiKey by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            ) 
        else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (model == AIModel.CUSTOM && customModel != null) {
                            customModel.displayName
                        } else {
                            model.displayName
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = if (model == AIModel.CUSTOM && customModel != null) {
                            customModel.description
                        } else {
                            model.description
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray700,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选择",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    if (model == AIModel.CUSTOM && onEditCustomModel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onEditCustomModel,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑自定义模型",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // 展开的API密钥配置
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // API密钥输入
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("API密钥") },
                    placeholder = { Text("请输入${model.displayName}的API密钥") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showApiKey) "隐藏密钥" else "显示密钥"
                            )
                        }
                    },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 选择按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!isSelected) {
                        Button(
                            onClick = onModelSelect,
                            enabled = apiKey.isNotEmpty()
                        ) {
                            Text("选择此模型")
                        }
                    } else {
                        Text(
                            text = "当前使用中",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 自定义模型项卡片
 */
@Composable
private fun CustomModelItemCard(
    customModel: domain.model.CustomAIModel,
    isSelected: Boolean,
    isActive: Boolean,
    onModelSelect: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onModelSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.secondaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            ) 
        else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = customModel.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "活跃",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = customModel.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray700,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "端点: ${customModel.apiUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray700,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选择",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑模型",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除模型",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}