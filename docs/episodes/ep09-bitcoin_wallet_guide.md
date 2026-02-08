# EP09 - Bitcoin钱包系统 / Bitcoin Wallet System

> **项目仓库 / Project Repository:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)  
> **当前进度 / Current Progress:** Phase 5 🚧 In Progress

---

## 📋 概述 / Overview

### 🇨🇳 中文
这一集我们进入**Bitcoin**世界。与前面的EVM和TRON不同，Bitcoin使用完全不同的账户模型。我们不会从链的角度出发做Bitcoin教学，而是只从**Wallet System / Backend**角度，看Bitcoin在真实钱包系统里如何被处理。最重要的是理解：在钱包系统的抽象层中，Bitcoin、EVM、TRON的差异如何被统一处理。

### 🇺🇸 English  
This episode enters the **Bitcoin** world. Different from previous EVM and TRON, Bitcoin uses a completely different account model. We won't teach Bitcoin from chain perspective, but only from **Wallet System / Backend** perspective, looking at how Bitcoin is actually handled in real wallet systems. Most importantly, understanding: how differences between Bitcoin, EVM, and TRON are unified in wallet system's abstraction layer.

---

## 🎯 学习目标 / Learning Objectives

### 🇨🇳 本集目标
- [ ] 理解Bitcoin Core在wallet system中的角色
- [ ] 掌握regtest的使用场景
- [ ] 理解config-driven system概念
- [ ] 理解Bitcoin的multiple address模型
- [ ] 使用OkHttp调用Bitcoin Core RPC
- [ ] 实现地址生成、余额查询、BTC转账
- [ ] 理解钱包系统的统一抽象层
- [ ] 掌握BTC/EVM/TRON在wallet中的一致性

### 🇺🇸 Episode Goals
- [ ] Understand Bitcoin Core's role in wallet system
- [ ] Master regtest usage scenarios
- [ ] Understand config-driven system concept
- [ ] Understand Bitcoin's multiple address model
- [ ] Use OkHttp to call Bitcoin Core RPC
- [ ] Implement address generation, balance query, BTC transfer
- [ ] Understand wallet system's unified abstraction layer
- [ ] Master BTC/EVM/TRON consistency in wallet

---

## 🎬 本集定位 / Episode Positioning

### 🇨🇳 进入Bitcoin世界
在前面几集，我们已经看过：
- EVM世界的account-based wallet
- TRON的运行期行为
- 合约型资产的处理方式

本集进入**Bitcoin**，但和前面一样：

### 🇺🇸 Entering Bitcoin World
In previous episodes, we've seen:
- EVM world's account-based wallet
- TRON's runtime behavior
- Contract-based asset handling

This episode enters **Bitcoin**, but same as before:

```
🇨🇳 不从链的角度出发
只从wallet system / backend角度
看Bitcoin在真实钱包系统里如何被处理

🇺🇸 Not from chain perspective
Only from wallet system / backend perspective
Looking at how Bitcoin is handled in real wallet systems
```

---

## 🔧 为什么使用Bitcoin Core / Why Use Bitcoin Core

### 🇨🇳 Bitcoin Core的系统定位
在Bitcoin世界里，如果要做真实的钱包系统，几乎绕不开：**Bitcoin Core**

### 🇺🇸 Bitcoin Core's System Positioning
In Bitcoin world, for real wallet systems, almost unavoidable: **Bitcoin Core**

### Bitcoin Core在Wallet System中的角色 / Bitcoin Core's Role in Wallet System

| 功能 / Function | 🇨🇳 说明 | 🇺🇸 Description |
|----------------|---------|-----------------|
| **查询链上状态 / Query Chain State** | 获取区块、交易信息 | Get block and transaction info |
| **构建交易 / Build Transaction** | 创建transaction | Create transactions |
| **广播交易 / Broadcast Transaction** | 发送到网络 | Broadcast to network |
| **查询余额 / Query Balance** | 获取wallet balance | Get wallet balance |
| **交易记录 / Transaction History** | 获取transaction history | Get transaction history |

### 与EVM/TRON的对比 / Comparison with EVM/TRON

```
🇨🇳 关键差异：
- EVM/TRON: 通过RPC服务（Infura/TronGrid）
- Bitcoin: 钱包系统直接依赖节点理解链状态

不是单纯调用RPC服务，
而是Bitcoin Core成为钱包系统的一部分

🇺🇸 Key Difference:
- EVM/TRON: Through RPC services (Infura/TronGrid)
- Bitcoin: Wallet system directly depends on node to understand chain state

Not simply calling RPC service,
But Bitcoin Core becomes part of wallet system
```

---

## 🧪 为什么使用regtest / Why Use regtest

### 🇨🇳 regtest的特性
本集使用**regtest**模式，而不是testnet：

### 🇺🇸 regtest Characteristics
This episode uses **regtest** mode, not testnet:

| 特性 / Feature | regtest | testnet |
|---------------|---------|---------|
| **运行环境 / Environment** | 本地完全可控 / Fully local controllable | 公共测试网络 / Public test network |
| **区块生成 / Block Generation** | 按需生成 / On-demand generation | 自然出块 / Natural block production |
| **适用场景 / Use Case** | 开发测试 / Development testing | 接近真实环境 / Close to real environment |
| **钱包验证 / Wallet Verification** | 理想环境 / Ideal environment | 依赖网络状态 / Depends on network state |

### 为什么选择regtest / Why Choose regtest

```
🇨🇳 regtest更适合用来验证：
- 钱包行为
- 余额变化
- 交易流程

完全可控的环境，适合wallet system测试

与之前对比：
- EVM/TRON: 使用testnet（公共环境）
- Bitcoin: 使用regtest（本地可控）

🇺🇸 regtest better for verifying:
- Wallet behavior
- Balance changes
- Transaction flow

Fully controllable environment, suitable for wallet system testing

Comparison with previous:
- EVM/TRON: Use testnet (public environment)
- Bitcoin: Use regtest (local controllable)
```

---

## ⚙️ 启动Bitcoin Core：Config-driven System / Start Bitcoin Core: Config-driven System

### 🇨🇳 核心观点
Bitcoin Core本身并不特殊。事实上，**任何区块链的node，本质上都是config-driven system**。

### 🇺🇸 Core Viewpoint
Bitcoin Core itself isn't special. In fact, **any blockchain node is essentially a config-driven system**.

### Config-driven的含义 / Config-driven Meaning

| 配置项 / Config Item | 🇨🇳 决定内容 | 🇺🇸 Determines |
|---------------------|-------------|-----------------|
| **网络类型 / Network Type** | mainnet/testnet/regtest | mainnet/testnet/regtest |
| **RPC行为 / RPC Behavior** | 端口、认证、接口 | Port, auth, interfaces |
| **钱包行为 / Wallet Behavior** | 地址类型、签名方式 | Address type, signing method |

### 视角变化 / Perspective Change

```
🇨🇳 在之前的EVM和TRON集数里：
我们通过BSC、TronGrid这类基础设施
把这些配置隐藏在服务后面

在这一集：
我们第一次直接运行Bitcoin Core
第一次需要显式地面对node的配置本身

这是视角的变化，而不是系统本质的不同

🇺🇸 In previous EVM and TRON episodes:
We used BSC, TronGrid infrastructure
To hide these configs behind services

In this episode:
First time directly running Bitcoin Core
First time explicitly facing node configuration itself

This is perspective change, not system essence difference
```

### 相关资源 / Related Resources
- Bitcoin Core下载 / Bitcoin Core Download: https://bitcoin.org/en/download
- Bitcoin CLI文档 / Bitcoin CLI Documentation: https://chainquery.com/bitcoin-cli

### bitcoin.conf配置示例 / bitcoin.conf Configuration Example

```ini
# 🇨🇳 regtest模式 | 🇺🇸 regtest mode
regtest=1

# 🇨🇳 启用RPC服务器（默认关闭）| 🇺🇸 Enable RPC server (default: off)
server=1

# 🇨🇳 RPC认证 | 🇺🇸 RPC authentication
rpcuser=bitcoinrpc
rpcpassword=your_password_here

```

#### 配置说明 / Configuration Notes

| 配置项 / Config | 默认值 / Default | 说明 / Description |
|----------------|------------------|---------------------|
| `regtest` | 0 | 启用regtest模式 / Enable regtest mode |
| `server` | 0 | 启用JSON-RPC服务器，允许外部程序连接 / Enable JSON-RPC server for external connections |
| `rpcport` | 18443 (regtest) | regtest模式的RPC端口 / RPC port for regtest mode |

#### 启动命令 / Startup Commands

```bash
# 🇨🇳 先进入Bitcoin Core的bin目录 | 🇺🇸 First change to Bitcoin Core's bin directory
cd /path/to/bitcoin-core/bin

# 🇨🇳 方式1：前台运行（方便查看日志）| 🇺🇸 Option 1: Foreground (easy to see logs)
./bitcoind -datadir=../

# 🇨🇳 方式2：GUI模式（方便查看交易和地址）| 🇺🇸 Option 2: GUI mode (easy to check transactions and addresses)
./bitcoin-qt -datadir=../

# 🇨🇳 方式3：后台运行 | 🇺🇸 Option 3: Background daemon
./bitcoind -datadir=../ -daemon
```

#### CLI命令 / CLI Commands

```bash
# 🇨🇳 检查状态 | 🇺🇸 Check status
./bitcoin-cli -datadir=../ getblockchaininfo

# 🇨🇳 创建钱包 | 🇺🇸 Create wallet
./bitcoin-cli -datadir=../ createwallet "defaultwallet"

# 🇨🇳 生成新地址 | 🇺🇸 Generate new address
./bitcoin-cli -datadir=../ getnewaddress

# 🇨🇳 挖矿生成区块（regtest专用）| 🇺🇸 Mine blocks (regtest only)
# 生成101个区块到指定地址（coinbase需要100个确认才能使用）
# Generate 101 blocks to address (coinbase needs 100 confirmations to spend)
./bitcoin-cli -datadir=../ generatetoaddress 101 <your_address>

# ⚠️ 🇨🇳 注意：Coinbase（挖矿奖励）必须等 100 个区块确认后，
#    才会从 Immature 变成 Available，所以至少需要生成 101 个区块。
# ⚠️ 🇺🇸 Note: Coinbase (mining reward) requires 100 block confirmations
#    before it changes from Immature to Available, so generate at least 101 blocks.

# 🇨🇳 查询钱包余额 | 🇺🇸 Get wallet balance
./bitcoin-cli -datadir=../ getbalance

# 🇨🇳 查询钱包详细信息 | 🇺🇸 Get wallet info
./bitcoin-cli -datadir=../ getwalletinfo

# 🇨🇳 停止节点 | 🇺🇸 Stop node
./bitcoin-cli -datadir=../ stop
```

#### RPC调用示例 / RPC Call Examples

```bash
# 🇨🇳 使用curl进行RPC调用（与Kotlin OkHttp相同原理）
# 🇺🇸 RPC calls using curl (same principle as Kotlin OkHttp)

# 🇨🇳 生成新地址 | 🇺🇸 Generate new address
curl --user bitcoinrpc:your_password_here \
  --data-binary '{"jsonrpc":"1.0","id":"1","method":"getnewaddress","params":[]}' \
  -H 'content-type:text/plain;' \
  http://localhost:18443/

# 🇨🇳 查询钱包余额 | 🇺🇸 Get wallet balance
curl --user bitcoinrpc:your_password_here \
  --data-binary '{"jsonrpc":"1.0","id":"1","method":"getbalance","params":[]}' \
  -H 'content-type:text/plain;' \
  http://localhost:18443/

# 🇨🇳 查询钱包详细信息 | 🇺🇸 Get wallet info
curl --user bitcoinrpc:your_password_here \
  --data-binary '{"jsonrpc":"1.0","id":"1","method":"getwalletinfo","params":[]}' \
  -H 'content-type:text/plain;' \
  http://localhost:18443/

# 🇨🇳 发送0.1 BTC | 🇺🇸 Send 0.1 BTC
curl --user bitcoinrpc:your_password_here \
  --data-binary '{"jsonrpc":"1.0","id":"1","method":"sendtoaddress","params":["<recipient_address>", 0.1]}' \
  -H 'content-type:text/plain;' \
  http://localhost:18443/

# 🇨🇳 查询交易记录（最近10笔）| 🇺🇸 List transactions (last 10)
curl --user bitcoinrpc:your_password_here \
  --data-binary '{"jsonrpc":"1.0","id":"1","method":"listtransactions","params":["*", 10]}' \
  -H 'content-type:text/plain;' \
  http://localhost:18443/
```

#### RPC调用（URL内嵌认证）/ RPC Call (URL Embedded Auth)

```bash
# 🇨🇳 认证信息直接放在URL中（更简洁）
# 🇺🇸 Credentials embedded in URL (simpler)

# 🇨🇳 生成新地址 | 🇺🇸 Generate new address
curl --data-binary '{"jsonrpc":"1.0","id":"1","method":"getnewaddress","params":[]}' \
  -H 'content-type:text/plain;' \
  http://bitcoinrpc:your_password_here@localhost:18443/

# 🇨🇳 查询钱包余额 | 🇺🇸 Get wallet balance
curl --data-binary '{"jsonrpc":"1.0","id":"1","method":"getbalance","params":[]}' \
  -H 'content-type:text/plain;' \
  http://bitcoinrpc:your_password_here@localhost:18443/

# 🇨🇳 查询钱包详细信息 | 🇺🇸 Get wallet info
curl --data-binary '{"jsonrpc":"1.0","id":"1","method":"getwalletinfo","params":[]}' \
  -H 'content-type:text/plain;' \
  http://bitcoinrpc:your_password_here@localhost:18443/

# 🇨🇳 发送0.1 BTC | 🇺🇸 Send 0.1 BTC
curl --data-binary '{"jsonrpc":"1.0","id":"1","method":"sendtoaddress","params":["<recipient_address>", 0.1]}' \
  -H 'content-type:text/plain;' \
  http://bitcoinrpc:your_password_here@localhost:18443/

# 🇨🇳 查询交易记录（最近10笔）| 🇺🇸 List transactions (last 10)
curl --data-binary '{"jsonrpc":"1.0","id":"1","method":"listtransactions","params":["*", 10]}' \
  -H 'content-type:text/plain;' \
  http://bitcoinrpc:your_password_here@localhost:18443/
```

```
🇨🇳 RPC调用要点：
- 方式1: --user bitcoinrpc:password (Basic Auth header)
- 方式2: http://user:password@localhost:18443/ (URL内嵌)
- Content-Type: text/plain 或 application/json
- 格式: JSON-RPC 1.0

🇺🇸 RPC Call Key Points:
- Option 1: --user bitcoinrpc:password (Basic Auth header)
- Option 2: http://user:password@localhost:18443/ (URL embedded)
- Content-Type: text/plain or application/json
- Format: JSON-RPC 1.0
```

### regtest RPC端点 / regtest RPC Endpoint

```
🇨🇳 本地regtest模式RPC地址：
http://localhost:18443

🇺🇸 Local regtest mode RPC URL:
http://localhost:18443
```

| 配置项 / Config | 值 / Value |
|----------------|------------|
| **RPC URL** | `http://localhost:18443` |
| **RPC User** | `bitcoinrpc` (在bitcoin.conf中配置 / configured in bitcoin.conf) |
| **RPC Password** | 自定义 / custom (在bitcoin.conf中配置 / configured in bitcoin.conf) |

---

## 🏗️ Bitcoin Wallet的基本模型差异 / Bitcoin Wallet Basic Model Difference

### 🇨🇳 账户模型对比
在进入test case之前，先明确最基础的模型差异：

### 🇺🇸 Account Model Comparison
Before test cases, clarify most basic model difference:

### EVM/TRON vs Bitcoin

| 特性 / Feature | EVM / TRON | Bitcoin |
|---------------|------------|---------|
| **Address数量 / Address Count** | 钱包通常只有一个address / Wallet usually has one address | 一个wallet管理多个address / One wallet manages multiple addresses |
| **Address角色 / Address Role** | Address就是账户本身 / Address is the account itself | Address只是接收单位 / Address is just receiving unit |
| **状态表示 / State Representation** | Address代表完整状态 / Address represents complete state | Wallet代表完整状态 / Wallet represents complete state |
| **余额查询 / Balance Query** | 查询address余额 / Query address balance | 查询wallet余额 / Query wallet balance |

### 系统影响 / System Impact

```
🇨🇳 这个差异会直接影响wallet system的内部实现方式：

EVM/TRON模型：
- 单一address = 账户标识
- Balance与address绑定

Bitcoin模型：
- Wallet包含多个address
- Address只是工具，不是账户
- Balance是wallet级别状态

🇺🇸 This difference directly impacts wallet system's internal implementation:

EVM/TRON Model:
- Single address = account identifier
- Balance tied to address

Bitcoin Model:
- Wallet contains multiple addresses
- Address is just tool, not account
- Balance is wallet-level state
```

---

## 💻 使用OkHttp调用Bitcoin Core RPC / Call Bitcoin Core RPC with OkHttp

### 🇨🇳 RPC调用基础
Bitcoin Core提供JSON-RPC接口，我们使用OkHttp进行调用：

### 🇺🇸 RPC Call Basics
Bitcoin Core provides JSON-RPC interface, we use OkHttp for calls:

### RPC调用工具类 / RPC Call Utility Class

```kotlin
// 🇨🇳 Bitcoin Core RPC客户端 | 🇺🇸 Bitcoin Core RPC Client
class BitcoinRpcClient(
    private val rpcUrl: String = "http://localhost:18443",
    private val rpcUser: String = "bitcoinrpc",
    private val rpcPassword: String
) {
    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()
    
    /**
     * 🇨🇳 调用Bitcoin Core RPC方法
     * 🇺🇸 Call Bitcoin Core RPC method
     */
    suspend fun call(method: String, params: List<Any> = emptyList()): JsonObject {
        return withContext(Dispatchers.IO) {
            val requestBody = buildJsonRpcRequest(method, params)
            val request = Request.Builder()
                .url(rpcUrl)
                .header("Authorization", Credentials.basic(rpcUser, rpcPassword))
                .post(requestBody.toRequestBody(mediaType))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw Exception("Empty response")
            
            val json = Json.parseToJsonElement(responseBody).jsonObject
            
            if (json.containsKey("error") && json["error"] !is JsonNull) {
                throw Exception("RPC Error: ${json["error"]}")
            }
            
            json["result"]?.jsonObject ?: JsonObject(emptyMap())
        }
    }
    
    private fun buildJsonRpcRequest(method: String, params: List<Any>): String {
        return """
            {
                "jsonrpc": "1.0",
                "id": "1",
                "method": "$method",
                "params": ${Json.encodeToString(params)}
            }
        """.trimIndent()
    }
}
```

---

## 📍 Test Case 1：生成接收地址 / Generate Receiving Address

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 生成新的接收地址 | 🇺🇸 Generate new receiving address
suspend fun generateNewAddress(label: String = ""): String {
    val rpcClient = BitcoinRpcClient(rpcPassword = "your_password")
    
    // 🇨🇳 调用getnewaddress RPC方法 | 🇺🇸 Call getnewaddress RPC method
    val result = rpcClient.call("getnewaddress", listOf(label))
    
    return result["address"]?.jsonPrimitive?.content
        ?: throw Exception("Failed to generate address")
}

// 🇨🇳 使用示例 | 🇺🇸 Usage example
val newAddress = generateNewAddress("receiving")
println("New address: $newAddress")
// Output: bcrt1q...
```

### 关键理解 / Key Understanding

```
🇨🇳 这里要注意：
- 这个address只是一个接收入口
- 它本身没有余额
- 也不代表wallet的状态

在Bitcoin的wallet system里：
Address更像是一个工具，而不是账户

🇺🇸 Note here:
- This address is just a receiving entry
- It has no balance itself
- Doesn't represent wallet state

In Bitcoin's wallet system:
Address is more like a tool, not an account
```

---

## 💰 Test Case 2：查询钱包余额 / Query Wallet Balance

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 查询钱包余额 | 🇺🇸 Query wallet balance
suspend fun getWalletBalance(): BigDecimal {
    val rpcClient = BitcoinRpcClient(rpcPassword = "your_password")
    
    // 🇨🇳 调用getbalance RPC方法 | 🇺🇸 Call getbalance RPC method
    val result = rpcClient.call("getbalance")
    
    return result["balance"]?.jsonPrimitive?.content?.toBigDecimal()
        ?: BigDecimal.ZERO
}

// 🇨🇳 使用示例 | 🇺🇸 Usage example
val balance = getWalletBalance()
println("Wallet balance: $balance BTC")
// Output: Wallet balance: 0.00000000 BTC (初始状态 / initial state)
```

### 关键理解 / Key Understanding

```
🇨🇳 初始状态下，wallet balance是0

要注意的是：
- Balance是wallet级别的状态
- 而不是某一个address的属性

这和EVM/TRON的account-based模型，
在概念上是不同的

🇺🇸 Initially, wallet balance is 0

Note that:
- Balance is wallet-level state
- Not a property of any single address

This is conceptually different from
EVM/TRON's account-based model
```

---

## ⛏️ 模拟接收BTC（通过挖矿）/ Simulate Receiving BTC (Through Mining)

### 🇨🇳 regtest环境特性
在regtest环境下，我们可以通过生成区块来模拟接收BTC：

### 🇺🇸 regtest Environment Feature
In regtest environment, we can simulate receiving BTC by generating blocks:

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 生成区块到指定地址 | 🇺🇸 Generate blocks to specified address
suspend fun generateBlocks(address: String, blockCount: Int = 101): List<String> {
    val rpcClient = BitcoinRpcClient(rpcPassword = "your_password")
    
    // 🇨🇳 调用generatetoaddress RPC方法 | 🇺🇸 Call generatetoaddress RPC method
    // 生成101个区块（coinbase奖励需要100个确认才能使用）
    // Generate 101 blocks (coinbase reward needs 100 confirmations to use)
    val result = rpcClient.call(
        "generatetoaddress",
        listOf(blockCount, address)
    )
    
    return result["blockhashes"]?.jsonArray?.map {
        it.jsonPrimitive.content
    } ?: emptyList()
}

// 🇨🇳 使用示例 | 🇺🇸 Usage example
val address = generateNewAddress()
val blocks = generateBlocks(address, 101)
val newBalance = getWalletBalance()
println("After mining, balance: $newBalance BTC")
// Output: After mining, balance: 50.00000000 BTC
```

### 关键理解 / Key Understanding

```
🇨🇳 当新区块被生成之后：
- BTC会进入wallet
- Wallet balance会发生变化

在Bitcoin中：
"接收"并不是一个单独的转账动作
而是随着区块状态变化而发生的结果

🇺🇸 After new blocks are generated:
- BTC enters wallet
- Wallet balance changes

In Bitcoin:
"Receiving" is not a separate transfer action
But result of block state change
```

---

## 💸 Test Case 3：发送BTC / Send BTC

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 发送BTC | 🇺🇸 Send BTC
suspend fun sendBTC(
    toAddress: String,
    amount: BigDecimal
): String {
    val rpcClient = BitcoinRpcClient(rpcPassword = "your_password")
    
    // 🇨🇳 调用sendtoaddress RPC方法 | 🇺🇸 Call sendtoaddress RPC method
    val result = rpcClient.call(
        "sendtoaddress",
        listOf(toAddress, amount.toPlainString())
    )
    
    // 🇨🇳 返回交易哈希 | 🇺🇸 Return transaction hash
    return result["txid"]?.jsonPrimitive?.content
        ?: throw Exception("Failed to send BTC")
}

// 🇨🇳 使用示例 | 🇺🇸 Usage example
val recipientAddress = "bcrt1q..."
val txHash = sendBTC(recipientAddress, BigDecimal("0.1"))
println("Transaction hash: $txHash")
```

### Wallet System的抽象 / Wallet System Abstraction

```
🇨🇳 在wallet system中，这一步被统一抽象为：
一次transaction

和EVM、TRON一样：
- Wallet system职责：构建交易、签名、广播
- Bitcoin内部交易结构（UTXO spend）不在此展开
- 在系统抽象层，处理方式一致

🇺🇸 In wallet system, this step is uniformly abstracted as:
One transaction

Same as EVM, TRON:
- Wallet system responsibility: build, sign, broadcast
- Bitcoin internal structure (UTXO spend) not expanded here
- At system abstraction layer, handling is consistent
```

---

## 📜 Test Case 4：查询交易记录 / Query Transaction History

### 代码实现 / Code Implementation

```kotlin
// 🇨🇳 查询交易历史 | 🇺🇸 Query transaction history
suspend fun getTransactionHistory(count: Int = 10): List<Transaction> {
    val rpcClient = BitcoinRpcClient(rpcPassword = "your_password")
    
    // 🇨🇳 调用listtransactions RPC方法 | 🇺🇸 Call listtransactions RPC method
    val result = rpcClient.call("listtransactions", listOf("*", count))
    
    return result["transactions"]?.jsonArray?.map { tx ->
        Transaction(
            txid = tx["txid"]?.jsonPrimitive?.content ?: "",
            amount = tx["amount"]?.jsonPrimitive?.content?.toBigDecimal() ?: BigDecimal.ZERO,
            confirmations = tx["confirmations"]?.jsonPrimitive?.int ?: 0,
            category = tx["category"]?.jsonPrimitive?.content ?: "",
            time = tx["time"]?.jsonPrimitive?.long ?: 0
        )
    } ?: emptyList()
}

// 🇨🇳 交易数据类 | 🇺🇸 Transaction data class
data class Transaction(
    val txid: String,
    val amount: BigDecimal,
    val confirmations: Int,
    val category: String, // "send" or "receive"
    val time: Long
)

// 🇨🇳 使用示例 | 🇺🇸 Usage example
val history = getTransactionHistory()
history.forEach { tx ->
    println("${tx.category}: ${tx.amount} BTC (${tx.confirmations} confirmations)")
}
```

### 关键理解 / Key Understanding

```
🇨🇳 完成转账后，查询wallet的交易记录

Wallet system通过Bitcoin Core：
获取与当前wallet相关的transaction列表

这一步和account-based链在对外行为上相似
但内部来源不同

🇺🇸 After transfer, query wallet's transaction history

Wallet system through Bitcoin Core:
Get transaction list related to current wallet

This step is similar to account-based chains in external behavior
But internal source different
```

---

## 🎨 钱包系统的统一抽象层 / Wallet System's Unified Abstraction Layer

### 🇨🇳 最重要的核心概念
到这里，我们可以回到wallet system的抽象层来看：

### 🇺🇸 Most Important Core Concept
At this point, we can return to wallet system's abstraction layer:

### 对外接口的统一性 / External Interface Uniformity

| 概念 / Concept | Bitcoin | EVM | TRON | 🇨🇳 统一抽象 | 🇺🇸 Unified Abstraction |
|---------------|---------|-----|------|-------------|----------------------|
| **Transaction** | BTC transfer | ETH/Token transfer | TRX/TRC20 transfer | 都被抽象为transaction | All abstracted as transaction |
| **Balance** | Wallet balance | Address balance | Address balance | 都是wallet-level状态 | All wallet-level state |
| **Address** | Multiple addresses | Single address | Single address | 都提供接收功能 | All provide receiving function |
| **History** | listtransactions | Transaction logs | Transaction logs | 都返回transaction列表 | All return transaction list |

### 内部实现的差异 / Internal Implementation Differences

| 实现层面 / Implementation | Bitcoin | EVM / TRON |
|-------------------------|---------|------------|
| **Address模型 / Address Model** | Multiple address模型 / Multiple address model | Single address模型 / Single address model |
| **状态存储 / State Storage** | UTXO-based | Account-based |
| **余额计算 / Balance Calculation** | UTXO集合 / UTXO set | Account balance |
| **交易构建 / Transaction Building** | Input/Output | From/To/Value |

### 核心观点 / Core Viewpoint

```
🇨🇳 在对外接口上：
Bitcoin、EVM、TRON都被统一抽象为transaction
Balance都是wallet-level状态

差异存在于系统内部：
- Bitcoin使用multiple address模型
- EVM/TRON使用single address模型

但这些差异都是实现细节，
而不是对外接口的一部分

钱包系统关注的不是链本身的复杂性，
而是如何对不同链提供一致的transaction行为

🇺🇸 In external interface:
Bitcoin, EVM, TRON all unified as transaction abstraction
Balance all wallet-level state

Differences exist in system internals:
- Bitcoin uses multiple address model
- EVM/TRON use single address model

But these differences are implementation details,
Not part of external interface

Wallet system focuses not on chain complexity,
But how to provide consistent transaction behavior across chains
```

---

## ✅ 完成检查清单 / Completion Checklist

### 🇨🇳 本集功能确认
- [ ] 理解Bitcoin Core在wallet system中的角色
- [ ] 掌握regtest的使用场景和优势
- [ ] 理解config-driven system概念
- [ ] 理解Bitcoin的multiple address模型
- [ ] 使用OkHttp实现Bitcoin Core RPC调用
- [ ] 实现地址生成功能
- [ ] 实现余额查询功能
- [ ] 实现BTC转账功能
- [ ] 实现交易历史查询
- [ ] 理解钱包系统的统一抽象层
- [ ] 掌握BTC/EVM/TRON的一致性处理

### 🇺🇸 Episode Feature Verification
- [ ] Understand Bitcoin Core's role in wallet system
- [ ] Master regtest usage scenarios and advantages
- [ ] Understand config-driven system concept
- [ ] Understand Bitcoin's multiple address model
- [ ] Implement Bitcoin Core RPC calls with OkHttp
- [ ] Implement address generation functionality
- [ ] Implement balance query functionality
- [ ] Implement BTC transfer functionality
- [ ] Implement transaction history query
- [ ] Understand wallet system's unified abstraction layer
- [ ] Master BTC/EVM/TRON consistent handling

---

## 🚀 下一集预告 / Next Episode Preview

### 🇨🇳 EP10: Explorer & Indexing视角
- 🔍 从node视角切换到**explorer/indexing视角**

### 🇺🇸 EP10: Explorer & Indexing Perspective
- 🔍 Switch from node perspective to **explorer/indexing perspective**

---

## 🔗 相关资源 / Related Resources

### 开发工具 / Development Tools
- [GitHub Repository](https://github.com/eastgatedev/metamask-clone)
- [Bitcoin Core Download](https://bitcoin.org/en/download)
- [Bitcoin CLI Documentation](https://chainquery.com/bitcoin-cli)

### 🇨🇳 中文资源
- [Bitcoin Core文档](https://bitcoin.org/zh_CN/)
- [Bitcoin开发者指南](https://bitcoin.org/zh_CN/developer-guide)

### 🇺🇸 English Resources
- [Bitcoin Core Documentation](https://bitcoin.org/en/)
- [Bitcoin Developer Guide](https://bitcoin.org/en/developer-guide)

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

Phase 5: Bitcoin Integration            ✅ COMPLETED (EP09)
├── Bitcoin Core Setup                   ✅
├── Address Generation                   ✅
├── Balance Query                        ✅
├── BTC Transfer                         ✅
└── Unified Abstraction Layer            ✅

Phase 6: Explorer & Indexing            📋 NEXT (EP10)
```

---

## 💭 核心要点回顾 / Key Takeaways

### 🇨🇳 这一集的重点
```
✅ Bitcoin Core是wallet system的一部分
✅ 任何区块链node本质都是config-driven system
✅ regtest适合本地wallet system测试
✅ Bitcoin使用multiple address模型
✅ Address是工具，不是账户
✅ Balance是wallet级别状态
✅ 统一抽象层：BTC/EVM/TRON在对外接口一致
✅ 差异在内部实现，不在对外接口
```

### 🇺🇸 This Episode's Focus
```
✅ Bitcoin Core is part of wallet system
✅ Any blockchain node is essentially config-driven system
✅ regtest suitable for local wallet system testing
✅ Bitcoin uses multiple address model
✅ Address is tool, not account
✅ Balance is wallet-level state
✅ Unified abstraction: BTC/EVM/TRON consistent in external interface
✅ Differences in internal implementation, not external interface
```

---

**🎥 YouTube Series:** [AI + IntelliJ Plugin + Web3 (MetaMask Clone)](https://github.com/eastgatedev/metamask-clone)  
**👨‍💻 Author:** 东门Eastgate  
**📅 Last Updated:** 2025-09-06  
**⭐ Star the Repo:** [eastgatedev/metamask-clone](https://github.com/eastgatedev/metamask-clone)
