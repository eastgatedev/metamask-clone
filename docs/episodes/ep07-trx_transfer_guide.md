# EP07 - TRON Runtime：TRX转账 / TRON Runtime: TRX Transfer

> **项目仓库 / Project Repository:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)  
> **当前进度 / Current Progress:** Phase 4 🚧 In Progress

---

## 📋 概述 / Overview

### 🇨🇳 中文
这是TRON系列里真正开始"跑起来"的一集。在EP06我们完成了TRON钱包的结构层（私钥、地址、编码），本集专注于**运行期**：当钱包已存在后，它会发生什么？我们从Wallet System / Backend角度看TRX余额查询和转账，以及为什么结果有时候并不如直觉。最重要的是，我们会明确**Wallet Backend的职责边界**。

### 🇺🇸 English  
This is the episode where TRON really "runs" in the TRON series. In EP06 we completed TRON wallet's structural layer (private key, address, encoding). This episode focuses on **runtime**: what happens after wallet exists? From Wallet System / Backend perspective, we look at TRX balance queries and transfers, and why results sometimes aren't intuitive. Most importantly, we'll clarify **Wallet Backend's responsibility boundaries**.

---

## 🎯 学习目标 / Learning Objectives

### 🇨🇳 本集目标
- [ ] 理解从结构层到运行期的转变
- [ ] 掌握TRX余额查询方法
- [ ] 实现TRX转账功能
- [ ] 理解运行期资源概念（bandwidth、energy）
- [ ] 明确Wallet Backend的职责边界
- [ ] 理解基础设施依赖的现实
- [ ] 掌握测试环境的准备

### 🇺🇸 Episode Goals
- [ ] Understand transition from structural layer to runtime
- [ ] Master TRX balance query methods
- [ ] Implement TRX transfer functionality
- [ ] Understand runtime resource concepts (bandwidth, energy)
- [ ] Clarify Wallet Backend's responsibility boundaries
- [ ] Understand reality of infrastructure dependencies
- [ ] Master test environment preparation

---

## 🎬 本集定位 / Episode Positioning

### 只关注一件事 / EP07 Focuses on One Thing

```
🇨🇳 当一个TRON钱包已经存在之后，
它在"运行期"会发生什么？

🇺🇸 When a TRON wallet already exists,
what happens during "runtime"?
```

### 本集范围 / Episode Scope

```
✅ 查询TRX balance
✅ 发送TRX
✅ 理解运行期行为
❌ 不讲TRC20（EP08）
```

---

## 🧪 测试环境准备 / Test Environment Preparation

### 🇨🇳 Faucet的系统定位
从Wallet Backend角度看：

### 🇺🇸 Faucet's System Positioning
From Wallet Backend perspective:

```
🇨🇳 Faucet不是给用户用的，
Faucet是backend的测试依赖。

🇺🇸 Faucet is not for users,
Faucet is backend's test dependency.
```

### 为什么需要Faucet / Why Faucet is Needed

| 原因 / Reason | 🇨🇳 说明 | 🇺🇸 Description |
|--------------|---------|-----------------|
| **测试用例 / Test Case** | 写稳定的测试用例 | Write stable test cases |
| **行为验证 / Behavior Verification** | 验证wallet system一致性 | Verify wallet system consistency |
| **开发测试 / Development Testing** | 无需真实资产 | No need for real assets |

### 获取测试TRX / Get Test TRX

**TRON Testnet Faucet:**
- **Shasta Testnet Faucet**: https://developers.tron.network/docs/getting-testnet-tokens-on-tron

### 测试环境假设 / Test Environment Assumption

```
🇨🇳 本集假设：
测试账户已通过faucet获得TRX
系统进入运行期测试阶段

🇺🇸 This episode assumes:
Test account obtained TRX through faucet
System enters runtime testing phase
```

---

## 💰 查询TRX Balance / Query TRX Balance

### 🇨🇳 最稳定的操作
查询TRX balance是最直觉的一步：

### 🇺🇸 Most Stable Operation
Querying TRX balance is the most intuitive step:

### 操作特性 / Operation Characteristics

| 特性 / Characteristic | 🇨🇳 说明 | 🇺🇸 Description |
|---------------------|---------|-----------------|
| **只读操作 / Read-only Operation** | 不改变链上状态 | Doesn't change on-chain state |
| **不需签名 / No Signature Required** | 无需私钥参与 | No private key required |
| **结果确定 / Deterministic Result** | 账户存在即可查询 | Queryable if account exists |
| **建立基线 / Establish Baseline** | 验证"正常状态" | Verify "normal state" |

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 查询TRX余额 | 🇺🇸 Query TRX Balance
suspend fun getTRXBalance(address: String): BigInteger {
    val apiWrapper = TronApiWrapper()
    
    // 🇨🇳 调用TRON API | 🇺🇸 Call TRON API
    val account = apiWrapper.getAccount(address)
    
    // 🇨🇳 返回余额（单位：sun，1 TRX = 10^6 sun）
    // 🇺🇸 Return balance (unit: sun, 1 TRX = 10^6 sun)
    return account.balance.toBigInteger()
}

// 🇨🇳 格式化显示 | 🇺🇸 Format for display
fun formatTRXBalance(balanceInSun: BigInteger): String {
    val balanceInTRX = balanceInSun.toBigDecimal()
        .divide(BigDecimal.valueOf(1_000_000))
    return "$balanceInTRX TRX"
}
```

### 目的 / Purpose

```
🇨🇳 建立一个"看起来一切都正常"的基线状态

🇺🇸 Establish a baseline state where "everything looks normal"
```

---

## 💸 发送TRX / Send TRX

### 🇨🇳 核心运行期操作
发送TRX是本集的核心内容：

### 🇺🇸 Core Runtime Operation
Sending TRX is the core content of this episode:

### 流程 / Flow

```
🇨🇳 Wallet Backend流程：
1. 构建交易
2. 使用私钥签名
3. 广播交易到网络

🇺🇸 Wallet Backend Flow:
1. Build transaction
2. Sign with private key
3. Broadcast transaction to network
```

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 发送TRX | 🇺🇸 Send TRX
suspend fun sendTRX(
    fromAddress: String,
    privateKey: String,
    toAddress: String,
    amountInSun: Long
): String {
    val apiWrapper = TronApiWrapper()
    
    // 🇨🇳 1. 创建交易 | 🇺🇸 1. Create transaction
    val transaction = apiWrapper.createTransaction(
        fromAddress = fromAddress,
        toAddress = toAddress,
        amount = amountInSun
    )
    
    // 🇨🇳 2. 签名交易 | 🇺🇸 2. Sign transaction
    val signedTransaction = signTransaction(transaction, privateKey)
    
    // 🇨🇳 3. 广播交易 | 🇺🇸 3. Broadcast transaction
    val result = apiWrapper.broadcastTransaction(signedTransaction)
    
    // 🇨🇳 4. 返回交易哈希 | 🇺🇸 4. Return transaction hash
    return result.txid
}
```

---

## ⚡ 运行期资源（概念说明）/ Runtime Resources (Conceptual)

### TRON运行期资源 / TRON Runtime Resources

| 资源 / Resource | 🇨🇳 用途 | 🇺🇸 Purpose | 🇨🇳 说明 | 🇺🇸 Note |
|----------------|---------|-------------|---------|----------|
| **Bandwidth** | 交易广播 | Transaction broadcast | 用于普通转账 | For regular transfers |
| **Energy** | 智能合约执行 | Smart contract execution | 用于合约调用 | For contract calls |

### 重要声明 / Important Declaration

```
🇨🇳 这些资源属于链的复杂性，不是钱包系统的职责

在我们的wallet system里：
- 我们不会管理这些资源
- 不会为它们做优化
- 也不会在UI中暴露这些细节

我们的选择是：
把TRX当作统一的执行支付单位

你只需要知道：
- 转账是执行
- 执行有成本
- 成本并不总是直觉可见的

到这里就够了。

🇺🇸 These resources belong to chain complexity, not wallet system's responsibility

In our wallet system:
- We don't manage these resources
- Don't optimize for them
- Don't expose these details in UI

Our choice is:
Treat TRX as unified execution payment unit

You only need to know:
- Transfer is execution
- Execution has cost
- Cost isn't always intuitively visible

This is enough.
```

---

## 🎯 Wallet Backend的职责边界 / Wallet Backend's Responsibility Boundaries

### 🇨🇳 最重要的部分
这是本集最核心的内容：

### 🇺🇸 Most Important Part
This is the core content of this episode:

### 应该做的事 / Should Do

| 职责 / Responsibility | 🇨🇳 说明 | 🇺🇸 Description |
|---------------------|---------|-----------------|
| **查询Balance / Query Balance** | 提供余额查询接口 | Provide balance query interface |
| **构建并发送交易 / Build and Send Transaction** | 完成交易构建和广播 | Complete transaction build and broadcast |
| **返回结果 / Return Result** | 清楚地返回成功或失败 | Clearly return success or failure |
| **暴露错误原因 / Expose Error Reason** | 如实报告错误信息 | Truthfully report error information |

### 不应该做的事 / Should NOT Do

| 边界 / Boundary | 🇨🇳 说明 | 🇺🇸 Description |
|----------------|---------|-----------------|
| **管理链上资源 / Manage On-chain Resources** | 不管理bandwidth/energy | Don't manage bandwidth/energy |
| **保证交易成功 / Guarantee Transaction Success** | 不能保证执行一定成功 | Can't guarantee execution success |
| **为失败兜底 / Cover for Failures** | 不隐藏或掩盖失败原因 | Don't hide or cover failure reasons |
| **优化资源 / Optimize Resources** | 不做链级资源优化 | Don't do chain-level resource optimization |

### 核心原则 / Core Principle

```
🇨🇳 Wallet backend提供的是执行入口，
而不是执行保证。

🇺🇸 Wallet backend provides execution entry,
not execution guarantee.
```

---

## 🌐 基础设施依赖 / Infrastructure Dependencies

### 🇨🇳 现实环境
在现实世界中，wallet system不可能自己跑全节点：

### 🇺🇸 Reality Environment
In real world, wallet system can't run full node itself:

### 依赖的基础设施 / Dependent Infrastructure

| 服务 / Service | 🇨🇳 说明 | 🇺🇸 Description |
|---------------|---------|-----------------|
| **公共节点 / Public Nodes** | 第三方RPC节点 | Third-party RPC nodes |
| **TronGrid** | 官方基础设施服务 | Official infrastructure service |
| **Rate Limit** | API调用限制 | API call limitations |
| **可用性 / Availability** | 网络不稳定因素 | Network instability factors |

### 现实约束 / Reality Constraints

```
🇨🇳 这些都不是异常，
而是wallet system的现实环境：
- 有rate limit
- 有可用性问题
- 有网络不稳定的情况

这是系统必须接受的外部依赖。

🇺🇸 These are not exceptions,
but wallet system's reality environment:
- Has rate limit
- Has availability issues
- Has network instability

This is external dependency system must accept.
```

---

## ✅ 完成检查清单 / Completion Checklist

### 🇨🇳 本集功能确认
- [ ] 理解从结构层到运行期的转变
- [ ] 准备好测试环境（Faucet）
- [ ] 实现TRX余额查询
- [ ] 实现TRX转账功能
- [ ] 理解运行期资源概念
- [ ] 明确Wallet Backend职责边界
- [ ] 理解基础设施依赖现实
- [ ] 观察并理解交易成功/失败行为

### 🇺🇸 Episode Feature Verification
- [ ] Understand transition from structural to runtime layer
- [ ] Prepare test environment (Faucet)
- [ ] Implement TRX balance query
- [ ] Implement TRX transfer functionality
- [ ] Understand runtime resource concepts
- [ ] Clarify Wallet Backend responsibility boundaries
- [ ] Understand infrastructure dependency reality
- [ ] Observe and understand transaction success/failure behavior

---

## 🚀 下一集预告 / Next Episode Preview

### 🇨🇳 EP08: TRC20代币转账
进入合约执行的世界：
- 🪙 **TRC20 Transfer**：智能合约级别的代币转账
- 📈 **复杂性放大**：为什么合约层的复杂性会进一步增加

### 🇺🇸 EP08: TRC20 Token Transfer
Enter the world of contract execution:
- 🪙 **TRC20 Transfer**: Smart contract-level token transfer
- 📈 **Complexity Amplified**: Why contract layer complexity further increases

---

## 🔗 相关资源 / Related Resources

### 开发工具 / Development Tools
- [GitHub Repository](https://github.com/eastgatedev/metamask-clone)
- [TRON Documentation](https://developers.tron.network/)
- [TronGrid API](https://www.trongrid.io/)

### 测试网络 / Test Networks
- [Nile Testnet Faucet](https://nileex.io/join/getJoinPage)
- [Shasta Testnet Faucet](https://www.trongrid.io/shasta)
- [Nile Explorer](https://nile.tronscan.org/)

### 🇨🇳 中文资源
- [TRON开发者文档](https://cn.developers.tron.network/)
- [TronGrid使用指南](https://www.trongrid.io/)

### 🇺🇸 English Resources
- [TRON Developer Hub](https://developers.tron.network/)
- [TronGrid Documentation](https://www.trongrid.io/)

---

## 📊 项目进度 / Project Progress

```
Phase 1-3: EVM Ecosystem                ✅ COMPLETED
├── Wallet Management                    ✅
├── BSC Integration                      ✅
├── BNB Transfer                         ✅
└── ERC20 Token                          ✅

Phase 4: TRON Ecosystem                 🚧 IN PROGRESS
├── TRON Wallet Structure                ✅ (EP06)
├── TRX Balance & Transfer               🚧 (EP07)
└── TRC20 Token                          ⏳ (EP08)

Phase 5: Advanced Features              📋 PLANNED
```

---

## 💭 核心要点回顾 / Key Takeaways

### 🇨🇳 这一集的重点
```
✅ 从结构层进入运行期
✅ TRX查询是只读操作，结果确定
✅ TRX转账是执行行为，有运行期成本
✅ 运行期资源（bandwidth/energy）不是钱包职责
✅ Wallet提供执行入口，不是执行保证
✅ 基础设施依赖是现实环境
✅ 职责边界最重要
```

### 🇺🇸 This Episode's Focus
```
✅ From structural layer to runtime
✅ TRX query is read-only, results deterministic
✅ TRX transfer is execution, has runtime cost
✅ Runtime resources (bandwidth/energy) not wallet's responsibility
✅ Wallet provides execution entry, not execution guarantee
✅ Infrastructure dependencies are reality
✅ Responsibility boundaries most important
```

---

**🎥 YouTube Series:** [AI + IntelliJ Plugin + Web3 (MetaMask Clone)](https://github.com/eastgatedev/metamask-clone)  
**👨‍💻 Author:** 东门Eastgate  
**📅 Last Updated:** 2026-01-26  
**⭐ Star the Repo:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)
