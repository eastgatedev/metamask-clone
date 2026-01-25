package dev.eastgate.metamaskclone.core.network

import dev.eastgate.metamaskclone.core.storage.Network
import dev.eastgate.metamaskclone.models.BlockchainType

object PredefinedNetworks {
    val ETHEREUM_MAINNET = Network(
        id = "ETH_MAINNET",
        name = "Ethereum Mainnet",
        rpcUrl = "https://ethereum-rpc.publicnode.com",
        chainId = 1,
        symbol = "ETH",
        blockExplorerUrl = "https://etherscan.io",
        isTestnet = false,
        isCustom = false
    )

    val ETHEREUM_SEPOLIA = Network(
        id = "ETH_SEPOLIA",
        name = "Ethereum Sepolia",
        rpcUrl = "https://ethereum-sepolia-rpc.publicnode.com",
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
        rpcUrl = "https://polygon-bor-rpc.publicnode.com",
        chainId = 137,
        symbol = "POL",
        blockExplorerUrl = "https://polygonscan.com",
        isTestnet = false,
        isCustom = false
    )

    val POLYGON_TESTNET = Network(
        id = "POLYGON_TESTNET",
        name = "Polygon Amoy",
        rpcUrl = "https://polygon-amoy-bor-rpc.publicnode.com",
        chainId = 80002,
        symbol = "POL",
        blockExplorerUrl = "https://amoy.polygonscan.com",
        isTestnet = true,
        isCustom = false
    )

    // TRON Networks (chainId = -1 since TRON doesn't use EVM chainId)
    val TRON_MAINNET = Network(
        id = "TRON_MAINNET",
        name = "TRON Mainnet",
        rpcUrl = "https://api.trongrid.io",
        chainId = -1,
        symbol = "TRX",
        blockExplorerUrl = "https://tronscan.org",
        isTestnet = false,
        isCustom = false,
        blockchainType = BlockchainType.TRON
    )

    val TRON_SHASTA = Network(
        id = "TRON_SHASTA",
        name = "TRON Shasta Testnet",
        rpcUrl = "https://api.shasta.trongrid.io",
        chainId = -1,
        symbol = "TRX",
        blockExplorerUrl = "https://shasta.tronscan.org",
        isTestnet = true,
        isCustom = false,
        blockchainType = BlockchainType.TRON
    )

    val ALL_NETWORKS = listOf(
        BNB_TESTNET, // Default first
        BNB_MAINNET,
        ETHEREUM_SEPOLIA,
        ETHEREUM_MAINNET,
        POLYGON_TESTNET,
        POLYGON_MAINNET,
        TRON_SHASTA,
        TRON_MAINNET
    )

    val TESTNET_NETWORKS = ALL_NETWORKS.filter { it.isTestnet }
    val MAINNET_NETWORKS = ALL_NETWORKS.filter { !it.isTestnet }

    val DEFAULT_ENABLED_IDS = listOf("BNB_TESTNET", "ETH_SEPOLIA", "POLYGON_TESTNET", "TRON_SHASTA")

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

    fun getNetworksByBlockchainType(blockchainType: BlockchainType): List<Network> {
        return ALL_NETWORKS.filter { it.blockchainType == blockchainType }
    }
}
