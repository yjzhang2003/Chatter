package presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.Agent

/**
 * 智能体编辑界面
 * 支持创建新智能体和编辑现有智能体
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentEditScreen(
    agent: Agent? = null,
    onBackClick: () -> Unit,
    onSaveClick: (name: String, description: String, systemPrompt: String, avatar: String) -> Unit
) {
    var name by remember { mutableStateOf(agent?.name ?: "") }
    var description by remember { mutableStateOf(agent?.description ?: "") }
    var systemPrompt by remember { mutableStateOf(agent?.systemPrompt ?: "") }
    var avatar by remember { mutableStateOf(agent?.avatar ?: "") }
    
    val isEditing = agent != null
    val canSave = name.isNotBlank() && systemPrompt.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditing) "编辑智能体" else "创建智能体",
                        fontWeight = FontWeight.Bold
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
                    IconButton(
                        onClick = { 
                            if (canSave) {
                                onSaveClick(name, description, systemPrompt, avatar)
                            }
                        },
                        enabled = canSave
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "保存"
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
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 名称输入
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("智能体名称") },
                placeholder = { Text("为你的智能体起个名字") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = name.isBlank()
            )
            
            // 描述输入
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                placeholder = { Text("简单描述这个智能体的功能和特点") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            // 头像输入（可选）
            OutlinedTextField(
                value = avatar,
                onValueChange = { avatar = it },
                label = { Text("头像 (可选)") },
                placeholder = { Text("输入emoji或头像URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 系统提示词输入
            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("系统提示词") },
                placeholder = { Text("定义智能体的角色、行为和回答风格...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                isError = systemPrompt.isBlank()
            )
            
            // 提示信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 提示",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• 系统提示词是智能体的核心，它定义了AI的角色和行为方式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• 尽量具体描述你希望智能体如何回答问题",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• 可以包含专业领域、语言风格、回答格式等要求",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 示例提示词
            if (!isEditing && systemPrompt.isBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📝 示例提示词",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        val examples = listOf(
                            "编程助手" to "你是一个专业的编程助手，擅长多种编程语言。请用简洁明了的方式回答技术问题，提供可运行的代码示例，并解释关键概念。",
                            "创意写手" to "你是一个富有创意的写作助手，擅长各种文体创作。请用生动有趣的语言风格，帮助用户进行创意写作、故事构思和文案创作。",
                            "学习导师" to "你是一个耐心的学习导师，善于用通俗易懂的方式解释复杂概念。请循序渐进地教学，多用比喻和实例，确保学生能够理解。"
                        )
                        
                        examples.forEach { (title, prompt) ->
                            TextButton(
                                onClick = { systemPrompt = prompt },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = prompt.take(50) + "...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 底部间距
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}