package dev.eastgate.metamaskclone.core.network

import com.intellij.openapi.project.Project
import dev.eastgate.metamaskclone.core.storage.Network
import dev.eastgate.metamaskclone.core.storage.ProjectStorage
import dev.eastgate.metamaskclone.models.BlockchainType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class NetworkManager private constructor(private val project: Project) {
    private val storage = ProjectStorage.getInstance(project)

    private val _selectedNetwork = MutableStateFlow<Network>(getInitialNetwork())
    val selectedNetwork: StateFlow<Network> = _selectedNetwork.asStateFlow()

    private val _enabledNetworkIds = MutableStateFlow<List<String>>(getInitialEnabledNetworkIds())
    val enabledNetworkIds: StateFlow<List<String>> = _enabledNetworkIds.asStateFlow()

    private val _customNetworks = MutableStateFlow<List<Network>>(storage.getCustomNetworks())
    val customNetworks: StateFlow<List<Network>> = _customNetworks.asStateFlow()

    companion object {
        private val instances = ConcurrentHashMap<Project, NetworkManager>()

        val DEFAULT_ENABLED_IDS = listOf("BNB_TESTNET", "ETH_SEPOLIA", "POLYGON_TESTNET", "TRON_SHASTA")

        fun getInstance(project: Project): NetworkManager {
            return instances.computeIfAbsent(project) { NetworkManager(it) }
        }
    }

    private fun getInitialNetwork(): Network {
        val selectedId = storage.getSelectedNetwork()
        return findNetworkById(selectedId) ?: PredefinedNetworks.BNB_TESTNET
    }

    private fun getInitialEnabledNetworkIds(): List<String> {
        val storedIds = storage.getEnabledNetworkIds()
        return if (storedIds.isEmpty()) DEFAULT_ENABLED_IDS else storedIds
    }

    fun findNetworkById(id: String): Network? {
        return PredefinedNetworks.getNetworkById(id)
            ?: _customNetworks.value.find { it.id == id }
    }

    fun selectNetwork(networkId: String) {
        val network = findNetworkById(networkId)
        if (network != null) {
            _selectedNetwork.value = network
            storage.setSelectedNetwork(networkId)
        }
    }

    /**
     * Get the blockchain type of the currently selected network.
     */
    fun getCurrentBlockchainType(): BlockchainType {
        return _selectedNetwork.value.blockchainType
    }

    fun getEnabledNetworks(): List<Network> {
        val enabledIds = _enabledNetworkIds.value
        val predefined = PredefinedNetworks.ALL_NETWORKS.filter { it.id in enabledIds }
        val custom = _customNetworks.value.filter { it.id in enabledIds }
        return predefined + custom
    }

    fun getDisabledPredefinedNetworks(): List<Network> {
        val enabledIds = _enabledNetworkIds.value
        return PredefinedNetworks.ALL_NETWORKS.filter { it.id !in enabledIds }
    }

    fun enableNetwork(networkId: String) {
        val currentIds = _enabledNetworkIds.value.toMutableList()
        if (networkId !in currentIds) {
            currentIds.add(networkId)
            _enabledNetworkIds.value = currentIds
            storage.setEnabledNetworkIds(currentIds)
        }
    }

    fun disableNetwork(networkId: String) {
        // Don't allow disabling the currently selected network
        if (networkId == _selectedNetwork.value.id) return

        val currentIds = _enabledNetworkIds.value.toMutableList()
        if (currentIds.remove(networkId)) {
            _enabledNetworkIds.value = currentIds
            storage.setEnabledNetworkIds(currentIds)
        }
    }

    fun addCustomNetwork(network: Network): Boolean {
        // Check for duplicate chain ID
        val existingNetwork = PredefinedNetworks.getNetworkByChainId(network.chainId)
            ?: _customNetworks.value.find { it.chainId == network.chainId }

        if (existingNetwork != null && existingNetwork.id != network.id) {
            return false // Duplicate chain ID
        }

        val customNetworks = _customNetworks.value.toMutableList()
        val existingIndex = customNetworks.indexOfFirst { it.id == network.id }

        if (existingIndex >= 0) {
            customNetworks[existingIndex] = network
        } else {
            customNetworks.add(network)
        }

        _customNetworks.value = customNetworks
        storage.saveCustomNetworks(customNetworks)

        // Auto-enable the new network
        enableNetwork(network.id)

        return true
    }

    fun updateCustomNetwork(network: Network): Boolean {
        return addCustomNetwork(network)
    }

    fun removeCustomNetwork(networkId: String) {
        // Don't allow removing the currently selected network
        if (networkId == _selectedNetwork.value.id) {
            // Switch to default network first
            selectNetwork(PredefinedNetworks.BNB_TESTNET.id)
        }

        val customNetworks = _customNetworks.value.toMutableList()
        if (customNetworks.removeIf { it.id == networkId }) {
            _customNetworks.value = customNetworks
            storage.saveCustomNetworks(customNetworks)
        }

        // Also remove from enabled networks
        disableNetwork(networkId)
    }

    fun isCustomNetwork(networkId: String): Boolean {
        return _customNetworks.value.any { it.id == networkId }
    }

    fun getAllNetworks(): List<Network> {
        return PredefinedNetworks.ALL_NETWORKS + _customNetworks.value
    }

    fun generateCustomNetworkId(): String {
        return "CUSTOM_${System.currentTimeMillis()}"
    }
}
