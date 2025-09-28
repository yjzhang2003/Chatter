package data.repository

import data.database.AgentDao
import domain.model.Agent
import domain.repository.AgentRepository

/**
 * 智能体仓库实现类
 * 实现智能体管理的具体业务逻辑
 */
class AgentRepositoryImpl(
    private val agentDao: AgentDao
) : AgentRepository {
    
    override suspend fun getAllAgents(): List<Agent> {
        return agentDao.getAllAgents()
    }
    
    override suspend fun getAgentById(id: String): Agent? {
        return agentDao.getAgentById(id)
    }
    
    override suspend fun createAgent(agent: Agent): Boolean {
        return agentDao.insertAgent(agent)
    }
    
    override suspend fun updateAgent(agent: Agent): Boolean {
        return agentDao.updateAgent(agent)
    }
    
    override suspend fun deleteAgent(id: String): Boolean {
        // 只允许删除自定义智能体
        val agent = agentDao.getAgentById(id)
        return if (agent?.isCustom() == true) {
            agentDao.deleteAgent(id)
        } else {
            false
        }
    }
    
    override suspend fun getPresetAgents(): List<Agent> {
        return agentDao.getPresetAgents()
    }
    
    override suspend fun getCustomAgents(): List<Agent> {
        return agentDao.getCustomAgents()
    }
    
    override suspend fun incrementAgentUsage(id: String): Boolean {
        return agentDao.incrementAgentUsage(id)
    }
    
    override suspend fun searchAgents(query: String): List<Agent> {
        return agentDao.searchAgents(query)
    }
    
    override suspend fun initializePresetAgents(): Boolean {
        return try {
            val existingPresets = getPresetAgents()
            val presetAgents = getDefaultPresetAgents()
            
            // 只添加不存在的预设智能体
            presetAgents.forEach { preset ->
                if (existingPresets.none { it.id == preset.id }) {
                    agentDao.insertAgent(preset)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取默认预设智能体列表
     */
    private fun getDefaultPresetAgents(): List<Agent> {
        return listOf(
            Agent.createPreset(
                id = "default_assistant",
                name = "通用助手",
                description = "友好、有帮助的AI助手，可以协助处理各种任务",
                systemPrompt = "你是一个友好、有帮助的AI助手。请以礼貌、专业的方式回答用户的问题，并尽力提供准确、有用的信息。",
                avatar = "🤖"
            ),
            Agent.createPreset(
                id = "translator",
                name = "翻译专家",
                description = "专业的多语言翻译助手，支持准确的语言转换",
                systemPrompt = "你是一个专业的翻译专家，精通多种语言。请准确、自然地翻译用户提供的文本，保持原文的语调和含义。如果用户没有指定目标语言，请询问需要翻译成哪种语言。",
                avatar = "🌐"
            ),
            Agent.createPreset(
                id = "programmer",
                name = "编程助手",
                description = "专业的编程和技术问题解答助手",
                systemPrompt = "你是一个经验丰富的程序员和技术专家。请帮助用户解决编程问题，提供清晰的代码示例，解释技术概念，并给出最佳实践建议。请确保代码的正确性和可读性。",
                avatar = "💻"
            ),
            Agent.createPreset(
                id = "writer",
                name = "创意写手",
                description = "专业的创意写作和文案助手",
                systemPrompt = "你是一个富有创意的写作专家。请帮助用户创作各种类型的文本，包括故事、文章、广告文案等。注重文字的美感、逻辑性和吸引力，根据用户需求调整写作风格。",
                avatar = "✍️"
            ),
            Agent.createPreset(
                id = "teacher",
                name = "学习导师",
                description = "耐心的教学助手，擅长解释复杂概念",
                systemPrompt = "你是一个耐心、专业的教师。请用简单易懂的方式解释复杂的概念，提供学习建议，帮助用户理解和掌握知识。根据用户的学习水平调整解释的深度和方式。",
                avatar = "👨‍🏫"
            ),
            Agent.createPreset(
                id = "analyst",
                name = "数据分析师",
                description = "专业的数据分析和商业洞察助手",
                systemPrompt = "你是一个专业的数据分析师。请帮助用户分析数据、解读趋势、提供商业洞察。用清晰的逻辑和数据支持你的分析结论，并提供可行的建议。",
                avatar = "📊"
            )
        )
    }
}