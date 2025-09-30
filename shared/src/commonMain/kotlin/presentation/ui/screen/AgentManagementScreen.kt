package presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import domain.model.Agent

/**
 * 智能体管理页面
 * 提供智能体的创建、编辑、删除和查看功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentManagementScreen(
    agents: List<Agent>,
    onCreateAgentClick: () -> Unit,
    onEditAgentClick: (Agent) -> Unit,
    onDeleteAgentClick: (Agent) -> Unit
) {
    // 删除确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    var agentToDelete by remember { mutableStateOf<Agent?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { 
                Text(
                    text = "智能体管理",
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = onCreateAgentClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "创建智能体"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // 内容区域
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 预设智能体分组
            val presetAgents = agents.filter { it.isPreset }
            if (presetAgents.isNotEmpty()) {
                item {
                    Text(
                        text = "预设智能体",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(presetAgents) { agent ->
                    AgentManagementCard(
                        agent = agent,
                        onEditClick = { onEditAgentClick(agent) },
                        onDeleteClick = { 
                            agentToDelete = agent
                            showDeleteDialog = true
                        }
                    )
                }
            }

            // 自定义智能体分组
            val customAgents = agents.filter { !it.isPreset }
            if (customAgents.isNotEmpty()) {
                item {
                    Text(
                        text = "自定义智能体",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(customAgents) { agent ->
                    AgentManagementCard(
                        agent = agent,
                        onEditClick = { onEditAgentClick(agent) },
                        onDeleteClick = { 
                            agentToDelete = agent
                            showDeleteDialog = true
                        }
                    )
                }
            }

            // 空状态
            if (agents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "暂无智能体",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "点击右上角 + 创建你的第一个智能体",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog && agentToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                agentToDelete = null
            },
            title = {
                Text("删除智能体")
            },
            text = {
                Text("确定要删除智能体 \"${agentToDelete!!.name}\" 吗？此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        agentToDelete?.let { agent ->
                            onDeleteAgentClick(agent)
                        }
                        showDeleteDialog = false
                        agentToDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        agentToDelete = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 智能体管理卡片组件
 * 显示智能体的基本信息和管理操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentManagementCard(
    agent: Agent,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = agent.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (agent.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = agent.getShortDescription(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // 显示智能体类型
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (agent.isPreset) "预设智能体" else "自定义智能体",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (agent.isPreset) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }
                
                // 编辑和删除按钮
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑智能体",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // 仅对自定义智能体显示删除按钮
                    if (agent.isCustom()) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除智能体",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}