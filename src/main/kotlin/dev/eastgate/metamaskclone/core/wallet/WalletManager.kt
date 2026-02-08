package dev.eastgate.metamaskclone.core.wallet

import com.intellij.openapi.project.Project
import dev.eastgate.metamaskclone.core.storage.ProjectStorage
import dev.eastgate.metamaskclone.models.BlockchainType
import dev.eastgate.metamaskclone.models.Wallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletManager(private val project: Project) {
    companion object {
        const val BITCOIN_CORE_ADDRESS = "BITCOIN_CORE_CONNECTION"

        private val instances = mutableMapOf<Project, WalletManager>()

        fun getInstance(project: Project): WalletManager {
            return instances.getOrPut(project) { WalletManager(project) }
        }
    }

    private val walletGenerator = WalletGenerator()
    private val tronWalletGenerator = TronWalletGenerator()
    private val storage = ProjectStorage.getInstance(project)

    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    val wallets: StateFlow<List<Wallet>> = _wallets.asStateFlow()

    private val _selectedWallet = MutableStateFlow<Wallet?>(null)
    val selectedWallet: StateFlow<Wallet?> = _selectedWallet.asStateFlow()

    init {
        loadWallets()
    }

    private fun loadWallets() {
        _wallets.value = storage.getWallets()
        if (_wallets.value.isNotEmpty() && _selectedWallet.value == null) {
            _selectedWallet.value = _wallets.value.first()
        }
    }

    /**
     * Get wallets filtered by blockchain type.
     */
    fun getWalletsForBlockchainType(blockchainType: BlockchainType): List<Wallet> {
        return _wallets.value.filter { it.blockchainType == blockchainType }
    }

    /**
     * Count wallets by blockchain type (for naming new wallets).
     */
    fun getWalletCountForType(blockchainType: BlockchainType): Int {
        return _wallets.value.count { it.blockchainType == blockchainType }
    }

    /**
     * Select the first wallet for a given blockchain type.
     */
    fun selectFirstWalletForType(blockchainType: BlockchainType) {
        val walletsOfType = getWalletsForBlockchainType(blockchainType)
        _selectedWallet.value = walletsOfType.firstOrNull()
    }

    fun createWallet(
        name: String? = null,
        password: String,
        blockchainType: BlockchainType = BlockchainType.EVM
    ): Wallet {
        if (blockchainType == BlockchainType.BITCOIN) {
            throw IllegalArgumentException("Bitcoin wallets are managed by Bitcoin Core. Use ensureBitcoinCoreWallet() instead.")
        }

        val walletCount = getWalletCountForType(blockchainType)
        val walletName = name ?: when (blockchainType) {
            BlockchainType.EVM -> walletGenerator.generateWalletName(walletCount)
            BlockchainType.TRON -> tronWalletGenerator.generateWalletName(walletCount)
            BlockchainType.BITCOIN -> throw IllegalStateException("Unreachable")
        }

        val wallet = when (blockchainType) {
            BlockchainType.EVM -> walletGenerator.generateNewWallet(walletName, password)
            BlockchainType.TRON -> tronWalletGenerator.generateNewWallet(walletName, password)
            BlockchainType.BITCOIN -> throw IllegalStateException("Unreachable")
        }

        val updatedWallets = _wallets.value + wallet
        _wallets.value = updatedWallets
        storage.saveWallets(updatedWallets)

        // Auto-select if no wallet selected or if it matches the blockchain type
        if (_selectedWallet.value == null ||
            _selectedWallet.value?.blockchainType == blockchainType
        ) {
            _selectedWallet.value = wallet
        }

        return wallet
    }

    fun importWallet(
        privateKey: String,
        name: String? = null,
        password: String,
        blockchainType: BlockchainType = BlockchainType.EVM
    ): Wallet {
        if (blockchainType == BlockchainType.BITCOIN) {
            throw IllegalArgumentException("Bitcoin wallets are managed by Bitcoin Core. Import keys directly in Bitcoin Core.")
        }

        val walletName = name ?: when (blockchainType) {
            BlockchainType.EVM -> "Imported Wallet"
            BlockchainType.TRON -> "Imported TRON Wallet"
            BlockchainType.BITCOIN -> throw IllegalStateException("Unreachable")
        }

        val wallet = when (blockchainType) {
            BlockchainType.EVM -> walletGenerator.importWalletFromPrivateKey(privateKey, walletName, password)
            BlockchainType.TRON -> tronWalletGenerator.importWalletFromPrivateKey(privateKey, walletName, password)
            BlockchainType.BITCOIN -> throw IllegalStateException("Unreachable")
        }

        // Check if wallet already exists (same address AND same blockchain type)
        if (_wallets.value.any {
                it.address.equals(wallet.address, ignoreCase = true) &&
                    it.blockchainType == blockchainType
            }
        ) {
            throw IllegalArgumentException("Wallet with this address already exists")
        }

        val updatedWallets = _wallets.value + wallet
        _wallets.value = updatedWallets
        storage.saveWallets(updatedWallets)

        // Auto-select if no wallet selected or if it matches the blockchain type
        if (_selectedWallet.value == null ||
            _selectedWallet.value?.blockchainType == blockchainType
        ) {
            _selectedWallet.value = wallet
        }

        return wallet
    }

    fun renameWallet(
        address: String,
        newName: String
    ) {
        val updatedWallets = _wallets.value.map { wallet ->
            if (wallet.address.equals(address, ignoreCase = true)) {
                wallet.copy(name = newName)
            } else {
                wallet
            }
        }

        _wallets.value = updatedWallets
        storage.saveWallets(updatedWallets)

        // Update selected wallet if it was renamed
        _selectedWallet.value?.let { selected ->
            if (selected.address.equals(address, ignoreCase = true)) {
                _selectedWallet.value = selected.copy(name = newName)
            }
        }
    }

    fun deleteWallet(address: String) {
        if (address == BITCOIN_CORE_ADDRESS) {
            throw IllegalArgumentException("Cannot delete Bitcoin Core pseudo-wallet")
        }

        val walletToDelete = _wallets.value.find {
            it.address.equals(address, ignoreCase = true)
        }

        val updatedWallets = _wallets.value.filter {
            !it.address.equals(address, ignoreCase = true)
        }

        _wallets.value = updatedWallets
        storage.saveWallets(updatedWallets)

        // Update selected wallet if it was deleted
        _selectedWallet.value?.let { selected ->
            if (selected.address.equals(address, ignoreCase = true)) {
                // Select first wallet of the same blockchain type, or null if none
                val blockchainType = walletToDelete?.blockchainType
                _selectedWallet.value = if (blockchainType != null) {
                    updatedWallets.firstOrNull { it.blockchainType == blockchainType }
                } else {
                    null
                }
            }
        }
    }

    fun selectWallet(address: String) {
        _selectedWallet.value = _wallets.value.find {
            it.address.equals(address, ignoreCase = true)
        }
    }

    fun exportPrivateKey(
        address: String,
        password: String
    ): String {
        if (address == BITCOIN_CORE_ADDRESS) {
            throw IllegalArgumentException("Bitcoin private keys are managed by Bitcoin Core. Export keys using bitcoin-cli.")
        }

        val wallet = _wallets.value.find {
            it.address.equals(address, ignoreCase = true)
        } ?: throw IllegalArgumentException("Wallet not found")

        return when (wallet.blockchainType) {
            BlockchainType.EVM -> walletGenerator.decryptPrivateKey(wallet.encryptedPrivateKey, password)
            BlockchainType.TRON -> tronWalletGenerator.decryptPrivateKey(wallet.encryptedPrivateKey, password)
            BlockchainType.BITCOIN -> throw IllegalArgumentException("Bitcoin private keys are managed by Bitcoin Core")
        }
    }

    fun getWalletByAddress(address: String): Wallet? {
        return _wallets.value.find {
            it.address.equals(address, ignoreCase = true)
        }
    }

    fun hasWallets(): Boolean {
        return _wallets.value.isNotEmpty()
    }

    // ==================== Bitcoin Core Pseudo-Wallet ====================

    /**
     * Ensure a Bitcoin Core pseudo-wallet exists.
     * Bitcoin Core manages keys server-side, so we create a placeholder wallet
     * to fit existing architecture.
     */
    fun ensureBitcoinCoreWallet(): Wallet {
        val existing = _wallets.value.find {
            it.address == BITCOIN_CORE_ADDRESS && it.blockchainType == BlockchainType.BITCOIN
        }
        if (existing != null) return existing

        val wallet = Wallet(
            name = "Bitcoin Core Wallet",
            address = BITCOIN_CORE_ADDRESS,
            encryptedPrivateKey = "",
            publicKey = "",
            blockchainType = BlockchainType.BITCOIN
        )

        val updatedWallets = _wallets.value + wallet
        _wallets.value = updatedWallets
        storage.saveWallets(updatedWallets)

        return wallet
    }

    /**
     * Check if a wallet is the Bitcoin Core pseudo-wallet.
     */
    fun isBitcoinCoreWallet(wallet: Wallet): Boolean {
        return wallet.address == BITCOIN_CORE_ADDRESS && wallet.blockchainType == BlockchainType.BITCOIN
    }
}
