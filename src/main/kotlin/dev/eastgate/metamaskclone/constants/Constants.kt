package dev.eastgate.metamaskclone.constants

object Constants {
    const val PLUGIN_NAME = "MetaMask Clone"
    const val TOOL_WINDOW_ID = "MetaMask Clone"

    // Default Networks
    const val DEFAULT_NETWORK = "BNB_TESTNET"

    // UI Constants
    const val WALLET_LIST_WIDTH = 250
    const val MIN_WINDOW_WIDTH = 400
    const val MIN_WINDOW_HEIGHT = 600

    // Security
    const val MIN_PASSWORD_LENGTH = 8
    const val PRIVATE_KEY_LENGTH = 64

    // Transaction
    const val DEFAULT_GAS_LIMIT = 21000L
    const val DEFAULT_GAS_PRICE_GWEI = 5L

    // API Endpoints for price data
    const val COINGECKO_API_BASE = "https://api.coingecko.com/api/v3"
    const val PRICE_UPDATE_INTERVAL_MS = 60000L // 1 minute
}
