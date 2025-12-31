# EP04 - 从Native Coin到ERC20 Token / From Native Coin to ERC20 Token

> **项目仓库 / Project Repository:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)  
> **当前进度 / Current Progress:** Phase 3 🚧 In Progress

---

## 📋 概述 / Overview

### 🇨🇳 中文
在EP03我们转的是Native Coin（ETH、BNB），但在真实世界，你用钱包最多的其实是ERC20代币。本集从钱包系统和后端的角度，深入理解**ERC20到底和Native Coin有什么本质不同**。我们会使用OpenZeppelin部署ERC20合约，理解DApp如何连接钱包，并用Web3j实现ERC20的余额查询和转账功能。

### 🇺🇸 English  
In EP03 we transferred Native Coins (ETH, BNB), but in the real world, you mostly use ERC20 tokens in your wallet. From the wallet system and backend perspective, this episode deeply understands **the fundamental difference between ERC20 and Native Coin**. We'll deploy ERC20 contracts using OpenZeppelin, understand how DApps connect to wallets, and implement ERC20 balance queries and transfers with Web3j.

---

## 🎯 学习目标 / Learning Objectives

### 🇨🇳 本集目标
- [ ] 理解ERC20与Native Coin的本质区别
- [ ] 掌握为什么不要自己写ERC20
- [ ] 学习OpenZeppelin的价值和重要性
- [ ] 使用Remix IDE部署ERC20合约
- [ ] 理解DApp如何连接钱包（window.ethereum）
- [ ] 用Web3j实现ERC20余额查询
- [ ] 实现ERC20代币转账功能
- [ ] 将ERC20功能集成到钱包UI

### 🇺🇸 Episode Goals
- [ ] Understand fundamental difference between ERC20 and Native Coin
- [ ] Master why you shouldn't write your own ERC20
- [ ] Learn OpenZeppelin's value and importance
- [ ] Deploy ERC20 contract using Remix IDE
- [ ] Understand how DApps connect to wallets (window.ethereum)
- [ ] Implement ERC20 balance query with Web3j
- [ ] Implement ERC20 token transfer functionality
- [ ] Integrate ERC20 features into wallet UI

---

## 🔄 回顾：ETH Transfer做了什么 / Recap: What ETH Transfer Does

### 🇨🇳 Native Coin转账
ETH/BNB转账对钱包来说非常简单：

### 🇺🇸 Native Coin Transfer
ETH/BNB transfer is very simple for wallets:

| 字段 / Field | 🇨🇳 说明 | 🇺🇸 Description |
|-------------|---------|-----------------|
| **to** | 接收者地址 | Recipient address |
| **value** | 转账金额 | Transfer amount |
| **data** | 空（无数据）| Empty (no data) |

### 一句话总结 / One-line Summary

```
🇨🇳 ETH是链原生转账
🇺🇸 ETH is chain-native transfer
```

---

## 💡 ERC20最大的不同点 / ERC20's Biggest Difference

### 🇨🇳 本质区别
**ERC20根本没有所谓的transfer transaction。**

ERC20的transfer本质上是**调用Smart Contract的方法**。

### 🇺🇸 Essential Difference
**ERC20 has no such thing as a transfer transaction.**

ERC20 transfer is essentially **calling a Smart Contract method**.

### 对比 / Comparison

| 类型 / Type | 🇨🇳 本质 | 🇺🇸 Essence |
|------------|---------|-------------|
| **ETH/BNB** | Send value（发送价值）| Send value |
| **ERC20** | Call function（调用函数）| Call function |

---

## 🎯 ERC20 Transfer的真实本质 / The Real Nature of ERC20 Transfer

### 🇨🇳 把话讲白
当你在转ERC20，你不是在"转币"。

你是在对一个Smart Contract说：

> 「请帮我改一下你内部的balance mapping。」

### 🇺🇸 Plain Speaking
When you transfer ERC20, you're not "transferring coins".

You're telling a Smart Contract:

> "Please update your internal balance mapping for me."

### 钱包系统的角度 / From Wallet System Perspective

```
🇨🇳 钱包 ≠ 转账系统
钱包 = Contract Interaction 工具

🇺🇸 Wallet ≠ Transfer system
Wallet = Contract Interaction tool
```

---

## 🛡️ 为什么要用OpenZeppelin / Why Use OpenZeppelin

### 🇨🇳 行业标准
OpenZeppelin是**行业默认的Smart Contract标准库**。

### 🇺🇸 Industry Standard
OpenZeppelin is the **industry-default Smart Contract standard library**.

### 三个原因 / Three Reasons

| 原因 / Reason | 🇨🇳 说明 | 🇺🇸 Description |
|--------------|---------|-----------------|
| **被审计过 / Audited** | 经过多次安全审计 | Multiple security audits |
| **被广泛使用 / Widely Used** | 大量项目在使用 | Used by numerous projects |
| **不是玩具 / Not a Toy** | 生产级代码质量 | Production-grade code quality |

### 重要原则 / Important Principle

```
🇨🇳 Don't write your own ERC20
🇺🇸 Don't write your own ERC20
```

---

## ⚠️ 为什么不要自己写ERC20 / Why Not Write Your Own ERC20

### 🇨🇳 真正的问题
ERC20看起来很简单，但真正的问题从来不在transfer那一行。

### 🇺🇸 Real Problems
ERC20 looks simple, but real problems are never in the transfer line.

### 容易遗漏的地方 / Easy to Miss

| 问题 / Issue | 🇨🇳 说明 | 🇺🇸 Description |
|-------------|---------|-----------------|
| **decimals** | 精度处理 | Precision handling |
| **allowance** | 授权机制 | Approval mechanism |
| **edge cases** | 边界情况 | Edge cases |

### 架构角度的现实 / Architectural Reality

```
🇨🇳 安全不是你一开始就有的东西，
是被用久了、被踩过坑之后才有的。

🇺🇸 Security isn't something you have from the start,
it comes from being used for a long time and learning from mistakes.
```

---

## 🔧 为什么用Remix IDE / Why Use Remix IDE

### 🇨🇳 选择Remix的原因
- Browser-based（浏览器内运行）
- 不用setup（无需配置）

### 🇺🇸 Why Choose Remix
- Browser-based
- No setup required

### 适合场景 / Suitable For

```
🇨🇳 适合：
- 学习
- Demo
- 教学

我们不是在做production deploy，
我们只是用它来理解ERC20的行为。

🇺🇸 Suitable for:
- Learning
- Demo
- Teaching

We're not doing production deploy,
we're just using it to understand ERC20 behavior.
```

## 🚀 部署ERC20合约 / Deploy ERC20 Contract

### 部署步骤 / Deployment Steps

1. **打开OpenZeppelin Wizard** / Open OpenZeppelin Wizard
   - URL: https://wizard.openzeppelin.com/
   - 修改Name、Symbol和Premint
   - Modify Name, Symbol and Premint

2. **打开Remix IDE** / Open Remix IDE
   - URL: https://remix.ethereum.org

3. **编写合约** / Write Contract
   - 使用OpenZeppelin模板
   - Use OpenZeppelin template

4. **编译合约** / Compile Contract
   - 选择Solidity版本
   - Select Solidity version

5. **连接MetaMask** / Connect MetaMask
   - 选择BSC Testnet
   - Select BSC Testnet

6. **部署合约** / Deploy Contract
   - 支付gas费
   - Pay gas fee

### 链上验证 / On-Chain Verification

```
🇨🇳 在BscScan上查看合约
确认这件事真的发生在链上

🇺🇸 View contract on BscScan
Confirm this really happened on-chain
```

---

## 🔗 DApp是怎么「连上钱包」的 / How DApps "Connect to Wallet"

### 🇨🇳 揭开"神奇"的面纱
当你在Remix deploy contract时，浏览器会弹出MetaMask要你connect wallet。这不是魔法。

### 🇺🇸 Unveiling the "Magic"
When deploying contract in Remix, browser pops up MetaMask asking to connect wallet. This is not magic.

### 本质机制 / Essential Mechanism

| 步骤 / Step | 🇨🇳 说明 | 🇺🇸 Description |
|-----------|---------|-----------------|
| **1. DApp运行 / DApp Runs** | 在浏览器里运行 | Runs in browser |
| **2. 查找provider / Find Provider** | 寻找`window.ethereum` | Looks for `window.ethereum` |
| **3. Extension注入 / Extension Injects** | MetaMask inject这个object | MetaMask injects this object |
| **4. 请求授权 / Request Authorization** | DApp请求使用provider | DApp requests to use provider |

### 关键概念 / Key Concept

```
🇨🇳 所谓「连接钱包」，
其实只是DApp请求你授权使用这个provider。

🇺🇸 So-called "connect wallet"
is just DApp requesting your authorization to use this provider.
```

---

## 📱 Mobile Wallet的情况 / Mobile Wallet Situation

### 🇨🇳 同样的逻辑
在Mobile Wallet里也是同一套逻辑：

### 🇺🇸 Same Logic
Same logic in Mobile Wallet:

### Mobile实现方式 / Mobile Implementation

| 组件 / Component | 🇨🇳 说明 | 🇺🇸 Description |
|-----------------|---------|-----------------|
| **DApp** | 跑在WebView里 | Runs in WebView |
| **Wallet App / 钱包应用** | 在WebView里inject `window.ethereum` | Injects `window.ethereum` in WebView |
| **接口标准 / Interface Standard** | EIP-1193 Provider | EIP-1193 Provider |

### DApp的视角 / DApp's Perspective

```
🇨🇳 DApp根本不关心：
你是Browser Extension，还是Mobile App。

它只关心一件事：
有没有一个符合规范的provider。

🇺🇸 DApp doesn't care:
Whether you're Browser Extension or Mobile App.

It only cares about one thing:
Is there a standard-compliant provider.
```

---

## 💭 为什么IntelliJ Plugin理论上也可以 / Why IntelliJ Plugin Theoretically Works

### 核心理解 / Core Understanding

```
🇨🇳 Wallet并不神秘。
它只是一个在正确的执行环境里，实现了正确接口的系统。

IntelliJ Plugin有WebView，也能inject provider，
理论上是可行的。

🇺🇸 Wallet is not mysterious.
It's just a system that implements the right interface 
in the right execution environment.

IntelliJ Plugin has WebView and can inject provider,
theoretically feasible.
```

---

## 💰 用Web3j实现ERC20功能 / Implement ERC20 with Web3j

### 余额查询 / Balance Query

```kotlin
// 🇨🇳 ERC20余额查询 | 🇺🇸 ERC20 Balance Query
suspend fun getERC20Balance(
    tokenAddress: String,
    walletAddress: String
): BigInteger {
    val web3j = Web3j.build(HttpService(RPC_URL))
    
    // 🇨🇳 加载ERC20合约 | 🇺🇸 Load ERC20 contract
    val contract = ERC20.load(
        tokenAddress,
        web3j,
        credentials,
        DefaultGasProvider()
    )
    
    return contract.balanceOf(walletAddress).send()
}
```

### ERC20转账 / ERC20 Transfer

```kotlin
// 🇨🇳 ERC20代币转账 | 🇺🇸 ERC20 Token Transfer
suspend fun transferERC20(
    tokenAddress: String,
    privateKey: String,
    toAddress: String,
    amount: BigInteger
): TransactionReceipt {
    val web3j = Web3j.build(HttpService(RPC_URL))
    val credentials = Credentials.create(privateKey)
    
    // 🇨🇳 加载合约 | 🇺🇸 Load contract
    val contract = ERC20.load(
        tokenAddress,
        web3j,
        credentials,
        DefaultGasProvider()
    )
    
    // 🇨🇳 调用transfer方法 | 🇺🇸 Call transfer method
    return contract.transfer(toAddress, amount).send()
}
```

---

## 🧪 Backend验证：Unit Test / Backend Verification: Unit Test

### 🇨🇳 测试目的
写unit test不是测试UI，而是确认：**合约里的状态，真的变了。**

### 🇺🇸 Test Purpose
Write unit test not to test UI, but to confirm: **Contract state really changed.**

### 测试用例 / Test Cases

```kotlin
@Test
fun `test ERC20 balanceOf`() = runBlocking {
    val balance = getERC20Balance(
        tokenAddress = "0x...",
        walletAddress = "0x..."
    )
    
    assertTrue(balance > BigInteger.ZERO)
}

@Test
fun `test ERC20 transfer`() = runBlocking {
    val initialBalance = getERC20Balance(tokenAddress, toAddress)
    
    transferERC20(
        tokenAddress = tokenAddress,
        privateKey = privateKey,
        toAddress = toAddress,
        amount = BigInteger.valueOf(100)
    )
    
    val finalBalance = getERC20Balance(tokenAddress, toAddress)
    assertEquals(
        initialBalance + BigInteger.valueOf(100),
        finalBalance
    )
}
```

### Web3j的角色 / Web3j's Role

```
🇨🇳 在这里，Web3j的角色只是一个RPC client
🇺🇸 Here, Web3j's role is just an RPC client
```

---

## ✅ 完成检查清单 / Completion Checklist

### 🇨🇳 本集功能确认
- [ ] 理解ERC20与Native Coin的本质区别
- [ ] 掌握OpenZeppelin的重要性
- [ ] 理解为什么不要自己写ERC20
- [ ] 成功部署ERC20合约到BSC Testnet
- [ ] 理解DApp连接钱包的机制
- [ ] 理解window.ethereum和EIP-1193
- [ ] 用Web3j实现ERC20余额查询
- [ ] 用Web3j实现ERC20转账
- [ ] 通过unit test验证合约状态变化
- [ ] 在MetaMask中导入和使用ERC20

### 🇺🇸 Episode Feature Verification
- [ ] Understand essential difference between ERC20 and Native Coin
- [ ] Master importance of OpenZeppelin
- [ ] Understand why not to write your own ERC20
- [ ] Successfully deploy ERC20 contract to BSC Testnet
- [ ] Understand DApp wallet connection mechanism
- [ ] Understand window.ethereum and EIP-1193
- [ ] Implement ERC20 balance query with Web3j
- [ ] Implement ERC20 transfer with Web3j
- [ ] Verify contract state changes through unit tests
- [ ] Import and use ERC20 in MetaMask

---

## 🚀 下一集预告 / Next Episode Preview

### 🇨🇳 EP05: 发布到IntelliJ Plugin Marketplace
- 📦 **准备发布**：将MetaMask Clone发布到官方插件市场

### 🇺🇸 EP05: Publish to IntelliJ Plugin Marketplace
- 📦 **Prepare for Release**: Publish MetaMask Clone to official plugin marketplace

---

## 🔗 相关资源 / Related Resources

### 开发工具 / Development Tools
- [GitHub Repository](https://github.com/eastgatedev/metamask-clone)
- [Remix IDE](https://remix.ethereum.org/)
- [OpenZeppelin Contracts](https://docs.openzeppelin.com/contracts/)
- [BSC Testnet Explorer](https://testnet.bscscan.com/)
- [Web3j Documentation](https://docs.web3j.io/)

### 🇨🇳 中文资源
- [ERC20标准](https://eips.ethereum.org/EIPS/eip-20)
- [EIP-1193规范](https://eips.ethereum.org/EIPS/eip-1193)
- [OpenZeppelin中文文档](https://docs.openzeppelin.com/contracts/)

### 🇺🇸 English Resources
- [ERC20 Standard](https://eips.ethereum.org/EIPS/eip-20)
- [EIP-1193 Specification](https://eips.ethereum.org/EIPS/eip-1193)
- [OpenZeppelin Documentation](https://docs.openzeppelin.com/contracts/)

---

## 📊 项目进度 / Project Progress

```
Phase 1: Basic Wallet Management           ✅ COMPLETED
├── Create/Import/Export Wallet             ✅
└── Secure Storage                          ✅

Phase 2: Multi-Chain & UI                  ✅ COMPLETED
├── Multi-Agent Collaboration               ✅
├── UI Redesign                             ✅
└── Network Selector                        ✅

Phase 3: Blockchain Interaction             ✅ COMPLETED (EP03-EP04)
├── BSC Testnet Integration                 ✅
├── Native Coin Transfer                    ✅
├── ERC20 Contract Deployment               ✅
├── ERC20 Balance Query                     ✅
└── ERC20 Token Transfer                    ✅

Phase 4: Plugin Release                     📋 NEXT (EP05)
└── Publish to Marketplace                  ⏳
```

---

## 💭 核心要点回顾 / Key Takeaways

### 🇨🇳 这一集的重点
```
✅ ETH = Send value | ERC20 = Call function
✅ 钱包 = Contract Interaction工具
✅ Don't write your own ERC20
✅ OpenZeppelin是行业标准
✅ DApp连接钱包 = 授权使用provider
✅ Web3j只是RPC client
```

### 🇺🇸 This Episode's Focus
```
✅ ETH = Send value | ERC20 = Call function
✅ Wallet = Contract Interaction tool
✅ Don't write your own ERC20
✅ OpenZeppelin is industry standard
✅ DApp connect wallet = authorize provider usage
✅ Web3j is just RPC client
```

---

**🎥 YouTube Series:** [AI + IntelliJ Plugin + Web3 (MetaMask Clone)](https://github.com/eastgatedev/metamask-clone)  
**👨‍💻 Author:** 东门Eastgate  
**📅 Last Updated:** 2025-12-28  
**⭐ Star the Repo:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)
