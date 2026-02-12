# EP10 - Explorer & Indexing：钱包系统如何获取交易历史 / How Wallet Systems Obtain Transaction History

> **项目仓库 / Project Repository:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)  
> **当前进度 / Current Progress:** Phase 1-5 ✅ Completed | Phase 6 🚧 In Progress

---

## 📋 概述 / Overview

### 🇨🇳 中文
本集从 wallet system / backend 的角度出发，从 node / RPC 视角切换到 explorer / indexing 视角。核心问题是：当 node 无法直接提供可用的 transaction history 时，钱包系统是如何补足这一层的。本集不包含 Bitcoin — Bitcoin 的 transaction history 已在 EP09 中通过 Bitcoin Core（wallet + node）完整呈现。

### 🇺🇸 English
This episode approaches from wallet system / backend perspective, shifting from node / RPC perspective to explorer / indexing perspective. The core question is: when the node can't directly provide usable transaction history, how does the wallet system fill this gap. Bitcoin is not included — Bitcoin transaction history was fully covered in EP09 via Bitcoin Core (wallet + node).

---

## 🎯 学习目标 / Learning Objectives

### 🇨🇳 本集目标
- [ ] 理解为什么区块链 raw data 不能直接用于钱包展示
- [ ] 理解 Explorer / Indexing 是链外系统
- [ ] 理解 JSON-RPC 为什么不适合查询交易历史
- [ ] 理解 Wallet System 中的 Transaction 抽象
- [ ] 使用 Etherscan v2 API 查询 EVM 交易历史（native + ERC20）
- [ ] 使用 TronGrid v1 API 查询 TRON 交易历史（TRX + TRC20）
- [ ] 理解 demo 与真实 wallet system 在 infra 层的不同选择

### 🇺🇸 Episode Goals
- [ ] Understand why blockchain raw data can't be directly used for wallet display
- [ ] Understand Explorer / Indexing as off-chain systems
- [ ] Understand why JSON-RPC is not suitable for querying transaction history
- [ ] Understand Transaction abstraction in wallet system
- [ ] Query EVM transaction history using Etherscan v2 API (native + ERC20)
- [ ] Query TRON transaction history using TronGrid v1 API (TRX + TRC20)
- [ ] Understand different infra choices between demo and real wallet systems

---

## 1️⃣ 为什么钱包系统需要 Explorer / Indexing / Why Wallet Systems Need Explorer / Indexing

### 🇨🇳 中文

区块链本身只提供 raw transaction data。Transaction 本质上只是账本记录（ledger entry）。

Raw data 本身：
- 没有可直接使用的顺序语义
- 没有明确的 in / out 方向
- 没有 native / token 的区分

钱包系统需要的是：
- 可展示的交易列表
- 明确的资金流向
- 面向用户的交易语义

**结论：钱包系统展示的 transaction history 并不是直接来自 node。**

### 🇺🇸 English

Blockchain itself only provides raw transaction data. Transactions are essentially ledger entries.

Raw data lacks:
- Usable ordering semantics
- Clear in / out direction
- Native / token distinction

Wallet system needs:
- Displayable transaction list
- Clear fund flow direction
- User-facing transaction semantics

**Conclusion: the transaction history shown by wallet system does not come directly from the node.**

---

## 2️⃣ Explorer / Indexing 是链外系统 / Explorer / Indexing as Off-Chain Systems

### 🇨🇳 中文

Explorer / indexing system 不属于区块链本身，它们运行在链外。

核心职责：
- 解析 raw transaction
- 重建交易顺序
- 解释 in / out 方向
- 还原 token transfer 语义

提供的是 **解释过的数据**，而不是共识的一部分。

### 🇺🇸 English

Explorer / indexing systems don't belong to the blockchain itself — they run off-chain.

Core responsibilities:
- Parse raw transactions
- Reconstruct transaction ordering
- Interpret in / out direction
- Restore token transfer semantics

They provide **interpreted data**, not part of consensus.

---

## 3️⃣ 为什么 JSON-RPC 不适合查询交易历史 / Why JSON-RPC Is Not Suitable for Transaction History

### 🇨🇳 中文

JSON-RPC 的设计目标是：
- 查询 block
- 查询 transaction by hash
- 查询 account / address 状态

JSON-RPC 不提供：
- wallet-level transaction history
- 可直接展示的交易列表

即使通过多次 RPC 拼接：成本高、复杂度高、不适合钱包系统直接使用。

### 🇺🇸 English

JSON-RPC is designed for:
- Querying blocks
- Querying transactions by hash
- Querying account / address state

JSON-RPC does not provide:
- Wallet-level transaction history
- Displayable transaction lists

Even stitching together multiple RPC calls: high cost, high complexity, not suitable for direct wallet system use.

---

## 4️⃣ Wallet System 中的 Transaction 抽象 / Transaction Abstraction in Wallet System

### 🇨🇳 中文

在 wallet system 的抽象层：
- 不区分链类型
- 不关心底层数据来源

Wallet system 只关心：
- in / out
- amount
- asset type（native / token）
- 时间顺序

Explorer / indexing 的角色：把 ledger 转换成 wallet system 可用的 transaction 抽象。

### 🇺🇸 English

At wallet system's abstraction layer:
- Chain type doesn't matter
- Underlying data source doesn't matter

Wallet system only cares about:
- in / out
- amount
- asset type (native / token)
- chronological order

Explorer / indexing role: convert ledger into transaction abstractions usable by wallet system.

---

## 5️⃣ EVM Explorer：Etherscan / EVM Explorer: Etherscan

### 🇨🇳 中文

- Etherscan 提供 EVM indexing API
- 覆盖所有 EVM 链（包含 testnet），通过 chain id 区分不同网络
- 统一 endpoint：`https://api.etherscan.io/v2/api`
- 在 wallet system 视角下：Etherscan 不是 Ethereum API，而是 **EVM Explorer / Indexing API**
- 需要注册免费 API key 才能使用所有 API
- BSC mainnet / testnet 需要付费 API key
- Demo 使用 Ethereum Sepolia testnet，chain id = `11155111`（免费 API key 即可使用）

### 🇺🇸 English

- Etherscan provides EVM indexing API
- Covers all EVM chains (including testnets), differentiated by chain id
- Single endpoint: `https://api.etherscan.io/v2/api`
- From wallet system perspective: Etherscan is not an Ethereum API — it's an **EVM Explorer / Indexing API**
- Requires free API key registration to use all APIs
- BSC mainnet / testnet requires paid API key
- Demo uses Ethereum Sepolia testnet, chain id = `11155111` (free API key is sufficient)

📎 参考 / Reference: https://docs.etherscan.io/supported-chains

---

## 6️⃣ EVM 交易历史 / EVM Transaction History — Code & Test Cases

### EvmScanClient（新 class，OkHttp）

```kotlin
class EvmScanClient(
    private val chainId: Int = 11155111, // Ethereum Sepolia Testnet
    private val apiKey: String = "", // Free API key from etherscan.io
    private val baseUrl: String = "https://api.etherscan.io/v2/api",
    private val client: OkHttpClient = OkHttpClient()
) {
    // Test Case 1 - query native coin transaction history
    fun getNativeTransactions(address: String): String {
        val url = "$baseUrl?chainid=$chainId&module=account&action=txlist" +
                "&address=$address&startblock=0&endblock=99999999&sort=desc" +
                "&apikey=$apiKey"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            return response.body?.string() ?: throw Exception("Empty response")
        }
    }

    // Test Case 2 - query erc20 token transaction history
    fun getErc20Transactions(address: String): String {
        val url = "$baseUrl?chainid=$chainId&module=account&action=tokentx" +
                "&address=$address&startblock=0&endblock=99999999&sort=desc" +
                "&apikey=$apiKey"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            return response.body?.string() ?: throw Exception("Empty response")
        }
    }
}
```

### 接口说明 / Endpoint Reference

| Action | 用途 / Purpose | API Reference |
|--------|---------------|---------------|
| `txlist` | Native coin（ETH / BNB / etc）的 in / out 记录 | [docs.etherscan.io/api-reference/endpoint/txlist](https://docs.etherscan.io/api-reference/endpoint/txlist) |
| `tokentx` | ERC20 token 的 in / out 记录 | [docs.etherscan.io/api-reference/endpoint/tokentx](https://docs.etherscan.io/api-reference/endpoint/tokentx) |

> 📌 本集只关注 native coin 与 ERC20，不讨论 internal transaction 或 event log。  
> 📌 This episode only covers native coin and ERC20. Internal transactions and event logs are not discussed.

---

## 7️⃣ TRON Explorer：TronGrid / TRON Explorer: TronGrid

### 🇨🇳 中文

- TronGrid v1 API = TRON 的 explorer / indexing 接口
- 在 wallet system 视角下，与 Etherscan 扮演相同角色
- 提供解释过的交易数据
- Demo 使用 Shasta testnet

### 🇺🇸 English

- TronGrid v1 API = TRON's explorer / indexing interface
- From wallet system perspective, plays the same role as Etherscan
- Provides interpreted transaction data
- Demo uses Shasta testnet

---

## 8️⃣ TRON 交易历史 / TRON Transaction History — Code & Test Cases

### 在现有 TronGrpcClient 上扩展 / Expand on Existing TronGrpcClient

```kotlin
// Expand on existing TronGrpcClient

// Test Case 3 - query trx transaction history
fun getTrxTransactions(address: String): String {
    val url = "https://api.shasta.trongrid.io/v1/accounts/$address/transactions"
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
        return response.body?.string() ?: throw Exception("Empty response")
    }
}

// Test Case 4 - query trc20 token transaction history
fun getTrc20Transactions(address: String): String {
    val url = "https://api.shasta.trongrid.io/v1/accounts/$address/transactions/trc20"
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
        return response.body?.string() ?: throw Exception("Empty response")
    }
}
```

### 接口说明 / Endpoint Reference

| Endpoint | 用途 / Purpose | API Reference |
|----------|---------------|---------------|
| `/v1/accounts/{address}/transactions` | TRX 的 in / out 交易记录 | [developers.tron.network/reference/get-transaction-info-by-account-address](https://developers.tron.network/reference/get-transaction-info-by-account-address) |
| `/v1/accounts/{address}/transactions/trc20` | TRC20 token 的 in / out 交易记录 | [developers.tron.network/reference/get-trc20-transaction-info-by-account-address](https://developers.tron.network/reference/get-trc20-transaction-info-by-account-address) |

> 📌 只关注钱包展示所需的最小信息集。  
> 📌 Only the minimum information set needed for wallet display.

---

## 9️⃣ 为什么真实 Wallet System 不直接使用这些 API / Why Real Wallet Systems Don't Use These APIs Directly

### 🇨🇳 中文

- Etherscan / TronGrid 是 wallet-centric API，以 address 为查询单位
- 当 wallet 数量很大时：查询成本高、不具备可扩展性
- 真实 wallet system 会采用 block-level scanning

### 🇺🇸 English

- Etherscan / TronGrid are wallet-centric APIs, querying by address
- When wallet count is large: high query cost, not scalable
- Real wallet systems adopt block-level scanning

---

## 🔟 为什么 MetaMask Clone 使用这些 API / Why MetaMask Clone Uses These APIs

### 🇨🇳 中文

MetaMask Clone 的目标是：
- 教学
- demo
- 验证 wallet system 抽象层

使用 Etherscan / TronGrid：
- 足够说明 transaction history 的来源
- 不引入复杂 infra

补充说明：
- Etherscan 需要注册免费 API key（BSC 需要付费 key，Sepolia 免费 key 即可）
- TronGrid 不需要 API key
- rate limit 对 demo 场景足够

### 🇺🇸 English

MetaMask Clone's goal:
- Education
- Demo
- Validating wallet system abstraction layer

Using Etherscan / TronGrid:
- Sufficiently demonstrates transaction history sources
- No complex infra needed

Additional notes:
- Etherscan requires free API key registration (BSC requires paid key, Sepolia works with free key)
- TronGrid does not require API key
- Rate limits are sufficient for demo scenarios

---

## 1️⃣1️⃣ Wallet System 的统一抽象回顾（概念层，不含 Bitcoin）/ Unified Abstraction Review (Conceptual, Excluding Bitcoin)

### 🇨🇳 中文

在 wallet system 的对外接口中：
- EVM 与 TRON 的交易历史被统一抽象为 transaction list
- Explorer / indexing 负责：把 ledger 转换成 wallet system 可用的数据
- Wallet system 对外接口一致，数据来源的差异在内部处理
- Bitcoin 已在 EP09 完成闭环，不在本集范围内

### 🇺🇸 English

In wallet system's external interface:
- EVM and TRON transaction histories are unified as transaction list
- Explorer / indexing is responsible for: converting ledger into wallet system-usable data
- Wallet system's external interface remains consistent; data source differences are handled internally
- Bitcoin was fully closed in EP09, not in this episode's scope

---

## 1️⃣2️⃣ 本集总结 / Episode Summary

### 🇨🇳 中文
- 区块链本身不提供可用的交易历史
- Explorer / indexing 是钱包系统的重要组成部分
- 第三方 API 是 demo 场景下的合理取舍
- 真实 wallet system 与 demo 在 infra 层做不同选择

### 🇺🇸 English
- Blockchain itself does not provide usable transaction history
- Explorer / indexing is an important part of wallet systems
- Third-party APIs are a reasonable trade-off for demo scenarios
- Real wallet systems and demos make different choices at the infra layer

---

## 1️⃣3️⃣ 下一集预告 / Next Episode Preview

### 🇨🇳 EP11：系列回顾与收尾（最后一集）
- 回顾 MetaMask Clone 系列的覆盖范围
- 说明系列的技术与系统边界
- 从系统与责任的角度收尾

### 🇺🇸 EP11: Series Review & Wrap-up (Final Episode)
- Review MetaMask Clone series coverage
- Define the series' technical and system boundaries
- Wrap up from a system and responsibility perspective

---

## 💭 核心要点回顾 / Key Takeaways

### 🇨🇳 这一集的重点
```
✅ 区块链 raw data 不能直接用于钱包展示
✅ Explorer / indexing 是链外系统，提供解释过的数据
✅ JSON-RPC 不适合查询交易历史
✅ Wallet system 的 transaction 抽象：in/out、amount、asset type、时间顺序
✅ Etherscan = EVM Explorer / Indexing API（不是 Ethereum API）
✅ TronGrid = TRON Explorer / Indexing API
✅ 统一 endpoint + chain id 覆盖所有 EVM 链
✅ Etherscan 需要免费 API key（BSC 需付费，Sepolia 免费即可）
✅ 真实 wallet system 使用 block-level scanning
✅ Demo 场景使用第三方 API 是合理取舍
```

### 🇺🇸 This Episode's Focus
```
✅ Blockchain raw data can't be directly used for wallet display
✅ Explorer / indexing are off-chain systems providing interpreted data
✅ JSON-RPC is not suitable for transaction history queries
✅ Wallet system transaction abstraction: in/out, amount, asset type, chronological order
✅ Etherscan = EVM Explorer / Indexing API (not Ethereum API)
✅ TronGrid = TRON Explorer / Indexing API
✅ Single endpoint + chain id covers all EVM chains
✅ Etherscan requires free API key (BSC needs paid, Sepolia works with free)
✅ Real wallet systems use block-level scanning
✅ Third-party APIs are a reasonable trade-off for demo scenarios
```

---

## 📊 项目进度 / Project Progress

```
Phase 1-3: EVM Ecosystem                ✅ COMPLETED
├── Wallet Management                    ✅
├── BSC Integration                      ✅
└── ERC20 Token                          ✅

Phase 4: TRON Ecosystem                 ✅ COMPLETED
├── TRON Wallet Structure                ✅
├── TRX Transfer                         ✅
└── TRC20 Token                          ✅

Phase 5: Bitcoin Integration            ✅ COMPLETED
├── Bitcoin Core Setup                   ✅
├── Address Generation                   ✅
├── Balance Query                        ✅
├── BTC Transfer                         ✅
└── Unified Abstraction Layer            ✅

Phase 6: Explorer & Indexing            ✅ COMPLETED (EP10)
├── Etherscan (EVM Indexing)             ✅
├── TronGrid (TRON Indexing)             ✅
├── Native + Token History               ✅
└── Unified Transaction Abstraction      ✅

Phase 7: Series Wrap-up                 📋 NEXT (EP11)
```

---

**🎥 YouTube Series:** [AI + IntelliJ Plugin + Web3 (MetaMask Clone)](https://github.com/eastgatedev/metamask-clone)  
**👨‍💻 Author:** 东门Eastgate  
**📅 Last Updated:** 2026-02-10  
**⭐ Star the Repo:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)
