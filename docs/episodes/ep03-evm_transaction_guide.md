# EP03 - 从后端工程师视角理解EVM Transaction / Understanding EVM Transactions from Backend Engineer's Perspective

> **项目仓库 / Project Repository:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)  
> **当前进度 / Current Progress:** Phase 2 🚧 In Progress

---

## 📋 概述 / Overview

### 🇨🇳 中文
这一集不是Solidity教学，也不是DeFi、NFT的演示。我们从**后端工程师的视角**，用Kotlin和Web3j，带你真正看清楚：**一笔EVM Transaction在系统里到底是怎么发生的**。我们会从最原始的JSON-RPC开始，理解Web3j的封装本质，最终实现完整的余额查询和转账功能。

### 🇺🇸 English  
This episode is not a Solidity tutorial, nor a DeFi/NFT demo. From a **backend engineer's perspective**, using Kotlin and Web3j, we'll show you how **an EVM Transaction actually happens in the system**. Starting from raw JSON-RPC, understanding Web3j's encapsulation essence, and finally implementing complete balance query and transfer functionality.

---

## 🎯 学习目标 / Learning Objectives

### 🇨🇳 本集目标
- [ ] 理解为什么企业选择EVM兼容链
- [ ] 掌握BSC Testnet作为开发环境的优势
- [ ] 从JSON-RPC层面理解区块链交互
- [ ] 使用Web3j实现余额查询
- [ ] 实现BNB转账功能
- [ ] 理解Transaction vs Receipt的区别
- [ ] 将区块链功能集成到UI

### 🇺🇸 Episode Goals
- [ ] Understand why enterprises choose EVM-compatible chains
- [ ] Master advantages of BSC Testnet as development environment
- [ ] Understand blockchain interaction at JSON-RPC level
- [ ] Implement balance query using Web3j
- [ ] Implement BNB transfer functionality
- [ ] Understand difference between Transaction and Receipt
- [ ] Integrate blockchain features into UI

---

## 🤔 为什么选择EVM生态 / Why Choose EVM Ecosystem

### 🇨🇳 企业的现实选择
在企业环境里，我们关心的从来不是"支持多少条链"，而是：

### 🇺🇸 Enterprise's Practical Choice
In enterprise environments, we never care about "how many chains supported", but:

| 考量因素 / Consideration | 🇨🇳 说明 | 🇺🇸 Description |
|------------------------|---------|-----------------|
| **协议稳定性** | 不频繁更新，便于长期维护 | Infrequent updates, easy long-term maintenance |
| **EVM兼容性** | 代码可迁移，生态成熟 | Code portable, mature ecosystem |
| **工程化支持** | 有成熟的SDK和工具链 | Mature SDKs and toolchains |
| **人才储备** | 更容易找到有经验的开发者 | Easier to find experienced developers |

### Web3j的定位 / Web3j's Position

```
🇨🇳 Web3j不是魔法，而是：
- 一个非常工程化的封装
- JVM生态的最佳选择
- 专注于EVM兼容链

🇺🇸 Web3j is not magic, but:
- A very engineering-oriented encapsulation
- Best choice for JVM ecosystem
- Focused on EVM-compatible chains
```

---

## 🌐 为什么选择BSC Testnet / Why Choose BSC Testnet

### 🇨🇳 技术选择理由
这一集的教学环境使用**BSC Testnet**，不是因为要教BSC生态，而是因为它是一个**非常稳定的EVM Sandbox**。

### 🇺🇸 Technical Choice Rationale
This episode uses **BSC Testnet** as teaching environment, not to teach BSC ecosystem, but because it's a **very stable EVM Sandbox**.

| 优势 / Advantage | 🇨🇳 说明    | 🇺🇸 Description                     |
|-----------------|------------|--------------------------------------|
| **Ethereum Fork** | 100% EVM兼容 | 100% EVM compatible                  |
| **快速出块** | 1秒出块，调试高效  | 1-second blocks, efficient debugging |
| **协议稳定** | 大型更新不频繁    | Infrequent major updates             |
| **适合学习** | 专注EVM底层流程  | Focus on EVM underlying process      |

### 重要说明 / Important Note

```
🇨🇳 本集只关注：
- 协议与系统行为
- EVM交易流程
- 不涉及BSC生态细节

🇺🇸 This episode focuses only on:
- Protocol and system behavior
- EVM transaction flow
- No BSC ecosystem details
```

---

## 🚰 获取测试币 / Get Test Coins

### 🇨🇳 BSC Testnet Faucet
在做任何演示之前，我们先准备一些test BNB。没有余额，后续的getBalance、transfer都无法直观展示。

### 🇺🇸 BSC Testnet Faucet
Before any demo, let's prepare some test BNB. Without balance, subsequent getBalance and transfer cannot be demonstrated intuitively.

#### 步骤 / Steps

1. **访问Faucet / Visit Faucet**
   - URL: https://testnet.bnbchain.org/faucet-smart

2. **领取测试币 / Claim Test Coins**
   ```
   🇨🇳 输入钱包地址 → 完成验证 → 领取0.3 tBNB
   🇺🇸 Enter wallet address → Complete verification → Claim 0.3 tBNB
   ```

3. **确认到账 / Confirm Receipt**
   - 在BSCScan查看交易
   - Check transaction on BSCScan

---

## 💰 用Kotlin获取余额 / Get Balance with Kotlin

### 🇨🇳 Web3j实现
用Web3j，只需要几行代码就可以直接获取地址的余额：

### 🇺🇸 Web3j Implementation
With Web3j, just a few lines of code to get address balance:

```kotlin
// 🇨🇳 余额查询 | 🇺🇸 Balance Query
suspend fun getBalance(address: String): BigInteger {
    val web3j = Web3j.build(
        HttpService("https://data-seed-prebsc-1-s1.binance.org:8545/")
    )
    
    val balance = web3j.ethGetBalance(
        address, 
        DefaultBlockParameterName.LATEST
    ).send()
    
    return balance.balance
}

// 🇨🇳 格式化显示 | 🇺🇸 Format for display
fun formatBalance(balanceWei: BigInteger): String {
    val balanceEther = Convert.fromWei(
        balanceWei.toBigDecimal(), 
        Convert.Unit.ETHER
    )
    return "$balanceEther BNB"
}
```

---

## 🔍 用Postman直接调用JSON-RPC / Direct JSON-RPC Call with Postman

### JSON-RPC请求 / JSON-RPC Request

```json
{
  "jsonrpc": "2.0",
  "method": "eth_getBalance",
  "params": [
    "0xYourAddress",
    "latest"
  ],
  "id": 1
}
```

### 关键理解 / Key Understanding

| 要点 / Point | 🇨🇳 说明 | 🇺🇸 Description |
|-------------|---------|-----------------|
| **method名称** | `eth_getBalance`是Ethereum JSON-RPC规范的一部分 | `eth_getBalance` is part of Ethereum JSON-RPC spec |
| **不是SDK发明的** | 不是Web3j或Web3.js的API | Not invented by Web3j or Web3.js |
| **返回格式** | Hex格式，单位是Wei | Hex format, unit is Wei |

---

## 🔄 数据转换 / Data Conversion

### 🇨🇳 从Hex到可读数字
返回的数据是Hex格式的Wei，我们需要理解如何转换：

### 🇺🇸 From Hex to Readable Numbers
The returned data is in Hex format Wei, we need to understand how to convert:

### 转换步骤 / Conversion Steps

| 步骤 / Step | 🇨🇳 说明 | 🇺🇸 Description |
|-----------|---------|-----------------|
| **1. Hex → Decimal** | 16进制转10进制 | Convert hex to decimal |
| **2. Wei → Ether** | 除以 10^18 | Divide by 10^18 |
| **3. 格式化** | 保留合适的小数位 | Keep appropriate decimal places |

### 在线转换工具 / Online Converters
- [Base Converter](https://www.rapidtables.com/convert/number/base-converter.html)
- [ETH Converter](https://eth-converter.com/)

---

## 💸 发起BNB转账 / Initiate BNB Transfer

### 🇨🇳 改变链上状态
现在我们做一件真正会改变链上状态的事情：发起一笔转账。

### 🇺🇸 Change On-Chain State
Now let's do something that truly changes on-chain state: initiate a transfer.

### 转账实现 / Transfer Implementation

```kotlin
// 🇨🇳 BNB转账 | 🇺🇸 BNB Transfer
suspend fun sendBNB(
    fromAddress: String,
    privateKey: String,
    toAddress: String,
    amountInBNB: BigDecimal
): String {
    val web3j = Web3j.build(HttpService(RPC_URL))
    
    // 🇨🇳 加载凭证 | 🇺🇸 Load credentials
    val credentials = Credentials.create(privateKey)
    
    // 🇨🇳 转换金额 | 🇺🇸 Convert amount
    val amountInWei = Convert.toWei(amountInBNB, Convert.Unit.ETHER)
    
    // 🇨🇳 发送交易 | 🇺🇸 Send transaction
    val transactionReceipt = Transfer.sendFunds(
        web3j,
        credentials,
        toAddress,
        amountInBNB,
        Convert.Unit.ETHER
    ).send()
    
    return transactionReceipt.transactionHash
}
```

### 关键参数 / Key Parameters

| 参数 / Parameter | 🇨🇳 说明 | 🇺🇸 Description |
|-----------------|---------|-----------------|
| **Gas Limit** | 21000（基础转账固定值）| 21000 (fixed for basic transfer) |
| **Gas Price** | 10 GWei（测试网推荐）| 10 GWei (testnet recommended) |

---

## 🔍 检查交易状态 / Check Transaction Status

### 🇨🇳 Transaction Hash
当你发起交易后，节点会返回一个**transaction hash**。

### 🇺🇸 Transaction Hash
After initiating transaction, node returns a **transaction hash**.

### ⚠️ 重要概念 / Important Concept

```
🇨🇳 特别注意：
Hash只是一个请求编号。

交易可能：
- Pending（待确认）
- Failed（失败）
- Replaced（被替换）

Hash ≠ 交易成功

🇺🇸 Important Note:
Hash is just a request ID.

Transaction could be:
- Pending (waiting confirmation)
- Failed
- Replaced

Hash ≠ Transaction Success
```

---

## 📜 Transaction Receipt（最终裁决）/ Transaction Receipt (Final Verdict)

### 🇨🇳 Receipt才是真相
在EVM世界里，**receipt才是最终裁决**。

### 🇺🇸 Receipt is the Truth
In EVM world, **receipt is the final verdict**.

### 查询Receipt / Query Receipt

```kotlin
// 🇨🇳 获取交易收据 | 🇺🇸 Get transaction receipt
suspend fun getTransactionReceipt(txHash: String): TransactionReceipt? {
    val web3j = Web3j.build(HttpService(RPC_URL))
    
    val receipt = web3j.ethGetTransactionReceipt(txHash)
        .send()
        .transactionReceipt
    
    return receipt.orElse(null)
}

// 🇨🇳 检查交易状态 | 🇺🇸 Check transaction status
fun isTransactionSuccessful(receipt: TransactionReceipt): Boolean {
    return receipt.status == "0x1" // 1 = success, 0 = failed
}
```

### 核心字段 / Core Fields

| 字段 / Field | 🇨🇳 说明 | 🇺🇸 Description |
|-------------|---------|-----------------|
| **status** | "0x1"成功，"0x0"失败 | "0x1" success, "0x0" failed |
| **gasUsed** | 实际消耗的gas | Actual gas consumed |
| **blockNumber** | 所在区块号 | Block number |
| **logs** | 事件日志（ERC20会用到）| Event logs (used in ERC20) |

---

## 📊 核心概念总结 / Core Concepts Summary

### Transaction vs Receipt

| 概念 / Concept | 🇨🇳 含义 | 🇺🇸 Meaning |
|---------------|---------|-------------|
| **Transaction** | 用户意图 | User intention |
| **Tx Hash** | 请求编号 | Request ID |
| **Receipt** | 链上结果 | On-chain result |

### 💡 关键理解 / Key Understanding

```
🇨🇳 在EVM世界里：
你发出的 ≠ 发生的

任何真实的钱包系统，都会有一张transaction表
用来追踪交易的最终状态。

🇺🇸 In EVM world:
What you send ≠ What happened

Any real wallet system has a transaction table
to track the final status of transactions.
```

---

## ✅ 完成检查清单 / Completion Checklist

### 🇨🇳 本集功能确认
- [ ] 理解企业选择EVM的原因
- [ ] 掌握BSC Testnet环境配置
- [ ] 成功获取测试币
- [ ] 理解JSON-RPC原理
- [ ] 使用Postman验证API调用
- [ ] 用Web3j实现余额查询
- [ ] 实现BNB转账功能
- [ ] 理解Transaction vs Receipt
- [ ] 完成UI集成

### 🇺🇸 Episode Feature Verification
- [ ] Understand why enterprises choose EVM
- [ ] Master BSC Testnet environment configuration
- [ ] Successfully obtained test coins
- [ ] Understand JSON-RPC principles
- [ ] Verify API calls with Postman
- [ ] Implement balance query with Web3j
- [ ] Implement BNB transfer functionality
- [ ] Understand Transaction vs Receipt
- [ ] Complete UI integration

---

## 🚀 下一集预告 / Next Episode Preview

### 🇨🇳 EP04: ERC20代币
- 🪙 **深入ERC20标准**：Token如何在EVM机制下运作

### 🇺🇸 EP04: ERC20 Token
- 🪙 **Deep Dive into ERC20 Standard**: How tokens work under EVM mechanism

---

## 🔗 相关资源 / Related Resources

### 开发工具 / Development Tools
- [GitHub Repository](https://github.com/eastgatedev/metamask-clone)
- [BSC Testnet Faucet](https://testnet.bnbchain.org/faucet-smart)
- [BSC Testnet Explorer](https://testnet.bscscan.com/)
- [Web3j Documentation](https://docs.web3j.io/)
- [Postman](https://www.postman.com/)

### 转换工具 / Conversion Tools
- [Base Converter](https://www.rapidtables.com/convert/number/base-converter.html)
- [ETH Unit Converter](https://eth-converter.com/)

### 🇨🇳 中文资源
- [Ethereum JSON-RPC规范](https://ethereum.org/zh/developers/docs/apis/json-rpc/)
- [Web3j开发指南](https://docs.web3j.io/)

### 🇺🇸 English Resources
- [Ethereum JSON-RPC Specification](https://ethereum.org/en/developers/docs/apis/json-rpc/)
- [Web3j Development Guide](https://docs.web3j.io/)

---

## 📊 项目进度 / Project Progress

```
Phase 1: Basic Wallet Management           ✅ COMPLETED
├── Create/Import/Export Wallet             ✅
└── Secure Storage                          ✅

Phase 2: Multi-Chain & Balance             ✅ COMPLETED (EP02-EP03)
├── Multi-Agent Collaboration               ✅
├── UI Redesign                             ✅
├── BSC Testnet Integration                 ✅
├── Balance Query                           ✅
└── BNB Transfer                            ✅

Phase 3: ERC20 Token                        📋 NEXT (EP04)
├── ERC20 Standard Understanding            ⏳
├── Deploy Custom Token                     ⏳
├── Token Balance Query                     ⏳
├── Token Transfer                          ⏳
└── Event Log Parsing                       ⏳

Phase 4: Advanced Features                 📋 PLANNED
```

---

## 💭 核心要点回顾 / Key Takeaways

### 🇨🇳 这一集的重点
```
✅ Web3j不是魔法，是工程化的封装
✅ JSON-RPC是一切的基础
✅ Transaction Hash只是请求编号
✅ Receipt才是最终裁决
✅ 每一个钱包操作背后，链上到底发生了什么
```

### 🇺🇸 This Episode's Focus
```
✅ Web3j is not magic, it's engineering encapsulation
✅ JSON-RPC is the foundation of everything
✅ Transaction Hash is just a request ID
✅ Receipt is the final verdict
✅ What really happens on-chain behind every wallet operation
```

---

**🎥 YouTube Series:** [AI + IntelliJ Plugin + Web3 (MetaMask Clone)](https://github.com/eastgatedev/metamask-clone)  
**👨‍💻 Author:** 东门Eastgate  
**📅 Last Updated:** 2025-09-06  
**⭐ Star the Repo:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)