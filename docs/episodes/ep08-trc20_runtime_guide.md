# EP08 - TRC20 Runtime / TRC20 Runtime

> **项目仓库 / Project Repository:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)  
> **当前进度 / Current Progress:** Phase 4 🚧 In Progress

---

## 📋 概述 / Overview

### 🇨🇳 中文
延续EP07的TRX Runtime，本集把视角放在**TRC20**。这一集不是教你写token，也不是分析token安全性。我们只从**Wallet System / Backend**角度看：当**TRC20成为支付单位**时，钱包系统在运行期需要面对什么。重点理解TRC20作为contract-based payment unit的运行期行为，以及与ERC20在执行成本模型上的差异。

### 🇺🇸 English  
Continuing EP07's TRX Runtime, this episode focuses on **TRC20**. This is not about teaching you to write tokens or analyzing token security. From **Wallet System / Backend** perspective only: when **TRC20 becomes a payment unit**, what does wallet system face during runtime? Focus on understanding TRC20 as contract-based payment unit's runtime behavior, and differences from ERC20 in execution cost model.

---

## 🎯 学习目标 / Learning Objectives

### 🇨🇳 本集目标
- [ ] 理解TRC20作为contract-based payment unit的定位
- [ ] 掌握TRC20与ERC20的运行期差异
- [ ] 理解TRC20部署的教学目的
- [ ] 明确OpenZeppelin在TRC20场景的角色
- [ ] 实现TRC20余额查询
- [ ] 实现TRC20代币转账
- [ ] 理解合约执行的复杂性
- [ ] 延续Wallet Backend职责边界思维

### 🇺🇸 Episode Goals
- [ ] Understand TRC20's positioning as contract-based payment unit
- [ ] Master runtime differences between TRC20 and ERC20
- [ ] Understand educational purpose of TRC20 deployment
- [ ] Clarify OpenZeppelin's role in TRC20 scenario
- [ ] Implement TRC20 balance query
- [ ] Implement TRC20 token transfer
- [ ] Understand complexity of contract execution
- [ ] Continue Wallet Backend responsibility boundary thinking

---

## 🎬 本集定位 / Episode Positioning

### 🇨🇳 延续EP07
在EP07我们讨论的是**TRX作为原生资产**在运行期的行为。本集继续往前走，视角放在**TRC20**。

### 🇺🇸 Continuing EP07
In EP07 we discussed **TRX as native asset** runtime behavior. This episode moves forward, focusing on **TRC20**.

### 核心观点 / Core Viewpoint

```
🇨🇳 我们不会把TRC20当成「复杂的token」
而是当成：一种基于合约的支付单位

差异不在token，而在执行成本的模型

🇺🇸 We don't treat TRC20 as "complex token"
But as: A contract-based payment unit

Difference is not in token, but in execution cost model
```

---

## 🔄 TRC20 vs ERC20（运行期视角）/ TRC20 vs ERC20 (Runtime Perspective)

### 🇨🇳 接口层的相似性
从接口层来看，TRC20和ERC20非常相似：

### 🇺🇸 Interface Layer Similarity
From interface layer, TRC20 and ERC20 are very similar:

```solidity
// 🇨🇳 相同的核心接口 | 🇺🇸 Same core interfaces
function balanceOf(address account) external view returns (uint256);
function transfer(address to, uint256 amount) external returns (bool);
function approve(address spender, uint256 amount) external returns (bool);
```

### 运行期行为对比 / Runtime Behavior Comparison

| 行为 / Behavior | ERC20 | TRC20 | 🇨🇳 差异 | 🇺🇸 Difference |
|----------------|-------|-------|---------|----------------|
| **Transfer执行 / Transfer Execution** | 合约执行 / Contract execution | 合约执行 / Contract execution | 本质相同 | Essentially same |
| **执行成本 / Execution Cost** | Gas (ETH) | Runtime Resources (TRX) | 成本模型不同 | Different cost model |
| **Deposit行为 / Deposit Behavior** | 直接转账到合约 / Direct transfer to contract | 直接转账到合约 / Direct transfer to contract | 机制相同 | Mechanism same |
| **资源消耗 / Resource Consumption** | Gas Limit | Bandwidth + Energy | 资源类型不同 | Different resource types |

### Runtime Resources对比 / Runtime Resources Comparison

| 资源 / Resource | EVM (ERC20) | TRON (TRC20) |
|----------------|-------------|--------------|
| **基础单位 / Base Unit** | Gas | Bandwidth + Energy |
| **支付货币 / Payment Currency** | ETH | TRX |
| **成本计算 / Cost Calculation** | Gas Used × Gas Price | Bandwidth消耗 + Energy消耗 / Bandwidth + Energy consumption |
| **钱包视角 / Wallet Perspective** | 统一的Gas模型 / Unified Gas model | 多资源但TRX结算 / Multi-resource, TRX settlement |

### 关键理解 / Key Understanding

```
🇨🇳 在Wallet System的视角下：
真正重要的不是接口，而是运行期行为

ERC20的transfer：
- 本质是合约执行
- 成本是gas（ETH）

TRC20的transfer：
- 同样是合约执行
- 成本来自TRON运行期资源
- 最终以TRX结算

🇺🇸 From Wallet System perspective:
What truly matters is not interface, but runtime behavior

ERC20 transfer:
- Essentially contract execution
- Cost is gas (ETH)

TRC20 transfer:
- Also contract execution
- Cost from TRON runtime resources
- Finally settled in TRX
```

---

## 🚀 TRC20 Deployment（教学前置）/ TRC20 Deployment (Educational Setup)

### 🇨🇳 部署目的
在进入钱包系统交互之前，我们需要一个**可用的TRC20合约**。

### 🇺🇸 Deployment Purpose
Before wallet system interaction, we need a **usable TRC20 contract**.

### 使用官方示例 / Use Official Example

```
🇨🇳 使用TRON官方提供的TRC20示例合约
部署仅用于：
- 教学
- 测试
- 验证wallet system的行为

🇺🇸 Use TRON official TRC20 example contract
Deployment only for:
- Teaching
- Testing
- Verifying wallet system behavior
```

**官方文档 / Official Documentation:**
https://developers.tron.network/docs/issuing-trc20-tokens-tutorial

**合约编译器 / Contract Compiler:**
https://shasta.tronscan.org/#/contracts/contract-compiler

### 不讨论的内容 / Not Discussed

```
❌ Token设计
❌ Token分发
❌ Token安全性

✅ 把这个合约当成：钱包系统的测试依赖

❌ Token design
❌ Token distribution
❌ Token security

✅ Treat this contract as: Wallet system's test dependency
```

---

## 🛡️ OpenZeppelin在Wallet System中的角色 / OpenZeppelin's Role in Wallet System

### 🇨🇳 ERC20中的角色回顾
在之前ERC20的集数里，OpenZeppelin的角色是：**被广泛接受的ERC20行为实现**

### 🇺🇸 Role in ERC20 Recap
In previous ERC20 episodes, OpenZeppelin's role was: **widely accepted ERC20 behavior implementation**

### TRC20场景下的定位 / Positioning in TRC20 Scenario

| 角色 / Role | 🇨🇳 说明 | 🇺🇸 Description |
|------------|---------|-----------------|
| **不是安全背书 / Not Security Endorsement** | 不代表生产环境标准 | Doesn't represent production standard |
| **不是生产建议 / Not Production Advice** | 仅作为行为参考 | Only as behavior reference |
| **作用 / Function** | ERC20世界中钱包系统默认假设的行为模型 | Behavior model wallet system assumes in ERC20 world |
| **目的 / Purpose** | 保持ERC20/TRC20教学一致性 | Maintain ERC20/TRC20 teaching consistency |

### 核心定位 / Core Positioning

```
🇨🇳 在Wallet System的视角下：
OpenZeppelin作为ERC20世界中，
钱包系统默认假设的行为模型

目的：让ERC20和TRC20的教学保持一致
而不是让你去复用合约代码

🇺🇸 From Wallet System perspective:
OpenZeppelin as behavior model
wallet system assumes in ERC20 world

Purpose: Keep ERC20 and TRC20 teaching consistent
Not for you to reuse contract code
```

---

## 💰 Test Case 1：查询TRC20 Balance / Query TRC20 Balance

### 🇨🇳 合约只读调用
查询TRC20 balance是一种**合约只读调用**：

### 🇺🇸 Contract Read-only Call
Querying TRC20 balance is a **contract read-only call**:

### 操作特性 / Operation Characteristics

| 特性 / Characteristic | 🇨🇳 说明 | 🇺🇸 Description |
|---------------------|---------|-----------------|
| **只读操作 / Read-only Operation** | 不改变合约状态 | Doesn't change contract state |
| **不需签名 / No Signature Required** | 无需私钥参与 | No private key required |
| **结果确定 / Deterministic Result** | 调用即可返回 | Returns upon call |
| **建立基线 / Establish Baseline** | 与EP07 TRX balance角色相同 | Same role as EP07 TRX balance |

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 查询TRC20余额 | 🇺🇸 Query TRC20 Balance
suspend fun getTRC20Balance(
    contractAddress: String,
    walletAddress: String
): BigInteger {
    val apiWrapper = TronApiWrapper()
    
    // 🇨🇳 构建balanceOf调用 | 🇺🇸 Build balanceOf call
    val functionSelector = "balanceOf(address)"
    val parameter = walletAddress.removePrefix("T") // Convert to hex
    
    // 🇨🇳 触发合约常量调用 | 🇺🇸 Trigger contract constant call
    val result = apiWrapper.triggerConstantContract(
        contractAddress = contractAddress,
        functionSelector = functionSelector,
        parameter = parameter
    )
    
    // 🇨🇳 解析返回值 | 🇺🇸 Parse return value
    return BigInteger(result.constantResult[0], 16)
}

// 🇨🇳 格式化显示（考虑decimals）| 🇺🇸 Format display (consider decimals)
suspend fun formatTRC20Balance(
    balance: BigInteger,
    decimals: Int = 18
): String {
    val divisor = BigDecimal.TEN.pow(decimals)
    val formattedBalance = balance.toBigDecimal().divide(divisor)
    return formattedBalance.toPlainString()
}
```

### Wallet System处理 / Wallet System Handles

```
🇨🇳 钱包系统需要处理：
- Token合约地址
- 用户钱包地址
- 返回的token balance

与EP07查询TRX balance的角色相同：
建立一个运行期的基线状态

🇺🇸 Wallet system handles:
- Token contract address
- User wallet address
- Returned token balance

Same role as EP07 TRX balance query:
Establish runtime baseline state
```

---

## 💸 Test Case 2：发送TRC20 Token / Send TRC20 Token

### 🇨🇳 核心运行期操作
发送TRC20 token是本集的真正重点：

### 🇺🇸 Core Runtime Operation
Sending TRC20 token is this episode's real focus:

### Wallet Backend流程 / Wallet Backend Flow

```
🇨🇳 流程：
1. 构建合约调用
2. 使用私钥签名
3. 广播交易

🇺🇸 Flow:
1. Build contract call
2. Sign with private key
3. Broadcast transaction
```

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 发送TRC20代币 | 🇺🇸 Send TRC20 Token
suspend fun sendTRC20(
    contractAddress: String,
    fromAddress: String,
    privateKey: String,
    toAddress: String,
    amount: BigInteger
): String {
    val apiWrapper = TronApiWrapper()
    
    // 🇨🇳 1. 构建transfer调用 | 🇺🇸 1. Build transfer call
    val functionSelector = "transfer(address,uint256)"
    val parameters = buildTransferParameters(toAddress, amount)
    
    // 🇨🇳 2. 创建交易 | 🇺🇸 2. Create transaction
    val transaction = apiWrapper.triggerSmartContract(
        contractAddress = contractAddress,
        functionSelector = functionSelector,
        parameter = parameters,
        feeLimit = 100_000_000, // 100 TRX
        ownerAddress = fromAddress
    )
    
    // 🇨🇳 3. 签名交易 | 🇺🇸 3. Sign transaction
    val signedTransaction = signTransaction(
        transaction.transaction,
        privateKey
    )
    
    // 🇨🇳 4. 广播交易 | 🇺🇸 4. Broadcast transaction
    val result = apiWrapper.broadcastTransaction(signedTransaction)
    
    // 🇨🇳 5. 返回交易哈希 | 🇺🇸 5. Return transaction hash
    return result.txid
}

// 🇨🇳 构建transfer参数 | 🇺🇸 Build transfer parameters
private fun buildTransferParameters(
    toAddress: String,
    amount: BigInteger
): String {
    // Convert address to bytes32
    val addressParam = toAddress.removePrefix("T")
        .padStart(64, '0')
    
    // Convert amount to bytes32
    val amountParam = amount.toString(16)
        .padStart(64, '0')
    
    return addressParam + amountParam
}
```

### 与TRX Transfer的差异 / Difference from TRX Transfer

| 对比项 / Comparison | TRX Transfer | TRC20 Transfer |
|-------------------|--------------|----------------|
| **执行性质 / Execution Nature** | 原生资产转移 / Native asset transfer | 合约执行 / Contract execution |
| **运行期成本 / Runtime Cost** | 较低 / Lower | 较高 / Higher |
| **执行路径 / Execution Path** | 简单 / Simple | 复杂 / Complex |
| **失败场景 / Failure Scenarios** | 较少 / Fewer | 较多 / More |

### 关键理解 / Key Understanding

```
🇨🇳 与TRX transfer不同：
TRC20 transfer是一次完整的合约执行

这意味着：
- 运行期成本更高
- 执行路径更复杂
- 失败场景更多

在wallet system里，我们不会试图控制这些复杂性
而是做一件事：

正确地发起执行，并如实地呈现执行结果

钱包系统不保证成功，只保证行为是正确的

🇺🇸 Different from TRX transfer:
TRC20 transfer is a complete contract execution

This means:
- Higher runtime cost
- More complex execution path
- More failure scenarios

In wallet system, we don't try to control these complexities
But do one thing:

Correctly initiate execution and truthfully present results

Wallet system doesn't guarantee success, only guarantees correct behavior
```

---

## ✅ 完成检查清单 / Completion Checklist

### 🇨🇳 本集功能确认
- [ ] 理解TRC20作为contract-based payment unit
- [ ] 掌握TRC20与ERC20运行期差异
- [ ] 理解TRC20部署的教学目的
- [ ] 明确OpenZeppelin的角色定位
- [ ] 实现TRC20余额查询功能
- [ ] 实现TRC20代币转账功能
- [ ] 理解合约执行的复杂性
- [ ] 延续职责边界思维

### 🇺🇸 Episode Feature Verification
- [ ] Understand TRC20 as contract-based payment unit
- [ ] Master TRC20 vs ERC20 runtime differences
- [ ] Understand educational purpose of TRC20 deployment
- [ ] Clarify OpenZeppelin's role positioning
- [ ] Implement TRC20 balance query functionality
- [ ] Implement TRC20 token transfer functionality
- [ ] Understand contract execution complexity
- [ ] Continue responsibility boundary thinking

---

## 🚀 下一集预告 / Next Episode Preview

### 🇨🇳 EP09: Bitcoin钱包
- ₿ **Bitcoin**

### 🇺🇸 EP09: Bitcoin Wallet
- ₿ **Bitcoin**

---

## 🔗 相关资源 / Related Resources

### 开发工具 / Development Tools
- [GitHub Repository](https://github.com/eastgatedev/metamask-clone)
- [TRON Documentation](https://developers.tron.network/)
- [TRC20 Official Tutorial](https://developers.tron.network/docs/issuing-trc20-tokens-tutorial)

### 🇨🇳 中文资源
- [TRON开发者文档](https://cn.developers.tron.network/)
- [TRC20代币发行教程](https://developers.tron.network/docs/issuing-trc20-tokens-tutorial)

### 🇺🇸 English Resources
- [TRON Developer Hub](https://developers.tron.network/)
- [TRC20 Token Issuance Tutorial](https://developers.tron.network/docs/issuing-trc20-tokens-tutorial)

---

## 📊 项目进度 / Project Progress

```
Phase 1-3: EVM Ecosystem                ✅ COMPLETED
├── Wallet Management                    ✅
├── BSC Integration                      ✅
├── BNB Transfer                         ✅
└── ERC20 Token                          ✅

Phase 4: TRON Ecosystem                 ✅ COMPLETED
├── TRON Wallet Structure                ✅ (EP06)
├── TRX Balance & Transfer               ✅ (EP07)
└── TRC20 Token                          ✅ (EP08)

Phase 5: Bitcoin                        📋 NEXT (EP09)
└── Bitcoin Wallet                       ⏳
```

---

## 💭 核心要点回顾 / Key Takeaways

### 🇨🇳 这一集的重点
```
✅ TRC20是contract-based payment unit
✅ 接口相似，运行期行为不同
✅ 差异在执行成本模型，不在token本身
✅ TRC20 transfer是完整的合约执行
✅ 复杂性来自运行期，不是token
✅ 钱包职责：执行与呈现，不是保证结果
✅ OpenZeppelin作为行为参考，不是标准
```

### 🇺🇸 This Episode's Focus
```
✅ TRC20 is contract-based payment unit
✅ Similar interface, different runtime behavior
✅ Difference in execution cost model, not token itself
✅ TRC20 transfer is complete contract execution
✅ Complexity from runtime, not token
✅ Wallet responsibility: execute and present, not guarantee results
✅ OpenZeppelin as behavior reference, not standard
```

---

**🎥 YouTube Series:** [AI + IntelliJ Plugin + Web3 (MetaMask Clone)](https://github.com/eastgatedev/metamask-clone)  
**👨‍💻 Author:** 东门Eastgate  
**📅 Last Updated:** 2026-02-01  
**⭐ Star the Repo:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)
