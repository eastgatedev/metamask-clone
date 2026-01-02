package dev.eastgate.metamaskclone.core.network

import dev.eastgate.metamaskclone.core.storage.Network

object PredefinedNetworks {
    val ETHEREUM_MAINNET = Network(
        id = "ETH_MAINNET",
        name = "Ethereum Mainnet",
        rpcUrl = "https://mainnet.infura.io/v3/YOUR_INFURA_KEY",
        chainId = 1,
        symbol = "ETH",
        blockExplorerUrl = "https://etherscan.io",
        isTestnet = false,
        isCustom = false
    )

    val ETHEREUM_SEPOLIA = Network(
        id = "ETH_SEPOLIA",
        name = "Ethereum Sepolia",
        rpcUrl = "https://sepolia.infura.io/v3/YOUR_INFURA_KEY",
        chainId = 11155111,
        symbol = "ETH",
        blockExplorerUrl = "https://sepolia.etherscan.io",
        isTestnet = true,
        isCustom = false
    )

    val BNB_MAINNET = Network(
        id = "BNB_MAINNET",
        name = "BNB Smart Chain",
        rpcUrl = "https://bsc-dataseed.binance.org",
        chainId = 56,
        symbol = "BNB",
        blockExplorerUrl = "https://bscscan.com",
        isTestnet = false,
        isCustom = false
    )

    val BNB_TESTNET = Network(
        id = "BNB_TESTNET",
        name = "BNB Testnet",
        rpcUrl = "https://data-seed-prebsc-1-s1.binance.org:8545",
        chainId = 97,
        symbol = "tBNB",
        blockExplorerUrl = "https://testnet.bscscan.com",
        isTestnet = true,
        isCustom = false
    )

    val POLYGON_MAINNET = Network(
        id = "POLYGON_MAINNET",
        name = "Polygon",
        rpcUrl = "https://polygon-rpc.com",
        chainId = 137,
        symbol = "MATIC",
        blockExplorerUrl = "https://polygonscan.com",
        isTestnet = false,
        isCustom = false
    )

    val POLYGON_TESTNET = Network(
        id = "POLYGON_TESTNET",
        name = "Polygon Mumbai",
        rpcUrl = "https://rpc-mumbai.maticvigil.com",
        chainId = 80001,
        symbol = "MATIC",
        blockExplorerUrl = "https://mumbai.polygonscan.com",
        isTestnet = true,
        isCustom = false
    )

    val ALL_NETWORKS = listOf(
        BNB_TESTNET, // Default first
        BNB_MAINNET,
        ETHEREUM_SEPOLIA,
        ETHEREUM_MAINNET,
        POLYGON_TESTNET,
        POLYGON_MAINNET
    )

    val TESTNET_NETWORKS = ALL_NETWORKS.filter { it.isTestnet }
    val MAINNET_NETWORKS = ALL_NETWORKS.filter { !it.isTestnet }

    val DEFAULT_ENABLED_IDS = listOf("BNB_TESTNET", "ETH_SEPOLIA", "POLYGON_TESTNET")

    fun getNetworkById(id: String): Network? {
        return ALL_NETWORKS.find { it.id == id }
    }

    fun getNetworkByChainId(chainId: Int): Network? {
        return ALL_NETWORKS.find { it.chainId == chainId }
    }

    fun getDefaultEnabledNetworks(): List<Network> {
        return ALL_NETWORKS.filter { it.id in DEFAULT_ENABLED_IDS }
    }

    fun isNetworkEnabled(
        id: String,
        enabledIds: List<String>
    ): Boolean {
        return id in enabledIds
    }
}
