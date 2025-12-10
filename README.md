# MetaMask Clone - IntelliJ IDEA Plugin

A demonstration project showcasing how to build an EVM blockchain wallet management plugin for IntelliJ IDEA, inspired by MetaMask functionality.

## 🎯 Overview

This is a **MetaMask Clone** built as an IntelliJ IDEA plugin using **Kotlin**. It provides blockchain wallet management capabilities directly within your IDE, demonstrating how to integrate Web3 functionality into development tools.

## 🛠️ Tech Stack

- **Language:** Kotlin 2.2.10
- **Platform:** IntelliJ IDEA Plugin SDK
- **Target JDK:** 21
- **Target IntelliJ:** 2025.1
- **Build Tool:** Gradle
- **Blockchain:** Web3j for EVM interaction

## 📺 Video Tutorial

🎥 **YouTube Tutorial Available!** 

Learn how to build this plugin step-by-step with our comprehensive video tutorial series.

[Watch the Tutorial Series](https://youtube.com/playlist?list=PLbqZIOzRvr8mrKmli_WOVogsUTawr9dML&si=6c4ZE7E6WmiuzqaR)

## ✨ Features

### Phase 1 - Basic Wallet Management ✅
- ✅ Create new wallets
- ✅ Import existing wallets via private key
- ✅ Export private keys (password-protected)
- ✅ Secure wallet storage with AES-256 encryption
- ✅ Project-level data persistence
- ✅ Clean, professional UI integrated as IntelliJ tool window

### Phase 2 - Network Management ✅
- ✅ Network selector bar with current network display
- ✅ Support for multiple EVM networks (Ethereum, BNB Chain, Polygon)
- ✅ Switch between Mainnet and Testnet networks
- ✅ Add custom RPC networks (supports localhost for development)
- ✅ Enable/disable predefined networks
- ✅ BNB Testnet as default network
- ✅ Network selection persists across IDE restarts

### Phase 3 - UI Components ✅ (UI Ready, Blockchain Integration Pending)
- ✅ Wallet selector dropdown with quick wallet switching
- ✅ Balance display panel (placeholder - blockchain integration pending)
- ✅ Send/Receive action buttons
- ✅ Token list with add custom token support
- ✅ Tokens/Activity tab navigation
- ✅ Send transaction dialog (UI only)
- ✅ Receive dialog with QR code generation
- ✅ Add token dialog

### Upcoming Features
- 🔜 Blockchain integration (fetch real balances)
- 🔜 Send/receive transactions (actual blockchain calls)
- 🔜 Token balance fetching
- 🔜 Transaction history
- 🔜 Mnemonic phrase support (BIP39)
- 🔜 Smart contract interaction

## 🚀 Getting Started

### Prerequisites
- IntelliJ IDEA 2025.1 or later
- JDK 21
- Gradle 8.x

### Installation

1. Clone the repository:
```bash
git clone https://github.com/eastgatedev/metamask-clone.git
cd metamask-clone
```

2. Build the plugin:
```bash
./gradlew build
```

3. Run the plugin in a test IntelliJ instance:
```bash
./gradlew runIde
```

### Usage

1. After installation, find the **MetaMask Clone** tool window on the right side of IntelliJ IDEA
2. **Network Selection**: Click the network bar at the top to switch between networks (BNB Testnet is default)
3. **Wallet Management**: Click the wallet dropdown to:
   - Create a new wallet
   - Import an existing wallet using a private key
   - Switch between wallets
   - Export private key (password required)
   - Delete wallet
4. **Send/Receive**: Use the Send and Receive buttons for transaction dialogs
5. **Token Management**: Go to the Tokens tab to add and manage custom tokens
6. **Custom Networks**: Add custom RPC networks including localhost for smart contract development

## 🏗️ Project Structure

```
metamask-clone/
├── src/main/kotlin/dev/eastgate/metamaskclone/
│   ├── core/
│   │   ├── wallet/           # Wallet management logic
│   │   ├── network/          # Network management (NetworkManager, PredefinedNetworks)
│   │   └── storage/          # Data persistence & encryption
│   ├── models/               # Data models (Wallet, Token)
│   ├── ui/
│   │   ├── MetaMaskToolWindow.kt  # Main UI component
│   │   ├── panels/           # UI panels (NetworkSelector, WalletSelector, Balance, Tokens, etc.)
│   │   └── dialogs/          # Dialogs (CreateWallet, Import, Send, Receive, AddToken, etc.)
│   ├── settings/             # Plugin settings
│   └── utils/                # Utility classes
├── src/main/resources/
│   └── META-INF/
│       └── plugin.xml        # Plugin configuration
├── docs/
│   ├── tasks/                # Implementation plans
│   └── ui/                   # UI reference screenshots
└── build.gradle.kts          # Build configuration
```

## 🔧 Development

### Building
```bash
# Clean build
./gradlew clean build

# Build without tests
./gradlew build -x test
```

### Key Dependencies
- **Web3j** - Ethereum blockchain interaction
- **BouncyCastle** - Cryptographic operations
- **BitcoinJ** - HD wallet generation
- **ZXing** - QR code generation
- **Kotlin Coroutines** - Async operations (provided by IntelliJ Platform)

### Supported Networks

| Network | Chain ID | Type |
|---------|----------|------|
| Ethereum Mainnet | 1 | Mainnet |
| Ethereum Sepolia | 11155111 | Testnet |
| BNB Smart Chain | 56 | Mainnet |
| BNB Testnet | 97 | Testnet (Default) |
| Polygon | 137 | Mainnet |
| Polygon Mumbai | 80001 | Testnet |

Custom networks can be added with any RPC URL (including localhost for local development).

## 📝 License

This project is for educational purposes and demonstration only.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Contact

For questions about the tutorial or project, please open an issue on GitHub.

## ⚠️ Disclaimer

This is a demonstration project for educational purposes. It should not be used for managing real cryptocurrency assets without proper security auditing and testing.

---

**Made with ❤️ for the Kotlin & IntelliJ Plugin Development Community**
