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

## Video Tutorial

Learn how to build this plugin step-by-step with our comprehensive video tutorial series:

[Watch the Tutorial Series](https://youtube.com/playlist?list=PLbqZIOzRvr8mrKmli_WOVogsUTawr9dML&si=6c4ZE7E6WmiuzqaR)

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
