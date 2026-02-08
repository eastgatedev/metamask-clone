# MetaMask Clone - Wallet Architecture Showcase

An **educational IntelliJ IDEA plugin** demonstrating how a wallet-like system can be modeled inside an IDE environment. This is a **technical showcase** focusing on architecture, data flow, and system design.

## Important Notice

**This is NOT a production wallet. No real funds are involved.**

This plugin is **not affiliated with, endorsed by, or connected to** MetaMask or any related organization. The term "MetaMask Clone" is used **solely as a descriptive reference** to indicate architectural inspiration and learning context.

## Purpose

This plugin exists for **learning and demonstration purposes only**, aimed at developers and system architects who want to understand:

- How a wallet-style system can be structured
- How transaction flows are modeled internally
- How backend concepts map to UI components
- How to build non-trivial IntelliJ plugins

## Key Characteristics

- Educational / showcase-oriented plugin
- No real blockchain interaction (simulated or simplified)
- No custody of private keys
- No security guarantees
- No financial functionality

All behaviors are either simulated or simplified for clarity.

## Intended Audience

- Backend engineers
- Plugin developers
- System architects
- Developers learning wallet system architecture
- Developers interested in IntelliJ Plugin development

## Tech Stack

- **Language:** Kotlin 2.2.10
- **Platform:** IntelliJ IDEA Plugin SDK
- **Target JDK:** 21
- **Target IntelliJ:** 2025.1
- **Build Tool:** Gradle

## JetBrains Marketplace

Install the plugin directly from the JetBrains Marketplace:

[MetaMask Clone Showcase on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/29585-metamask-clone-showcase-)

## Video Tutorial & Episode Guides

Learn how to build this plugin step-by-step with our comprehensive video tutorial series:

[Watch the Full Playlist on YouTube](https://youtube.com/playlist?list=PLbqZIOzRvr8mrKmli_WOVogsUTawr9dML&si=6c4ZE7E6WmiuzqaR)

### Episode List

| Episode | Title | Video | Guide |
|---------|-------|-------|-------|
| EP01 | IntelliJ Plugin开发环境搭建 / Plugin Development Setup | [🎬 Watch](https://youtu.be/OmCBY8EkklE) | [📖 Guide](docs/episodes/ep01-intellij_plugin_setup.md) |
| EP02 | 多Agent协作 & UI重新设计 / Multi-Agent Collaboration & UI Redesign | [🎬 Watch](https://youtu.be/XGGEhTAuiWQ) | [📖 Guide](docs/episodes/ep02-bsc_testnet_guide.md) |
| EP03 | 从后端工程师视角理解EVM Transaction / EVM Transactions from Backend Perspective | [🎬 Watch](https://youtu.be/SGxM09HeiPk) | [📖 Guide](docs/episodes/ep03-evm_transaction_guide.md) |
| EP04 | 从Native Coin到ERC20 Token / From Native Coin to ERC20 | [🎬 Watch](https://youtu.be/b3cxsHu1Dm4) | [📖 Guide](docs/episodes/ep04-erc20_token_guide.md) |
| EP05 | 发布到IntelliJ Plugin Marketplace / Publish to Marketplace | [🎬 Watch](https://youtu.be/KZtfq0mpnuw) | [📖 Guide](docs/episodes/ep05-plugin_marketplace.md) |
| EP06 | TRON钱包系统 / TRON Wallet System | [🎬 Watch](https://youtu.be/7KM0kCrmv28) | [📖 Guide](docs/episodes/ep06-tron_wallet_guide.md) |
| EP07 | TRON Runtime：TRX转账 / TRON Runtime: TRX Transfer | [🎬 Watch](https://youtu.be/vSEKXA5KOVg) | [📖 Guide](docs/episodes/ep07-trx_transfer_guide.md) |
| EP08 | TRC20 钱包运行期 / TRC20 Wallet Runtime: Contract-Based Payment Execution | [🎬 Watch](https://youtu.be/amakBnKsmQ8) | [📖 Guide](docs/episodes/ep08-trc20_runtime_guide.md) |
| EP09 | Bitcoin钱包系统 / Bitcoin Wallet System | [🎬 Watch](https://youtu.be/ulAXb_hQmjQ) | [📖 Guide](docs/episodes/ep09-bitcoin_wallet_guide.md) |

## Getting Started

### Prerequisites
- IntelliJ IDEA 2025.1 or later
- JDK 21
- Gradle 8.x

### Build

```bash
git clone https://github.com/eastgatedev/metamask-clone.git
cd metamask-clone
./gradlew build
```

### Run in Test IDE

```bash
./gradlew runIde
```

## Project Structure

```
metamask-clone/
├── src/main/kotlin/dev/eastgate/metamaskclone/
│   ├── core/
│   │   ├── wallet/           # Wallet management logic
│   │   ├── network/          # Network management
│   │   ├── blockchain/       # Blockchain service (simulated)
│   │   └── storage/          # Data persistence & encryption
│   ├── models/               # Data models
│   ├── ui/
│   │   ├── panels/           # UI panels
│   │   └── dialogs/          # Dialogs
│   ├── settings/             # Plugin settings
│   └── utils/                # Utility classes
├── src/main/resources/
│   └── META-INF/
│       └── plugin.xml        # Plugin configuration
└── build.gradle.kts          # Build configuration
```

## Disclaimer

This project is provided **as-is** for educational purposes only.

- It should **not** be used for managing real assets
- The author assumes **no responsibility** for misuse
- This is part of an educational effort to explain wallet system design from a backend and architectural perspective

## License

This project is for educational purposes and demonstration only.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Contact

For questions about the tutorial or project, please open an issue on GitHub.

---

**Summary:** Educational / demo plugin. No misleading claims. No real asset handling. Clear non-affiliation disclaimer.
