# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**MetaMask Clone IntelliJ Plugin** - EVM blockchain wallet management for IntelliJ IDEA.

- **Kotlin:** 2.2.10
- **JDK:** 21
- **IntelliJ Platform:** 2025.1
- **Package:** `dev.eastgate.metamaskclone`
- **Plugin ID:** `dev.eastgate.wallet.metamaskclone`

## Commands

```bash
./gradlew build          # Build the project
./gradlew clean build    # Clean build
./gradlew buildPlugin    # Build distributable plugin zip
./gradlew verifyPlugin   # Verify plugin compatibility (REQUIRED before Marketplace submission)
```

**Avoid** `./gradlew runIde` - resource intensive. Use `./gradlew build` to verify compilation.

## Key Warnings

**CRITICAL - No Internal API:** Do NOT use any IntelliJ Internal API (`@ApiStatus.Internal`). JetBrains Marketplace will REJECT plugins that use internal APIs. Always run `./gradlew verifyPlugin` before submission to check for:
- Internal API usages
- Deprecated API usages
- Experimental API usages

**Kotlin JVM Default:** The project uses `-Xjvm-default=all` compiler option to avoid generating bridge methods that trigger internal API warnings from interface defaults (e.g., `ToolWindowFactory`).

**ToolWindowFactory:** Do NOT override deprecated methods. Use plugin.xml attributes instead:
- `doNotActivateOnStart="true"` instead of `isDoNotActivateOnStart()`
- `anchor="right"` instead of `getAnchor()`
- `icon="..."` instead of `getIcon()`

**Coroutines:** Do NOT add explicit coroutine dependencies - they're provided by IntelliJ Platform. Adding them causes `CoroutineExceptionHandler` conflicts.

**BouncyCastle:** BitcoinJ must exclude BouncyCastle to avoid conflicts:
```kotlin
implementation("org.bitcoinj:bitcoinj-core:0.16.2") {
    exclude(group = "org.bouncycastle")
}
```

## Architecture

- **WalletManager** - Singleton per project, manages wallets with StateFlow
- **NetworkManager** - Manages EVM network selection
- **BlockchainService** - Web3j integration for blockchain calls
- **ProjectStorage** - PersistentStateComponent for encrypted data persistence
- **MetaMaskToolWindow** - Main UI with coroutine-based state observation
