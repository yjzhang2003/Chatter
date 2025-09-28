# 🤖 Chatter - 多平台智能聊天应用

## 💡 项目简介

一个基于 Compose Multiplatform 构建的跨平台智能聊天应用，支持多种 AI 模型，包括 Gemini Pro、Kimi、豆包和自定义模型。提供强大的多模态对话能力和丰富的自定义选项。

## 📱 支持平台

本项目支持以下平台：
* **Android** - 完整功能支持
* **iOS** - 完整功能支持  
* **Desktop** - Linux、macOS、Windows 桌面版

> 注：浏览器支持正在开发中，将在未来版本中添加。

## ✨ 功能特性

### 💬 智能对话管理
- **对话列表**: 统一管理所有AI对话，支持按时间排序
- **对话创建**: 快速创建新对话，自动生成智能标题
- **对话搜索**: 支持按标题和消息内容搜索对话
- **消息历史**: 完整保存对话历史，支持上下文连续对话
- **对话统计**: 实时显示消息数量和对话统计信息
- **数据持久化**: 本地SQLite数据库安全存储所有对话数据

### 🤖 多AI模型支持
- **OpenAI GPT系列**: GPT-3.5-turbo, GPT-4, GPT-4-turbo
- **Google Gemini**: Gemini-pro, Gemini-pro-vision
- **Anthropic Claude**: Claude-3-haiku, Claude-3-sonnet, Claude-3-opus
- **月之暗面 Kimi**: Moonshot-v1-8k, Moonshot-v1-32k, Moonshot-v1-128k
- **自定义API**: 支持OpenAI兼容的API接口

### 🎨 现代化界面设计
- **Material Design 3**: 遵循最新设计规范
- **深色/浅色主题**: 自动适配系统主题
- **响应式布局**: 适配不同屏幕尺寸
- **流畅动画**: 提供优雅的交互体验

### 🖼️ 多模态支持
- **图片上传**: 支持多种图片格式
- **图片预览**: 内置图片查看器
- **多模态对话**: 结合文本和图片进行AI对话

### ⚙️ 高级配置
- **模型参数调节**: Temperature、Max Tokens等
- **API密钥管理**: 安全的密钥存储
- **对话历史**: 本地数据持久化
- **导入导出**: 支持对话数据备份

## 📱 应用界面

### 对话管理
- **对话列表页面**: 展示所有对话，支持搜索和快速创建
- **对话详情页面**: 完整的聊天界面，支持文本和图片输入
- **消息气泡**: 区分用户和AI消息，支持Markdown渲染
- **加载状态**: 实时显示AI响应生成状态

### 核心功能演示
1. **创建对话**: 点击"+"按钮快速创建新对话
2. **发送消息**: 支持纯文本或文本+图片的多模态输入
3. **查看历史**: 所有对话历史自动保存，支持随时查看
4. **搜索对话**: 在对话列表中快速搜索特定内容
5. **管理对话**: 支持删除不需要的对话

## 🏗️ 技术架构

### 核心技术栈
- **Kotlin Multiplatform**: 跨平台共享业务逻辑
- **Compose Multiplatform**: 现代化声明式UI框架
- **SQLDelight**: 类型安全的SQL数据库操作
- **Ktor**: 网络请求和API调用
- **Kotlinx.Serialization**: JSON序列化和反序列化
- **Kotlinx.Coroutines**: 异步编程和并发处理

### 架构设计
- **MVVM模式**: 清晰的数据流和状态管理
- **Repository模式**: 统一的数据访问层
- **依赖注入**: 松耦合的组件设计
- **响应式编程**: Flow和StateFlow状态管理

### 数据存储
- **本地数据库**: SQLite存储对话和消息数据
- **安全存储**: 加密保存API密钥等敏感信息
- **数据同步**: 支持数据导入导出功能

## 🚀 快速开始

### 前置要求

1. **获取 API 密钥**：
   - Gemini Pro: https://ai.google.dev
   - Kimi: https://platform.moonshot.cn
   - 豆包: https://www.volcengine.com/product/doubao
   - 自定义模型: 根据具体服务商获取

2. **开发环境**：
   - Android Studio 或 IntelliJ IDEA
   - JDK 17+
   - Kotlin 1.9.21+

### 配置步骤

1. **克隆项目**：
```bash
git clone https://github.com/yjzhang2003/Chatter.git
cd Chatter
```

2. **配置 API 密钥**：
   - 启动应用后，进入设置页面
   - 在"API 管理"中配置各模型的密钥
   - 或在代码中直接设置（不推荐用于生产环境）

3. **运行项目**：
```bash
# Android
./gradlew :androidApp:installDebug

# Desktop
./gradlew :desktopApp:run

# iOS (需要 Xcode)
open iosApp/iosApp.xcodeproj
```

## 🔧 自定义模型配置

应用支持添加任何 OpenAI 兼容的 API 服务：

1. 进入"设置" → "自定义模型配置"
2. 点击"添加新模型"
3. 填写以下信息：
   - 模型名称和描述
   - API 端点 URL
   - 模型标识符
   - 请求格式（OpenAI/Anthropic/自定义）
   - 自定义请求头（可选）
   - 模型参数（温度、最大 token 等）

### 支持的 API 格式

- **OpenAI 格式**: 兼容 OpenAI ChatGPT API
- **Anthropic 格式**: 兼容 Claude API  
- **自定义格式**: 支持其他厂商的 API 格式

## 📋 版本历史

### v1.4.0 (即将发布)
- ✅ 新增多模型支持架构
- ✅ 添加自定义模型配置功能
- ✅ 实现 API 管理界面
- ✅ 优化 JSON 解析和错误处理
- ✅ 改进用户界面和交互体验

### v1.3.0 (当前版本)
- 基础 Gemini Pro 集成
- 多模态对话支持
- 主题切换功能
- 基本设置界面

## 🛠 技术栈

- **框架**: Compose Multiplatform
- **语言**: Kotlin
- **网络**: Ktor Client
- **序列化**: Kotlinx Serialization
- **状态管理**: MVVM + Compose State
- **图片处理**: Kamel Image Loading
- **Markdown**: Multiplatform Markdown Renderer

## 🤝 贡献指南

我们欢迎任何形式的贡献！

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

### 开发规范

- 遵循 Kotlin 编码规范
- 添加适当的注释和文档
- 确保跨平台兼容性
- 编写单元测试（推荐）

## 📄 许可证

```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 💙 支持项目

如果这个项目对你有帮助，请给我们一个 ⭐️ 

---

**联系方式**: 如有问题或建议，请通过 Issues 或 Discussions 与我们联系。

