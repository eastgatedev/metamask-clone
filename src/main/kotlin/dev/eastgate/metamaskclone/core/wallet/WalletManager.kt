package dev.eastgate.metamaskclone.core.wallet

import com.intellij.openapi.project.Project
import dev.eastgate.metamaskclone.core.storage.ProjectStorage
import dev.eastgate.metamaskclone.models.Wallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletManager(private val project: Project) {
    private val walletGenerator = WalletGenerator()
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

    fun createWallet(
        name: String? = null,
        password: String
    ): Wallet {
        val walletName = name ?: walletGenerator.generateWalletName(_wallets.value.size)
        val wallet = walletGenerator.generateNewWallet(walletName, password)

        val updatedWallets = _wallets.value + wallet
        _wallets.value = updatedWallets
        storage.saveWallets(updatedWallets)

        if (_selectedWallet.value == null) {
            _selectedWallet.value = wallet
        }

        return wallet
    }

    fun importWallet(
        privateKey: String,
        name: String? = null,
        password: String
    ): Wallet {
        val walletName = name ?: walletGenerator.generateWalletName(_wallets.value.size)

        // Check if wallet already exists
        val existingWallet = walletGenerator.importWalletFromPrivateKey(privateKey, walletName, password)
        if (_wallets.value.any { it.address.equals(existingWallet.address, ignoreCase = true) }) {
            throw IllegalArgumentException("Wallet with this address already exists")
        }

        val wallet = existingWallet
        val updatedWallets = _wallets.value + wallet
        _wallets.value = updatedWallets
        storage.saveWallets(updatedWallets)

        if (_selectedWallet.value == null) {
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
        val updatedWallets = _wallets.value.filter {
            !it.address.equals(address, ignoreCase = true)
        }

        _wallets.value = updatedWallets
        storage.saveWallets(updatedWallets)

        // Update selected wallet if it was deleted
        _selectedWallet.value?.let { selected ->
            if (selected.address.equals(address, ignoreCase = true)) {
                _selectedWallet.value = updatedWallets.firstOrNull()
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
        val wallet = _wallets.value.find {
            it.address.equals(address, ignoreCase = true)
        } ?: throw IllegalArgumentException("Wallet not found")

        return walletGenerator.decryptPrivateKey(wallet.encryptedPrivateKey, password)
    }

    fun getWalletByAddress(address: String): Wallet? {
        return _wallets.value.find {
            it.address.equals(address, ignoreCase = true)
        }
    }

    fun hasWallets(): Boolean {
        return _wallets.value.isNotEmpty()
    }

    companion object {
        private val instances = mutableMapOf<Project, WalletManager>()

        fun getInstance(project: Project): WalletManager {
            return instances.getOrPut(project) { WalletManager(project) }
        }
    }
}
