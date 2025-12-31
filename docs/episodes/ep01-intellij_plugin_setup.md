# EP01 - IntelliJ Plugin开发环境搭建 / IntelliJ Plugin Development Setup

> **项目仓库 / Project Repository:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)  
> **当前进度 / Current Progress:** Phase 1 ✅ Completed

---

## 📋 概述 / Overview

### 🇨🇳 中文
欢迎来到MetaMask Clone开发系列！本集我们将从零开始搭建IntelliJ插件开发环境，并使用AI辅助开发完整的钱包管理功能。与传统教程不同，我们直接实现创建钱包、导入私钥、导出私钥等核心功能，为后续区块链交互打下基础。

### 🇺🇸 English  
Welcome to the MetaMask Clone development series! In this episode, we'll build an IntelliJ plugin development environment from scratch and use AI assistance to develop complete wallet management functionality. Unlike traditional tutorials, we directly implement core features like wallet creation, private key import/export, laying the foundation for future blockchain interactions.

---

## 🎯 系列介绍 / Series Introduction

### 🇨🇳 我们要构建什么？
一个功能完整的区块链钱包，就像MetaMask一样，但运行在IntelliJ IDEA中：

- 🏗️ **多钱包管理**：创建、导入、重命名、导出钱包
- 🔐 **安全存储**：项目级加密数据持久化
- 🌐 **多链支持**：ETH、BNB、TRON、BTC
- 💰 **代币管理**：查询余额、添加ERC20代币
- 💸 **交易功能**：发送代币、查看历史
- 🔗 **DApp交互**：连接去中心化应用

### 🇺🇸 What Are We Building?
A fully functional blockchain wallet, like MetaMask, but running inside IntelliJ IDEA:

- 🏗️ **Multi-Wallet Management**: Create, import, rename, export wallets
- 🔐 **Secure Storage**: Project-level encrypted data persistence
- 🌐 **Multi-Chain Support**: ETH, BNB, TRON, BTC
- 💰 **Token Management**: Balance queries, add ERC20 tokens
- 💸 **Transaction Features**: Send tokens, view history
- 🔗 **DApp Interaction**: Connect to decentralized applications

---

## 🎯 学习目标 / Learning Objectives

### 🇨🇳 本集目标
- [ ] 搭建IntelliJ插件开发环境
- [ ] 理解AI协作编程的正确方式
- [ ] 实现Tool Window界面
- [ ] 完成完整的钱包管理功能
- [ ] 掌握AES-256私钥加密存储
- [ ] 实现项目级数据持久化

### 🇺🇸 Episode Goals
- [ ] Set up IntelliJ plugin development environment
- [ ] Understand the right way of AI-assisted programming
- [ ] Implement Tool Window interface
- [ ] Complete full wallet management functionality
- [ ] Master AES-256 private key encryption storage
- [ ] Implement project-level data persistence

---

## 🛠️ 技术栈 / Technology Stack

### 核心技术选择 / Core Technology Choices

| 组件 / Component | 选择 / Choice | 🇨🇳 原因 | 🇺🇸 Reason |
|------------------|---------------|---------|-----------|
| **语言 / Language** | Kotlin | JVM生态，类型安全 | JVM ecosystem, type safety |
| **平台 / Platform** | IntelliJ Plugin | 开发者熟悉环境 | Familiar developer environment |
| **UI** | Swing | 原生IntelliJ集成 | Native IntelliJ integration |
| **区块链 / Blockchain** | Web3j | 成熟的Java EVM库 | Mature Java EVM library |
| **加密 / Encryption** | BouncyCastle | 企业级加密标准 | Enterprise encryption standard |

---

## 🤖 AI协作开发 / AI-Assisted Development

### 🇨🇳 AI协作的正确姿势
本系列的核心理念：**AI辅助，人工调优**

### 🇺🇸 The Right Way of AI Collaboration
Core philosophy of this series: **AI-assisted, human-optimized**

| 阶段 / Phase | 🇨🇳 人的职责 | 🇺🇸 Human Role | 🇨🇳 AI的作用 | 🇺🇸 AI's Role |
|--------------|-------------|----------------|-------------|---------------|
| **需求分析 / Requirements** | 明确功能和约束 | Define requirements | 提供技术建议 | Technical suggestions |
| **代码实现 / Implementation** | 架构设计和审查 | Architecture & review | 快速生成代码 | Rapid code generation |
| **调试优化 / Debug & Optimize** | 问题定位和优化 | Debug & optimize | 错误分析 | Error analysis |

### 有效Prompt示例 / Effective Prompt Examples

#### 🇨🇳 好的Prompt
```
"帮我创建IntelliJ插件的Tool Window，要求：
1. 位于右侧面板，支持钱包列表显示
2. 包含创建、导入、导出钱包按钮
3. 使用Kotlin + Swing，项目级数据持久化
4. 私钥需要AES-256加密存储"
```

#### 🇺🇸 Good Prompt
```
"Help me create IntelliJ plugin Tool Window with:
1. Right panel with wallet list display
2. Create, import, export wallet buttons
3. Kotlin + Swing, project-level persistence
4. AES-256 encrypted private key storage"
```

---

## 🚀 实战开发 / Implementation

### 第一步：环境搭建 / Step 1: Environment Setup

#### 🇨🇳 系统要求
- IntelliJ IDEA 2025.1+
- JDK 21
- 8GB+ RAM

#### 🇺🇸 System Requirements
- IntelliJ IDEA 2025.1+
- JDK 21
- 8GB+ RAM

### 第二步：项目创建 / Step 2: Project Creation

```kotlin
// 🇨🇳 项目配置 | 🇺🇸 Project Configuration
Project Name: metamask-clone
Package: dev.eastgate.metamaskclone
Language: Kotlin
Build: Gradle (Kotlin DSL)
```

### 第三步：核心功能实现 / Step 3: Core Features

#### 钱包管理功能 / Wallet Management Features

```kotlin
// 🇨🇳 主要功能组件 | 🇺🇸 Main functional components
class MetaMaskToolWindow {
    // ✅ 创建新钱包 | Create new wallet
    // ✅ 导入现有钱包 | Import existing wallet  
    // ✅ 导出私钥 | Export private key
    // ✅ 钱包列表显示 | Wallet list display
    // ✅ 安全加密存储 | Secure encrypted storage
}
```

### 第四步：安全存储 / Step 4: Secure Storage

#### 🇨🇳 加密机制
- **算法**：AES-256-GCM
- **密钥派生**：PBKDF2 + 项目ID
- **数据隔离**：项目级存储

#### 🇺🇸 Encryption Mechanism
- **Algorithm**: AES-256-GCM
- **Key Derivation**: PBKDF2 + Project ID
- **Data Isolation**: Project-level storage

---

## ✅ 完成功能检查 / Completed Features Checklist

### 🇨🇳 Phase 1 功能确认
- [ ] ✅ IntelliJ插件环境搭建完成
- [ ] ✅ Tool Window在右侧面板正常显示
- [ ] ✅ 创建钱包功能（生成地址和私钥）
- [ ] ✅ 导入钱包功能（通过私钥）
- [ ] ✅ 导出私钥功能（密码保护）
- [ ] ✅ 钱包重命名功能
- [ ] ✅ AES-256加密存储私钥
- [ ] ✅ 项目级数据持久化
- [ ] ✅ IDE重启后数据保持

### 🇺🇸 Phase 1 Feature Verification
- [ ] ✅ IntelliJ plugin environment setup completed
- [ ] ✅ Tool Window displays properly in right panel
- [ ] ✅ Create wallet functionality (generate address and private key)
- [ ] ✅ Import wallet functionality (via private key)
- [ ] ✅ Export private key functionality (password protected)
- [ ] ✅ Wallet renaming functionality
- [ ] ✅ AES-256 encrypted private key storage
- [ ] ✅ Project-level data persistence
- [ ] ✅ Data persists after IDE restart

---

## 🎯 关键成就 / Key Achievements

### 🇨🇳 我们完成了什么？
- 🏗️ **完整的钱包管理系统**：不是Hello World，是真正可用的工具
- 🔒 **企业级安全标准**：AES-256加密确保私钥安全
- 🎨 **专业UI集成**：完美融入IntelliJ开发环境
- 🤖 **AI协作实践**：展示了正确的AI辅助开发流程

### 🇺🇸 What Did We Accomplish?
- 🏗️ **Complete Wallet Management System**: Not Hello World, but truly usable tool
- 🔒 **Enterprise-level Security**: AES-256 encryption ensures private key safety
- 🎨 **Professional UI Integration**: Perfect integration into IntelliJ environment
- 🤖 **AI Collaboration Practice**: Demonstrated proper AI-assisted development workflow

---

## 🚀 下一集预告 / Next Episode Preview

### 🇨🇳 EP02: BSC Testnet实战
现在我们有了钱包，但还没有"币"可以操作。下一集我们将：

- 🌐 **配置BSC测试网**：为什么选择BSC作为学习起点
- 💰 **获取测试币**：免费领取tBNB测试币
- 🪙 **创建ERC20代币**：使用Remix IDE部署智能合约
- 📚 **理解ERC20标准**：深入学习代币标准
- 🛠️ **OpenZeppelin实践**：智能合约最佳实践

### 🇺🇸 EP02: BSC Testnet Practice
Now we have a wallet, but no "coins" to operate with. In the next episode:

- 🌐 **Configure BSC Testnet**: Why BSC is chosen as learning starting point
- 💰 **Get Test Coins**: Free tBNB testnet coins
- 🪙 **Create ERC20 Token**: Deploy smart contract using Remix IDE
- 📚 **Understand ERC20 Standard**: Deep dive into token standards
- 🛠️ **OpenZeppelin Practice**: Smart contract best practices

---

## 📝 重要信息 / Important Information

### 🇨🇳 保存项目信息
```
项目仓库: https://github.com/eastgatedev/metamask-clone
本地路径: 你的项目目录
钱包数据: 已保存在项目配置中
```

### 🇺🇸 Save Project Information
```
Project Repository: https://github.com/eastgatedev/metamask-clone
Local Path: Your project directory
Wallet Data: Saved in project configuration
```

---

## 🔗 相关资源 / Related Resources

### 开发工具 / Development Tools
- [GitHub Repository](https://github.com/eastgatedev/metamask-clone)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- [Web3j Documentation](https://docs.web3j.io/)

### 🇨🇳 中文文档
- [Kotlin官方文档](https://kotlinlang.org/docs/)
- [IntelliJ插件开发指南](https://plugins.jetbrains.com/docs/intellij/)

### 🇺🇸 English Documentation  
- [Kotlin Official Docs](https://kotlinlang.org/docs/)
- [IntelliJ Plugin Development Guide](https://plugins.jetbrains.com/docs/intellij/)

---

## 📊 项目进度 / Project Progress

```
Phase 1: Basic Wallet Management           ✅ COMPLETED
├── Create Wallet                           ✅
├── Import Private Key                      ✅ 
├── Export Private Key                      ✅
├── Secure Storage                          ✅
└── Project Data Persistence               ✅

Phase 2: Blockchain Preparation             🚧 NEXT (EP02)
├── BSC Testnet Setup                       ⏳
├── Get Test Coins                          ⏳
├── Create ERC20 Token                      ⏳
└── Understand Token Standards              ⏳

Phase 3: Blockchain Interaction             📋 PLANNED
Phase 4: Advanced Features                 📋 PLANNED
```

---

**🎥 YouTube Series:** [AI + IntelliJ Plugin + Web3 (MetaMask Clone)](https://youtube.com/playlist?list=PLbqZIOzRvr8mrKmli_WOVogsUTawr9dML&si=6c4ZE7E6WmiuzqaR)
**👨‍💻 Author:** 东门Eastgate  
**📅 Last Updated:** 2025-12-15  
**⭐ Star the Repo:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)
